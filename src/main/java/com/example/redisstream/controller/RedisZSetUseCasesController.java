package com.example.redisstream.controller;

import com.example.redisstream.dto.ApiResponse;
import com.example.redisstream.service.RedisZSetUseCasesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 演示 Redis ZSet 经典使用场景 API
 */
@RestController
@RequestMapping("/api/redis-usecases/zset")
public class RedisZSetUseCasesController {

    @Autowired
    private RedisZSetUseCasesService redisZSetUseCasesService;

    // --- 1. 排行榜场景 ---
    
    @PostMapping("/leaderboard/{boardName}/addScore")
    public ApiResponse<String> addScore(@PathVariable String boardName, @RequestParam String userId, @RequestParam double score) {
        redisZSetUseCasesService.addScore(boardName, userId, score);
        return ApiResponse.success("积分增加成功", null);
    }

    @GetMapping("/leaderboard/{boardName}/top")
    public ApiResponse<Set<String>> getTopPlayers(@PathVariable String boardName, @RequestParam(defaultValue = "10") int top) {
        Set<String> players = redisZSetUseCasesService.getTopPlayers(boardName, top);
        return ApiResponse.success("获取排行榜成功", players);
    }

    // --- 2. 延时队列场景 ---
    
    @PostMapping("/delayedQueue/{queueName}/send")
    public ApiResponse<String> sendDelayedTask(@PathVariable String queueName, @RequestParam String taskId, @RequestParam long delaySeconds) {
        redisZSetUseCasesService.sendDelayedTask(queueName, taskId, delaySeconds);
        return ApiResponse.success("延时任务投递成功", null);
    }

    @GetMapping("/delayedQueue/{queueName}/fetch")
    public ApiResponse<Set<String>> fetchDueTasks(@PathVariable String queueName) {
        Set<String> tasks = redisZSetUseCasesService.fetchDueTasks(queueName);
        if (tasks != null && !tasks.isEmpty()) {
            return ApiResponse.success("拉取到期任务成功", tasks);
        } else {
            return ApiResponse.success("当前无到期任务", null);
        }
    }
}
