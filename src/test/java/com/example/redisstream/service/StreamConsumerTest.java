package com.example.redisstream.service;

import com.example.redisstream.dto.OrderMessage;
import com.example.redisstream.service.producer.OrderProducerService;
import com.example.redisstream.support.RedisStreamOperator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 消费者集成测试。
 *
 * 验证消息发送后，经过一段消费时间，各消费者组的 pending 数量会归零（正常 ACK）。
 */
@SpringBootTest
class StreamConsumerTest {

    @Autowired
    private OrderProducerService producerService;

    @Autowired
    private RedisStreamOperator streamOperator;

    @Test
    void testConsumerAck() throws InterruptedException {
        OrderMessage message = OrderMessage.builder()
                .orderId("CONSUMER-TEST-" + System.currentTimeMillis())
                .userId("U002")
                .sku("SKU-200")
                .quantity(1)
                .amount(new BigDecimal("9.90"))
                .build();

        producerService.send(message);

        // 等待消费者处理（默认轮询间隔 1 秒）
        TimeUnit.SECONDS.sleep(3);

        // 由于 simulate-failure 默认 false，两个组都应该成功 ACK
        assertPendingZero("order:group:inventory");
        assertPendingZero("order:group:sms");
    }

    private void assertPendingZero(String group) {
        PendingMessagesSummary summary = streamOperator.pendingSummary(group);
        assertNotNull(summary);
        assertEquals(0L, summary.getTotalPendingMessages(),
                "消费组 " + group + " 存在未 ACK 消息");
    }
}
