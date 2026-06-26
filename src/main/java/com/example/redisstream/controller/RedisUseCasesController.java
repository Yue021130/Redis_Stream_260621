package com.example.redisstream.controller;

import com.example.redisstream.dto.ApiResponse;
import com.example.redisstream.service.RedisUseCasesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 演示 Redis 经典使用场景 API
 */
@RestController
@RequestMapping("/api/redis-usecases")
public class RedisUseCasesController {

    @Autowired
    private RedisUseCasesService redisUseCasesService;

    // 1. 缓存场景：获取用户信息
    @GetMapping("/cache/user/{userId}")
    public ApiResponse<String> getUserInfo(@PathVariable String userId) {
        String userInfo = redisUseCasesService.getUserInfoWithCache(userId);
        return ApiResponse.success("获取成功", userInfo);
    }

    // 2. 计数器场景：增加文章阅读量
    @PostMapping("/counter/article/{articleId}/view")
    public ApiResponse<Long> viewArticle(@PathVariable String articleId) {
        Long views = redisUseCasesService.incrementArticleViews(articleId);
        return ApiResponse.success("增加阅读量成功", views);
    }

    // 3. Session 集中管理场景：模拟登录生成Token
    @PostMapping("/session/login")
    public ApiResponse<String> login(@RequestParam String userId) {
        String token = redisUseCasesService.loginAndCreateSession(userId);
        return ApiResponse.success("登录成功", token);
    }

    // 3. Session 集中管理场景：模拟验证Token
    @GetMapping("/session/check")
    public ApiResponse<String> checkSession(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ApiResponse.error(401, "缺少Authorization头");
        }
        String userId = redisUseCasesService.checkSession(token);
        if (userId != null) {
            return ApiResponse.success("Token有效，当前登录用户为", userId);
        } else {
            return ApiResponse.error(401, "Token无效或已过期，请重新登录");
        }
    }

    // 4. 限流场景：模拟接口限流测试
    @GetMapping("/ratelimit/test")
    public ApiResponse<String> testRateLimit(@RequestParam String userId) {
        // 限制同一个 userId 每分钟（60秒）最多访问 5 次
        boolean allowed = redisUseCasesService.checkRateLimit(userId, "testAction", 5, 60);
        if (allowed) {
            return ApiResponse.success("访问成功", "业务数据响应成功...");
        } else {
            return ApiResponse.error(429, "请求太频繁，请稍后再试！");
        }
    }
}
