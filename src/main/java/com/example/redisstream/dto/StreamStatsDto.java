package com.example.redisstream.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Stream 统计信息 DTO。
 *
 * 用于 /api/order/stats 接口，直观展示：
 * - stream 当前长度
 * - 各消费者组 pending 数量
 * - 各组消费者列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamStatsDto {

    /** Stream key */
    private String streamKey;

    /** 当前 stream 长度 */
    private Long length;

    /** 各消费组 pending 数量：group -> count */
    private Map<String, Long> pendingCounts;

    /** 各消费组消费者列表：group -> [consumerName] */
    private Map<String, List<String>> consumers;

    /** 死信队列长度 */
    private Long dlqLength;
}
