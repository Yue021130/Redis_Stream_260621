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
 * 短信通知消费者。
 *
 * 业务场景：订单创建后，给用户发送短信通知。
 * 短信服务通常耗时且不稳定，适合异步处理。
 *
 * 消费者组：order:group:sms
 */
@Slf4j
@Component
public class SmsConsumer extends AbstractStreamConsumer {

    private final String consumerName;
    private final EventLogService eventLogService;

    public SmsConsumer(StringRedisTemplate stringRedisTemplate,
                       ObjectMapper objectMapper,
                       IdempotentService idempotentService,
                       RedisStreamOperator streamOperator,
                       StreamProperties streamProperties,
                       EventLogService eventLogService,
                       @Value("${app.stream.consumer-prefix:consumer}-sms") String consumerName) {
        super(stringRedisTemplate, objectMapper, idempotentService, streamOperator, streamProperties);
        this.consumerName = consumerName;
        this.eventLogService = eventLogService;
    }

    @Override
    public String getGroupName() {
        return "order:group:sms";
    }

    @Override
    public String getConsumerName() {
        return consumerName;
    }

    @Override
    protected void handleBusiness(OrderMessage message) throws Exception {
        log.info("[短信组] 正在发送短信：orderId={}, userId={}",
                message.getOrderId(), message.getUserId());

        // 模拟随机失败
        if (shouldSimulateFailure()) {
            eventLogService.log("warning", String.format("[短信组] 发送失败，模拟业务异常 (orderId=%s)", message.getOrderId()));
            throw new RuntimeException("短信服务异常，模拟业务失败");
        }

        // TODO：真实业务中调用短信网关
        // smsGateway.send(message.getUserId(), "您的订单已创建：" + message.getOrderId());

        log.info("[短信组] 短信发送成功：orderId={}", message.getOrderId());
        eventLogService.log("success", String.format("[短信组] 发送短信成功并 ACK (orderId=%s)", message.getOrderId()));
    }
}
