package com.example.redisstream.support;

import com.example.redisstream.config.StreamProperties;
import com.example.redisstream.constants.StreamConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis Stream 底层操作封装。
 *
 * 把所有和 Stream 相关的命令（XADD、XACK、XPENDING、XCLAIM、XLEN、XRANGE 等）
 * 集中到这里，上层业务只关注业务逻辑。
 *
 * 泛型说明：
 * - K = String：Stream key 类型
 * - HK = String：entry 中的 field 名
 * - HV = String：entry 中的 value（这里统一用 JSON 字符串）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamOperator {

    private final StringRedisTemplate stringRedisTemplate;
    private final StreamProperties streamProperties;

    /**
     * 获取 StreamOperations，简化调用。
     */
    private StreamOperations<String, String, String> ops() {
        return stringRedisTemplate.opsForStream();
    }

    /**
     * 发送一条消息到 Stream。
     *
     * @param payload 业务 JSON 字符串
     * @return Redis 生成的消息 ID（如 1625097600000-0）
     */
    public RecordId addMessage(String payload) {
        Map<String, String> body = new HashMap<>();
        body.put(StreamConstants.FIELD_PAYLOAD, payload);
        MapRecord<String, String, String> record = MapRecord.create(streamProperties.getKey(), body);
        RecordId id = ops().add(record);

        // 控制 stream 长度，避免无限增长
        trimStream();
        return id;
    }

    /**
     * 将失败消息写入死信队列，并附带失败原因和重试次数。
     */
    public RecordId addToDlq(String originalId, String payload, int retryCount, String reason) {
        Map<String, String> body = new HashMap<>();
        body.put(StreamConstants.FIELD_ORIGINAL_ID, originalId);
        body.put(StreamConstants.FIELD_PAYLOAD, payload);
        body.put(StreamConstants.FIELD_RETRY_COUNT, String.valueOf(retryCount));
        body.put(StreamConstants.FIELD_REASON, reason);
        MapRecord<String, String, String> record = MapRecord.create(streamProperties.getDlqKey(), body);
        return ops().add(record);
    }

    /**
     * 确认消息已处理（ACK）。
     *
     * @param group 消费者组
     * @param id    消息 ID
     * @return 被 ACK 的消息数量
     */
    public long acknowledge(String group, RecordId id) {
        Long count = ops().acknowledge(streamProperties.getKey(), group, id);
        return count == null ? 0L : count;
    }

    /**
     * 获取 Stream 当前长度（XLEN）。
     */
    public long len(String key) {
        Long length = ops().size(key);
        return length == null ? 0L : length;
    }

    /**
     * 截断 Stream 到最大长度（XTRIM）。
     */
    public void trimStream() {
        try {
            ops().trim(streamProperties.getKey(), streamProperties.getMaxLen());
        } catch (Exception e) {
            // trim 失败不影响主流程，仅记录日志
            log.warn("XTRIM 失败：{}", e.getMessage());
        }
    }

    /**
     * 查询某消费组的 pending 消息列表。
     *
     * @param group 消费者组
     * @param count 最多返回条数
     */
    public PendingMessages pending(String group, int count) {
        return ops().pending(streamProperties.getKey(), group, Range.unbounded(), count);
    }

    /**
     * 查询某消费组 pending 消息摘要（XPENDING key group）。
     */
    public PendingMessagesSummary pendingSummary(String group) {
        return ops().pending(streamProperties.getKey(), group);
    }

    /**
     * 将 idle 超过指定时间的 pending 消息 claim 给新的消费者。
     *
     * @param group       消费者组
     * @param newConsumer 新消费者名称
     * @param minIdleTime 最小空闲时间
     * @param ids         要 claim 的消息 ID
     * @return claim 到的消息记录
     */
    public List<MapRecord<String, String, String>> claim(
            String group, String newConsumer, Duration minIdleTime, RecordId... ids) {
        if (ids == null || ids.length == 0) {
            return Collections.emptyList();
        }
        return ops().claim(streamProperties.getKey(), group, newConsumer, minIdleTime, ids);
    }

    /**
     * 根据消息 ID 范围查询完整消息内容（XRANGE）。
     */
    public List<MapRecord<String, String, String>> range(RecordId start, RecordId end, int count) {
        return range(start.getValue(), end.getValue(), count);
    }

    /**
     * 根据消息 ID 字符串范围查询完整消息内容（XRANGE）。
     *
     * 支持特殊范围符号："-" 表示最小 ID，"+" 表示最大 ID。
     */
    public List<MapRecord<String, String, String>> range(String start, String end, int count) {
        return ops().range(streamProperties.getKey(),
                Range.closed(start, end),
                RedisZSetCommands.Limit.limit().count(count));
    }

    /**
     * 根据消息 ID 倒序查询完整消息内容（XREVRANGE），用于获取最新消息。
     */
    public List<MapRecord<String, String, String>> reverseRange(int count) {
        return ops().reverseRange(streamProperties.getKey(),
                Range.unbounded(),
                RedisZSetCommands.Limit.limit().count(count));
    }

    /**
     * 创建消费者组（若已存在则忽略异常）。
     */
    public void createGroup(String group) {
        try {
            // ReadOffset.from("0") 表示从 stream 第一条消息开始消费
            ops().createGroup(streamProperties.getKey(), ReadOffset.from("0"), group);
            log.info("创建消费者组成功：key={}, group={}", streamProperties.getKey(), group);
        } catch (Exception e) {
            // BUSYGROUP 表示组已存在，属于正常情况
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                log.info("消费者组已存在：group={}", group);
            } else {
                log.error("创建消费者组失败：group={}", group, e);
                throw new RuntimeException("创建消费者组失败：" + group, e);
            }
        }
    }
}
