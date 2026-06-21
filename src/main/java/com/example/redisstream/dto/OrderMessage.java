package com.example.redisstream.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 业务消息体：模拟电商订单事件。
 *
 * 设计要点：
 * 1. 必须实现 Serializable，便于 Redis/网络传输。
 * 2. 使用 Jackson 注解格式化时间，避免前端展示为时间戳。
 * 3. 使用 @NotBlank/@NotNull 做基础校验。
 * 4. 金额使用 BigDecimal，避免 double/float 精度问题。
 *
 * 在 Redis Stream 中，本对象会被序列化为 JSON 字符串，存储在字段 payload 中。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单 ID，业务唯一标识，也是幂等键的核心 */
    @NotBlank(message = "订单 ID 不能为空")
    private String orderId;

    /** 用户 ID */
    @NotBlank(message = "用户 ID 不能为空")
    private String userId;

    /** 商品 SKU */
    @NotBlank(message = "SKU 不能为空")
    private String sku;

    /** 购买数量 */
    @NotNull(message = "数量不能为空")
    private Integer quantity;

    /** 订单金额 */
    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    /** 下单时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
