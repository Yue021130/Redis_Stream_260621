package com.example.redisstream.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 演示 Redis 经典使用场景：缓存、计数器、Session、限流
 */
@Slf4j
@Service
public class RedisUseCasesService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 1. 缓存（Cache）场景：模拟获取用户信息
     * 先查 Redis，没有再查数据库并回写 Redis
     */
    public String getUserInfoWithCache(String userId) {
        String cacheKey = "user:info:" + userId;
        // 1. 查询缓存
        String userInfo = stringRedisTemplate.opsForValue().get(cacheKey);
        if (userInfo != null) {
            log.info("从Redis缓存中获取到用户信息: {}", userInfo);
            return userInfo;
        }

        // 2. 模拟从数据库查询
        log.info("Redis缓存未命中，模拟从数据库查询用户信息...");
        userInfo = "User_Name_" + userId + "_from_DB";

        // 3. 写入缓存，并设置过期时间（例如1小时），防止缓存雪崩可以加个随机时间
        stringRedisTemplate.opsForValue().set(cacheKey, userInfo, 1, TimeUnit.HOURS);
        log.info("将用户信息回写到Redis缓存中");

        return userInfo;
    }

    /**
     * 2. 计数器（Counter）场景：模拟统计文章阅读量
     */
    public Long incrementArticleViews(String articleId) {
        String counterKey = "article:views:" + articleId;
        // 每次调用增加1
        Long views = stringRedisTemplate.opsForValue().increment(counterKey);
        log.info("文章 {} 的最新阅读量为: {}", articleId, views);
        return views;
    }

    /**
     * 3. Session 集中管理：模拟用户登录生成Token并存储在Redis中
     */
    public String loginAndCreateSession(String userId) {
        // 生成随机 Token
        String token = UUID.randomUUID().toString().replace("-", "");
        String sessionKey = "session:token:" + token;

        // 将 Token 作为 key，userId 作为 value 存入 Redis，并设置有效期（如30分钟）
        stringRedisTemplate.opsForValue().set(sessionKey, userId, 30, TimeUnit.MINUTES);
        log.info("用户 {} 登录成功，生成Token并存入Redis，有效期30分钟", userId);
        
        return token;
    }

    /**
     * 3. Session 集中管理：模拟验证Token并获取当前登录用户
     */
    public String checkSession(String token) {
        String sessionKey = "session:token:" + token;
        String userId = stringRedisTemplate.opsForValue().get(sessionKey);
        
        if (userId != null) {
            // 验证通过后可以顺便续期（例如重新设置为30分钟）
            stringRedisTemplate.expire(sessionKey, 30, TimeUnit.MINUTES);
            log.info("Token验证通过，当前用户为: {}", userId);
            return userId;
        } else {
            log.warn("Token已失效或不存在: {}", token);
            return null;
        }
    }

    /**
     * 4. 限流（Rate Limiting）场景：简单计数器限流（限制某用户在一定时间窗口内的接口调用次数）
     */
    public boolean checkRateLimit(String userId, String action, int maxRequests, int windowSeconds) {
        // key格式：rate:limit:{userId}:{action}
        String limitKey = String.format("rate:limit:%s:%s", userId, action);

        // 每次访问增加1
        Long currentRequests = stringRedisTemplate.opsForValue().increment(limitKey);

        if (currentRequests != null && currentRequests == 1L) {
            // 第一次访问，设置过期时间（时间窗口）
            stringRedisTemplate.expire(limitKey, windowSeconds, TimeUnit.SECONDS);
        }

        if (currentRequests != null && currentRequests > maxRequests) {
            log.warn("用户 {} 对 {} 的请求频率超限，当前 {}/{}次", userId, action, currentRequests, maxRequests);
            return false;
        }

        log.info("用户 {} 访问 {}，当前请求次数 {}/{}", userId, action, currentRequests, maxRequests);
        return true;
    }
}
