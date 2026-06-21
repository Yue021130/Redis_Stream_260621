package com.example.redisstream.service.consumer;

import com.example.redisstream.config.StreamProperties;
import com.example.redisstream.constants.StreamConstants;
import com.example.redisstream.dto.OrderMessage;
import com.example.redisstream.support.IdempotentService;
import com.example.redisstream.support.RedisStreamOperator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Stream 消费者抽象模板。
 *
 * 设计思想：
 * 1. 每个消费者对应一个“消费者组”，组内可启动多个实例做负载均衡。
 * 2. 收到消息后：幂等检查 -> 业务处理 -> ACK。
 * 3. 业务异常必须抛出，让消息保持未 ACK 状态，从而进入 pending list，供后续重试。
 * 4. 已幂等的消息也要 ACK，避免 pending 消息无限累积。
 *
 * 注意：
 * - 子类需要实现 getGroupName()、getConsumerName()、handleBusiness()。
 * - 子类必须是 Spring Bean，由 RedisConfig 统一注册到 StreamMessageListenerContainer。
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractStreamConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final IdempotentService idempotentService;
    private final RedisStreamOperator streamOperator;
    private final StreamProperties streamProperties;

    private final Random random = new Random();

    /** 子类指定所属消费者组 */
    public abstract String getGroupName();

    /** 子类指定消费者名称（同一组内多个实例建议不同名） */
    public abstract String getConsumerName();

    /** 子类实现具体业务逻辑 */
    protected abstract void handleBusiness(OrderMessage message) throws Exception;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        RecordId id = message.getId();
        String payload = message.getValue().get(StreamConstants.FIELD_PAYLOAD);
        OrderMessage orderMessage = parsePayload(payload);
        if (orderMessage == null) {
            log.error("[{}] 无法解析消息 payload，直接 ACK：id={}", getGroupName(), id.getValue());
            ack(id);
            return;
        }

        String orderId = orderMessage.getOrderId();
        log.debug("[{}] 收到消息：id={}, orderId={}", getGroupName(), id.getValue(), orderId);

        // 1. 业务幂等检查
        if (idempotentService.isProcessed(orderId)) {
            log.warn("[{}] 订单已处理，跳过业务并 ACK：orderId={}", getGroupName(), orderId);
            ack(id);
            return;
        }

        // 2. 抢占幂等锁，防止多实例同时处理同一订单
        if (!idempotentService.tryLock(orderId, streamProperties.getIdempotentTtlSeconds())) {
            log.warn("[{}] 幂等锁抢占失败，跳过业务并 ACK：orderId={}", getGroupName(), orderId);
            ack(id);
            return;
        }

        try {
            // 3. 执行业务逻辑
            handleBusiness(orderMessage);

            // 4. 业务成功，ACK 消息
            ack(id);
            log.info("[{}] 业务处理成功并已 ACK：orderId={}, id={}",
                    getGroupName(), orderId, id.getValue());
        } catch (Exception e) {
            // 业务失败，不 ACK，消息会进入 pending list
            log.error("[{}] 业务处理失败，消息进入 pending，等待重试：orderId={}, id={}",
                    getGroupName(), orderId, id.getValue(), e);

            // 记录重试次数，便于 pending 处理器判断是否进 DLQ
            incrementRetry(orderId);

            // 模拟失败后不 ACK，抛出运行时异常让监听容器感知
            throw new RuntimeException("业务处理失败：" + e.getMessage(), e);
        }
    }

    /**
     * ACK 确认消息。
     */
    protected void ack(RecordId id) {
        try {
            long ackCount = streamOperator.acknowledge(getGroupName(), id);
            if (ackCount <= 0) {
                log.warn("[{}] ACK 返回 0，可能消息已被确认或不存在：id={}", getGroupName(), id.getValue());
            }
        } catch (DataAccessException e) {
            log.error("[{}] ACK 异常：id={}", getGroupName(), id.getValue(), e);
        }
    }

    /**
     * 增加重试计数（基于 Redis hash）。
     */
    protected void incrementRetry(String orderId) {
        String key = StreamConstants.RETRY_PREFIX + getGroupName() + ":" + orderId;
        try {
            stringRedisTemplate.opsForHash().increment(key, "count", 1);
            stringRedisTemplate.expire(key, streamProperties.getIdempotentTtlSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[{}] 重试计数失败：orderId={}", getGroupName(), orderId, e);
        }
    }

    /**
     * 获取当前重试次数。
     */
    protected int getRetryCount(String orderId) {
        String key = StreamConstants.RETRY_PREFIX + getGroupName() + ":" + orderId;
        Object count = stringRedisTemplate.opsForHash().get(key, "count");
        if (count == null) {
            return 0;
        }
        try {
            return Integer.parseInt(count.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 是否触发模拟失败（仅当配置开启时）。
     */
    protected boolean shouldSimulateFailure() {
        return Boolean.TRUE.equals(streamProperties.getSimulateFailure())
                && random.nextDouble() < streamProperties.getFailureRate();
    }

    private OrderMessage parsePayload(String payload) {
        try {
            return objectMapper.readValue(payload, OrderMessage.class);
        } catch (IOException e) {
            log.error("JSON 反序列化失败：{}", payload, e);
            return null;
        }
    }
}
