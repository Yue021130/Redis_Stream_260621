package com.example.redisstream.service;

import com.example.redisstream.dto.OrderMessage;
import com.example.redisstream.service.producer.OrderProducerService;
import com.example.redisstream.support.RedisStreamOperator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.stream.RecordId;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 生产者集成测试。
 *
 * 验证消息能正确写入 Redis Stream，并返回合法 RecordId。
 */
@SpringBootTest
class OrderProducerServiceTest {

    @Autowired
    private OrderProducerService producerService;

    @Autowired
    private RedisStreamOperator streamOperator;

    @Test
    void testSendMessage() {
        long before = streamOperator.len("order:stream");

        OrderMessage message = OrderMessage.builder()
                .orderId("TEST-" + System.currentTimeMillis())
                .userId("U001")
                .sku("SKU-100")
                .quantity(2)
                .amount(new BigDecimal("199.99"))
                .build();

        RecordId id = producerService.send(message);

        assertNotNull(id);
        assertTrue(id.getValue().contains("-"));
        long after = streamOperator.len("order:stream");
        assertEquals(before + 1, after);
    }
}
