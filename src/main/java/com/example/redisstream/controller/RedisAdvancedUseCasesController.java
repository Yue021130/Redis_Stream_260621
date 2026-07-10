package com.example.redisstream.controller;

import com.example.redisstream.dto.ApiResponse;
import com.example.redisstream.service.RedisAdvancedUseCasesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 演示 Redis 高级数据结构 (Bitmap, HyperLogLog) API
 */
@RestController
@RequestMapping("/api/redis-usecases/advanced")
public class RedisAdvancedUseCasesController {

    @Autowired
    private RedisAdvancedUseCasesService redisAdvancedUseCasesService;

    // --- 1. Bitmap 签到场景 ---

    @PostMapping("/bitmap/sign/{userId}")
    public ApiResponse<String> sign(
            @PathVariable String userId, 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        boolean isFirstSign = redisAdvancedUseCasesService.sign(userId, date);
        if (isFirstSign) {
            return ApiResponse.success("签到成功", "积分+10");
        } else {
            return ApiResponse.success("今日已签到过，无需重复签到", null);
        }
    }

    @GetMapping("/bitmap/sign/{userId}/check")
    public ApiResponse<Boolean> checkSign(
            @PathVariable String userId, 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        boolean isSigned = redisAdvancedUseCasesService.checkSign(userId, date);
        return ApiResponse.success("查询签到状态成功", isSigned);
    }

    @GetMapping("/bitmap/sign/{userId}/count")
    public ApiResponse<Long> countSign(
            @PathVariable String userId, 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        long count = redisAdvancedUseCasesService.countSign(userId, date);
        return ApiResponse.success("本月累计签到天数", count);
    }

    // --- 2. HyperLogLog 亿级 UV 统计场景 ---

    @PostMapping("/hll/uv/{pageId}/visit")
    public ApiResponse<String> visitPageHll(
            @PathVariable String pageId, 
            @RequestParam String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        redisAdvancedUseCasesService.recordHllUv(pageId, userId, date);
        return ApiResponse.success("HyperLogLog 访问记录成功", null);
    }

    @GetMapping("/hll/uv/{pageId}/count")
    public ApiResponse<Long> getUvCountHll(
            @PathVariable String pageId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        long count = redisAdvancedUseCasesService.getHllUvCount(pageId, date);
        return ApiResponse.success("HyperLogLog 获取 UV 估算值成功", count);
    }
}
