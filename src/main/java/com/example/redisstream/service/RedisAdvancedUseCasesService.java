package com.example.redisstream.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 演示 Redis 高级数据结构：Bitmap (位图)、HyperLogLog (基数统计)
 */
@Slf4j
@Service
public class RedisAdvancedUseCasesService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // ==========================================
    // 1. Bitmap (位图) - 海量用户签到
    // ==========================================

    /**
     * 用户签到
     * Bitmap 的 key 设计：sign:{userId}:{yyyyMM}
     * offset 是当月的第几天 (1-31)，减1映射到 0-30
     */
    public boolean sign(String userId, LocalDate date) {
        String key = buildSignKey(userId, date);
        int offset = date.getDayOfMonth() - 1; // 0-indexed
        
        // setBit 返回的是该位原来的值，如果是 false 说明是今天第一次签到
        Boolean isSignedBefore = stringRedisTemplate.opsForValue().setBit(key, offset, true);
        boolean isFirstSign = (isSignedBefore == null || !isSignedBefore);
        
        log.info("用户签到: userId={}, date={}, offset={}, isFirstSign={}", userId, date, offset, isFirstSign);
        return isFirstSign;
    }

    /**
     * 检查用户某天是否签到
     */
    public boolean checkSign(String userId, LocalDate date) {
        String key = buildSignKey(userId, date);
        int offset = date.getDayOfMonth() - 1;
        Boolean isSigned = stringRedisTemplate.opsForValue().getBit(key, offset);
        return isSigned != null && isSigned;
    }

    /**
     * 统计当月总签到次数 (BITCOUNT)
     * 注意：Spring Data Redis 的 opsForValue() 原生不直接暴露 BITCOUNT 方法。
     * 因此使用 execute 回调底层的 RedisConnection 来执行。
     */
    public long countSign(String userId, LocalDate date) {
        String key = buildSignKey(userId, date);
        Long count = stringRedisTemplate.execute((org.springframework.data.redis.connection.RedisCallback<Long>) connection -> 
            connection.bitCount(key.getBytes())
        );
        log.info("统计用户当月签到次数: userId={}, month={}, count={}", userId, date.format(DateTimeFormatter.ofPattern("yyyyMM")), count);
        return count == null ? 0 : count;
    }

    private String buildSignKey(String userId, LocalDate date) {
        return String.format("sign:%s:%s", userId, date.format(DateTimeFormatter.ofPattern("yyyyMM")));
    }


    // ==========================================
    // 2. HyperLogLog - 亿级 UV 去重统计
    // ==========================================

    /**
     * 记录页面访问 (自动去重)
     * Key 设计: hll:uv:{pageId}:{yyyyMMdd}
     */
    public void recordHllUv(String pageId, String userId, LocalDate date) {
        String key = String.format("hll:uv:%s:%s", pageId, date.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        // PFADD
        stringRedisTemplate.opsForHyperLogLog().add(key, userId);
        log.info("HyperLogLog 记录访问: page={}, userId={}", pageId, userId);
    }

    /**
     * 获取页面的 UV 估算值 (PFCOUNT)
     */
    public long getHllUvCount(String pageId, LocalDate date) {
        String key = String.format("hll:uv:%s:%s", pageId, date.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        // PFCOUNT
        Long count = stringRedisTemplate.opsForHyperLogLog().size(key);
        log.info("HyperLogLog 统计UV: page={}, count={}", pageId, count);
        return count == null ? 0 : count;
    }
}
