package com.example.redisstream.support;

import com.example.redisstream.config.StreamProperties;
import com.example.redisstream.constants.StreamConstants;
import com.example.redisstream.service.consumer.AbstractStreamConsumer;
import com.example.redisstream.support.EventLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Pending 消息定时巡检处理器。
 *
 * 学习要点：
 * 1. 消费者处理消息时抛异常不 ACK，消息会进入该消费者组的 pending list。
 * 2. XPENDING 可以查看 pending 消息：ID、所属消费者、idle 时间、deliveryCount。
 * 3. XCLAIM 可以把 idle 时间过长的消息“抢”过来，由当前消费者重新处理。
 * 4. 超过最大重试次数的消息写入 DLQ（死信队列）并 ACK，避免阻塞 pending list。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingMessageProcessor {

    private final StreamProperties streamProperties;
    private final RedisStreamOperator streamOperator;
    private final EventLogService eventLogService;
    private final List<AbstractStreamConsumer> consumers;

    /**
     * 定时扫描所有消费者组的 pending 消息。
     *
     * fixedDelay：上次执行完毕后，间隔 pending-interval-ms 再执行。
     */
    @Scheduled(fixedDelayString = "${app.stream.pending-interval-ms:10000}")
    public void processPendingMessages() {
        for (AbstractStreamConsumer consumer : consumers) {
            String group = consumer.getGroupName();
            String owner = consumer.getConsumerName();
            try {
                PendingMessages pendingMessages = streamOperator.pending(group, 100);
                if (pendingMessages.isEmpty()) {
                    continue;
                }
                log.info("[Pending 巡检] group={} 发现 {} 条 pending 消息", group, pendingMessages.size());

                for (PendingMessage pm : pendingMessages) {
                    handlePendingMessage(group, owner, pm);
                }
            } catch (Exception e) {
                log.error("[Pending 巡检] group={} 处理异常", group, e);
            }
        }
    }

    /**
     * 处理单条 pending 消息。
     */
    private void handlePendingMessage(String group, String owner, PendingMessage pm) {
        RecordId id = pm.getId();
        long idleMs = pm.getElapsedTimeSinceLastDelivery().toMillis();
        long deliveryCount = pm.getTotalDeliveryCount();

        log.debug("[Pending] id={}, consumer={}, idleMs={}, deliveryCount={}",
                id.getValue(), pm.getConsumerName(), idleMs, deliveryCount);

        // 只有 idle 超过阈值才处理，避免和原消费者竞争
        if (idleMs < streamProperties.getClaimIdleMs()) {
            log.debug("[Pending] id={} idle 时间不足，暂不 claim", id.getValue());
            return;
        }

        // 超过最大重试次数，进死信队列
        if (deliveryCount > streamProperties.getMaxRetries()) {
            moveToDlq(group, id, (int) deliveryCount);
            return;
        }

        // 执行 claim：把消息所有权转给当前消费者
        try {
            List<MapRecord<String, String, String>> claimed = streamOperator.claim(
                    group, owner, Duration.ofMillis(streamProperties.getClaimIdleMs()), id);

            if (claimed.isEmpty()) {
                log.warn("[Pending] claim 未拿到消息，可能已被其他消费者处理：id={}", id.getValue());
                return;
            }

            // claim 后由当前消费者再次处理
            // 为了简化，这里直接调用对应 consumer 的 onMessage；
            // 也可以把消息重新丢回业务线程池处理。
            for (MapRecord<String, String, String> record : claimed) {
                AbstractStreamConsumer target = findConsumer(group);
                if (target != null) {
                    log.info("[Pending] 重新处理消息：group={}, id={}", group, id.getValue());
                    eventLogService.log("info", String.format("[%s] XCLAIM 抢占超时 Pending 消息: %s", group, id.getValue()));
                    target.onMessage(record);
                } else {
                    log.error("[Pending] 找不到对应消费者：group={}", group);
                }
            }
        } catch (Exception e) {
            log.error("[Pending] claim 或重处理失败：group={}, id={}", group, id.getValue(), e);
        }
    }

    /**
     * 将超过重试次数的消息写入死信队列，并 ACK 原消息。
     */
    private void moveToDlq(String group, RecordId id, int retryCount) {
        try {
            // 先查出完整消息内容
            List<MapRecord<String, String, String>> records = streamOperator.range(id, id, 1);
            if (records.isEmpty()) {
                log.warn("[DLQ] 找不到原消息内容，直接 ACK：id={}", id.getValue());
                streamOperator.acknowledge(group, id);
                return;
            }

            MapRecord<String, String, String> record = records.get(0);
            String payload = record.getValue().get(StreamConstants.FIELD_PAYLOAD);

            // 写入 DLQ
            streamOperator.addToDlq(id.getValue(), payload, retryCount,
                    "超过最大重试次数 " + streamProperties.getMaxRetries());

            // ACK 原消息，从 pending list 移除
            streamOperator.acknowledge(group, id);

            log.warn("[DLQ] 消息进入死信队列：group={}, id={}, retryCount={}", group, id.getValue(), retryCount);
            eventLogService.log("danger", String.format("[%s] 多次重试失败，已将消息 %s 移入 DLQ", group, id.getValue()));
        } catch (Exception e) {
            log.error("[DLQ] 移入死信队列失败：group={}, id={}", group, id.getValue(), e);
        }
    }

    private AbstractStreamConsumer findConsumer(String group) {
        for (AbstractStreamConsumer c : consumers) {
            if (c.getGroupName().equals(group)) {
                return c;
            }
        }
        return null;
    }
}
