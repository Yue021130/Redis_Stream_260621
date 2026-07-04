package com.example.redisstream.controller;

import com.example.redisstream.dto.ApiResponse;
import com.example.redisstream.service.RedisSetUseCasesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 演示 Redis Set 经典使用场景 API
 */
@RestController
@RequestMapping("/api/redis-usecases/set")
public class RedisSetUseCasesController {

    @Autowired
    private RedisSetUseCasesService redisSetUseCasesService;

    // --- 1. UV 统计场景 ---
    @PostMapping("/uv/{pageId}/visit")
    public ApiResponse<String> visitPage(@PathVariable String pageId, @RequestParam String userId) {
        redisSetUseCasesService.recordUniqueVisit(pageId, userId);
        return ApiResponse.success("访问记录成功", null);
    }

    @GetMapping("/uv/{pageId}/count")
    public ApiResponse<Long> getUvCount(@PathVariable String pageId) {
        Long count = redisSetUseCasesService.getUniqueVisitCount(pageId);
        return ApiResponse.success("获取UV成功", count);
    }

    // --- 2. 抽奖系统场景 ---
    @PostMapping("/lottery/{activityId}/join")
    public ApiResponse<String> joinLottery(@PathVariable String activityId, @RequestParam String userId) {
        redisSetUseCasesService.joinLottery(activityId, userId);
        return ApiResponse.success("参与抽奖成功", null);
    }

    @GetMapping("/lottery/{activityId}/draw")
    public ApiResponse<List<String>> drawLottery(@PathVariable String activityId, @RequestParam(defaultValue = "1") int count) {
        List<String> winners = redisSetUseCasesService.drawLottery(activityId, count);
        return ApiResponse.success("抽奖完成", winners);
    }

    // --- 3. 社交关系 (共同关注) ---
    @PostMapping("/social/{userId}/follow")
    public ApiResponse<String> follow(@PathVariable String userId, @RequestParam String targetUserId) {
        redisSetUseCasesService.follow(userId, targetUserId);
        return ApiResponse.success("关注成功", null);
    }

    @GetMapping("/social/common")
    public ApiResponse<Set<String>> getCommonFollows(@RequestParam String userId1, @RequestParam String userId2) {
        Set<String> common = redisSetUseCasesService.getCommonFollows(userId1, userId2);
        return ApiResponse.success("获取共同关注成功", common);
    }
}
