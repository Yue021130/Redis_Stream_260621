package com.example.redisstream.task;

import com.example.redisstream.config.StreamProperties;
import com.example.redisstream.service.consumer.AbstractStreamConsumer;
import com.example.redisstream.support.RedisStreamOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stream 运行状态监控任务。
 *
 * 每隔一段时间打印：
 * - 当前 stream 长度
 * - 每个消费组的 pending 数量
 * - 死信队列长度
 *
 * 这些指标也可以对接 Prometheus/Micrometer，这里先用日志输出便于学习观察。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreamMonitorTask {

    private final StreamProperties streamProperties;
    private final RedisStreamOperator streamOperator;
    private final List<AbstractStreamConsumer> consumers;

    /**
     * 每 30 秒输出一次统计信息。
     */
    @Scheduled(fixedDelay = 30000)
    public void monitor() {
        try {
            long streamLen = streamOperator.len(streamProperties.getKey());
            long dlqLen = streamOperator.len(streamProperties.getDlqKey());

            StringBuilder sb = new StringBuilder();
            sb.append("\n========== Stream 监控 ==========");
            sb.append("\nStream key: ").append(streamProperties.getKey());
            sb.append("\nStream length: ").append(streamLen);
            sb.append("\nDLQ length: ").append(dlqLen);
            sb.append("\n消费组 pending:");

            for (AbstractStreamConsumer consumer : consumers) {
                String group = consumer.getGroupName();
                try {
                    PendingMessagesSummary summary = streamOperator.pendingSummary(group);
                    long pendingCount = summary.getTotalPendingMessages();
                    sb.append("\n  ").append(group).append(" = ").append(pendingCount);
                } catch (Exception e) {
                    sb.append("\n  ").append(group).append(" = 获取失败(").append(e.getMessage()).append(")");
                }
            }
            sb.append("\n=================================");
            log.info(sb.toString());
        } catch (Exception e) {
            log.error("Stream 监控任务异常", e);
        }
    }
}
