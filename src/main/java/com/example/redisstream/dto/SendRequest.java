package com.example.redisstream.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 批量发送消息请求体。
 *
 * 用于 /api/order/send/batch 接口，一次可以发多条消息，方便压测和学习观察。
 */
@Data
public class SendRequest {

    /** 要发送的订单消息 */
    @Valid
    @NotNull(message = "订单消息不能为空")
    private OrderMessage message;

    /** 重复发送次数，默认 1 */
    @Min(value = 1, message = "数量至少为 1")
    private Integer count = 1;
}
