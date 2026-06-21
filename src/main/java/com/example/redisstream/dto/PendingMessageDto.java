package com.example.redisstream.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pending 消息列表返回 DTO。
 *
 * 对应 Redis XPENDING 命令返回的结构：
 * - id：消息 ID
 * - consumer：当前持有该消息的消费者
 * - idleTime：空闲时间（毫秒）
 * - deliveryCount：被投递次数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingMessageDto {

    private String id;
    private String consumer;
    private Long idleTime;
    private Long deliveryCount;
}
