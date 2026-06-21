package com.example.redisstream.service.producer;

import com.example.redisstream.dto.OrderMessage;
import com.example.redisstream.support.RedisStreamOperator;
import com.example.redisstream.support.EventLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 消息生产者：负责把业务事件写入 Redis Stream。
 *
 * 对应 Redis 命令：XADD key * field value [field value ...]
 * 这里使用 field=payload, value=订单消息 JSON 字符串。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProducerService {

    private final RedisStreamOperator streamOperator;
    private final ObjectMapper objectMapper;
    private final EventLogService eventLogService;

    /**
     * 发送单条订单消息。
     *
     * @param message 订单消息（必须包含 orderId）
     * @return Redis 返回的消息 ID
     */
    public RecordId send(OrderMessage message) {
        if (message.getCreateTime() == null) {
            message.setCreateTime(LocalDateTime.now());
        }
        String payload = toJson(message);
        RecordId id = streamOperator.addMessage(payload);
        log.info("[生产者] 发送消息成功，id={}, orderId={}", id.getValue(), message.getOrderId());
        eventLogService.log("info", String.format("[生产者] 订单 %s (id: %s) 已写入 Stream (XADD)", message.getOrderId(), id.getValue()));
        return id;
    }

    /**
     * 批量发送同一条消息 count 次，用于压测或观察多消费者竞争。
     */
    public List<RecordId> sendBatch(OrderMessage message, int count) {
        List<RecordId> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // 每次生成新的 orderId，避免幂等导致只处理一次
            OrderMessage copy = copyWithNewOrderId(message);
            ids.add(send(copy));
        }
        return ids;
    }

    /**
     * 构造一条简单的测试消息。
     */
    public OrderMessage buildDemoMessage() {
        return OrderMessage.builder()
                .orderId(UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .userId("U" + System.currentTimeMillis() % 10000)
                .sku("SKU-" + (System.currentTimeMillis() % 100))
                .quantity(1)
                .amount(new java.math.BigDecimal("99.99"))
                .createTime(LocalDateTime.now())
                .build();
    }

    private OrderMessage copyWithNewOrderId(OrderMessage source) {
        return OrderMessage.builder()
                .orderId(UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .userId(source.getUserId())
                .sku(source.getSku())
                .quantity(source.getQuantity())
                .amount(source.getAmount())
                .createTime(source.getCreateTime())
                .build();
    }

    private String toJson(OrderMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("订单消息序列化失败", e);
        }
    }
}
