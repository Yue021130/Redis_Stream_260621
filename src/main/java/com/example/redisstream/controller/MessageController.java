package com.example.redisstream.controller;

import com.example.redisstream.config.StreamProperties;
import com.example.redisstream.dto.*;
import com.example.redisstream.service.consumer.AbstractStreamConsumer;
import com.example.redisstream.service.producer.OrderProducerService;
import com.example.redisstream.support.RedisStreamOperator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.redisstream.support.RedisStreamOperator;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息队列调试接口。
 *
 * 提供 RESTful API，方便学习者手动触发发送、查看消费状态、查看 pending、
 * 查看死信队列等，无需写额外脚本。
 */
@Validated
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class MessageController {

    private final OrderProducerService producerService;
    private final RedisStreamOperator streamOperator;
    private final StreamProperties streamProperties;
    private final List<AbstractStreamConsumer> consumers;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 发送单条订单消息。
     */
    @PostMapping("/send")
    public ApiResponse<String> send(@RequestBody @Valid OrderMessage message) {
        RecordId id = producerService.send(message);
        return ApiResponse.success("发送成功", id.getValue());
    }

    /**
     * 批量发送消息。
     */
    @PostMapping("/send/batch")
    public ApiResponse<List<String>> sendBatch(@RequestBody @Valid SendRequest request) {
        List<RecordId> ids = producerService.sendBatch(request.getMessage(), request.getCount());
        List<String> idStrList = new ArrayList<>();
        for (RecordId id : ids) {
            idStrList.add(id.getValue());
        }
        return ApiResponse.success("批量发送成功，共 " + ids.size() + " 条", idStrList);
    }

    /**
     * 发送一条测试消息。
     */
    @PostMapping("/send/demo")
    public ApiResponse<String> sendDemo() {
        OrderMessage message = producerService.buildDemoMessage();
        RecordId id = producerService.send(message);
        return ApiResponse.success("测试消息发送成功", id.getValue());
    }

    /**
     * 查看某消费组的 pending 消息。
     */
    @GetMapping("/pending")
    public ApiResponse<List<PendingMessageDto>> pending(@RequestParam String group) {
        PendingMessages pendingMessages = streamOperator.pending(group, 100);
        List<PendingMessageDto> result = new ArrayList<>();
        for (PendingMessage pm : pendingMessages) {
            result.add(PendingMessageDto.builder()
                    .id(pm.getId().getValue())
                    .consumer(pm.getConsumerName())
                    .idleTime(pm.getElapsedTimeSinceLastDelivery().toMillis())
                    .deliveryCount(pm.getTotalDeliveryCount())
                    .build());
        }
        return ApiResponse.success(result);
    }

    /**
     * 查看死信队列最近 50 条消息。
     */
    @GetMapping("/dlq")
    public ApiResponse<List<Map<String, String>>> dlq() {
        // "-" 表示最小 ID，"+" 表示最大 ID
        List<MapRecord<String, String, String>> records = streamOperator.range("-", "+", 50);
        List<Map<String, String>> result = new ArrayList<>();
        for (MapRecord<String, String, String> record : records) {
            Map<String, String> map = new HashMap<>(record.getValue());
            map.put("id", record.getId().getValue());
            result.add(map);
        }
        return ApiResponse.success(result);
    }

    /**
     * 查看 Stream 统计信息。
     */
    @GetMapping("/stats")
    public ApiResponse<StreamStatsDto> stats() {
        Map<String, Long> pendingCounts = new HashMap<>();
        Map<String, List<String>> consumersMap = new HashMap<>();

        for (AbstractStreamConsumer consumer : consumers) {
            String group = consumer.getGroupName();
            try {
                PendingMessagesSummary summary = streamOperator.pendingSummary(group);
                pendingCounts.put(group, summary.getTotalPendingMessages());
            } catch (Exception e) {
                pendingCounts.put(group, -1L);
            }
            // 消费者名称直接取当前实例名称，实际多实例场景需结合 XINFO CONSUMERS
            List<String> names = new ArrayList<>();
            names.add(consumer.getConsumerName());
            consumersMap.put(group, names);
        }

        StreamStatsDto dto = StreamStatsDto.builder()
                .streamKey(streamProperties.getKey())
                .length(streamOperator.len(streamProperties.getKey()))
                .pendingCounts(pendingCounts)
                .consumers(consumersMap)
                .dlqLength(streamOperator.len(streamProperties.getDlqKey()))
                .build();

        return ApiResponse.success(dto);
    }

    /**
     * 获取供前端展示的实时事件日志。
     */
    @GetMapping("/logs")
    public ApiResponse<List<Map<String, String>>> logs() {
        List<String> rawLogs = stringRedisTemplate.opsForList().range("order:ui:logs", 0, 49);
        List<Map<String, String>> result = new ArrayList<>();
        if (rawLogs != null) {
            for (String raw : rawLogs) {
                try {
                    result.add(objectMapper.readValue(raw, Map.class));
                } catch (Exception e) {
                    // ignore parse errors
                }
            }
        }
        return ApiResponse.success(result);
    }

    /**
     * 获取最近生产的 Stream 消息（XREVRANGE）。
     */
    @GetMapping("/recent")
    public ApiResponse<List<Map<String, String>>> recentMessages() {
        List<MapRecord<String, String, String>> records = streamOperator.reverseRange(50);
        List<Map<String, String>> result = new ArrayList<>();
        if (records != null) {
            for (MapRecord<String, String, String> record : records) {
                Map<String, String> map = new HashMap<>(record.getValue());
                map.put("id", record.getId().getValue());
                result.add(map);
            }
        }
        return ApiResponse.success(result);
    }
}
