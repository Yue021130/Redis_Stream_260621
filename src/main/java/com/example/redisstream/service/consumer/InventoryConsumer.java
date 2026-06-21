package com.example.redisstream.service.consumer;

import com.example.redisstream.config.StreamProperties;
import com.example.redisstream.dto.OrderMessage;
import com.example.redisstream.support.IdempotentService;
import com.example.redisstream.support.RedisStreamOperator;
import com.example.redisstream.support.EventLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 库存扣减消费者。
 *
 * 业务场景：订单创建后，需要扣减对应 SKU 的库存。
 * 这里用日志模拟真实的数据库/缓存扣减操作。
 *
 * 消费者组：order:group:inventory
 */
@Slf4j
@Component
public class InventoryConsumer extends AbstractStreamConsumer {

    private final String consumerName;
    private final EventLogService eventLogService;

    public InventoryConsumer(StringRedisTemplate stringRedisTemplate,
                             ObjectMapper objectMapper,
                             IdempotentService idempotentService,
                             RedisStreamOperator streamOperator,
                             StreamProperties streamProperties,
                             EventLogService eventLogService,
                             @Value("${app.stream.consumer-prefix:consumer}-inventory") String consumerName) {
        super(stringRedisTemplate, objectMapper, idempotentService, streamOperator, streamProperties);
        this.consumerName = consumerName;
        this.eventLogService = eventLogService;
    }

    @Override
    public String getGroupName() {
        return "order:group:inventory";
    }

    @Override
    public String getConsumerName() {
        return consumerName;
    }

    @Override
    protected void handleBusiness(OrderMessage message) throws Exception {
        log.info("[库存组] 正在扣减库存：orderId={}, sku={}, quantity={}",
                message.getOrderId(), message.getSku(), message.getQuantity());

        // 模拟随机失败，用于观察 pending 和重试
        if (shouldSimulateFailure()) {
            eventLogService.log("warning", String.format("[库存组] 扣减失败，模拟业务异常 (orderId=%s)", message.getOrderId()));
            throw new RuntimeException("库存服务异常，模拟业务失败");
        }

        // TODO：真实业务中这里调用库存服务/更新数据库
        // inventoryService.decrease(message.getSku(), message.getQuantity());

        log.info("[库存组] 库存扣减成功：orderId={}", message.getOrderId());
        eventLogService.log("success", String.format("[库存组] 扣减库存成功并 ACK (orderId=%s)", message.getOrderId()));
    }
}
