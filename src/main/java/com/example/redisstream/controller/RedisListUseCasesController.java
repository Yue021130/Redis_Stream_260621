package com.example.redisstream.controller;

import com.example.redisstream.dto.ApiResponse;
import com.example.redisstream.service.RedisListUseCasesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 演示 Redis List 经典使用场景 API
 */
@RestController
@RequestMapping("/api/redis-usecases/list")
public class RedisListUseCasesController {

    @Autowired
    private RedisListUseCasesService redisListUseCasesService;

    // --- 1. 简单消息队列场景 ---

    @PostMapping("/queue/{queueName}/send")
    public ApiResponse<String> sendMessage(@PathVariable String queueName, @RequestParam String message) {
        redisListUseCasesService.sendMessageToQueue(queueName, message);
        return ApiResponse.success("消息发送成功", message);
    }

    @GetMapping("/queue/{queueName}/receive")
    public ApiResponse<String> receiveMessage(@PathVariable String queueName, @RequestParam(defaultValue = "10") long timeoutSeconds) {
        String message = redisListUseCasesService.receiveMessageFromQueue(queueName, timeoutSeconds);
        if (message != null) {
            return ApiResponse.success("接收消息成功", message);
        } else {
            return ApiResponse.error(404, "队列暂无消息，等待超时");
        }
    }

    // --- 2. 最新列表/时间线场景 ---

    @PostMapping("/timeline/{timelineKey}/add")
    public ApiResponse<String> addLatestRecord(
            @PathVariable String timelineKey, 
            @RequestParam String record, 
            @RequestParam(defaultValue = "50") int maxKeep) {
        redisListUseCasesService.addLatestRecord(timelineKey, record, maxKeep);
        return ApiResponse.success("添加最新记录成功", record);
    }

    @GetMapping("/timeline/{timelineKey}")
    public ApiResponse<List<String>> getLatestRecords(
            @PathVariable String timelineKey,
            @RequestParam(defaultValue = "0") long start,
            @RequestParam(defaultValue = "9") long end) {
        // 默认获取前 10 条数据 (索引 0 ~ 9)
        List<String> records = redisListUseCasesService.getLatestRecords(timelineKey, start, end);
        return ApiResponse.success("获取列表成功", records);
    }
}
