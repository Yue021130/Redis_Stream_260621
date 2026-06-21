package com.example.redisstream.support;

import com.example.redisstream.constants.StreamConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 业务级幂等支持。
 *
 * 核心思路：
 * 1. 消费者拿到消息后，先检查该 orderId 是否已经处理过。
 * 2. 若未处理，使用 Redis SETNX（setIfAbsent）原子地设置一个标记。
 * 3. 设置成功后执行业务；设置失败说明其他实例或上次重试已处理，直接 ACK。
 *
 * 注意：
 * - 这里的幂等是“业务幂等”，和 Stream 的 ACK 不是一回事。
 * - ACK 是告诉 Redis 这条消息我已经消费了，可以删除 pending。
 * - 业务幂等是防止同一业务 ID 被重复执行（如重复扣库存、重复发短信）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试标记某个业务 ID 为“处理中/已处理”。
     *
     * @param orderId 业务 ID
     * @param ttlSeconds 过期时间
     * @return true 表示首次设置成功；false 表示已存在
     */
    public boolean tryLock(String orderId, long ttlSeconds) {
        String key = StreamConstants.IDEMPOTENT_PREFIX + orderId;
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(success);
    }

    /**
     * 标记为已处理（可选，通常在 tryLock 成功后不需要再调用，因为 TTL 内都会存在）。
     */
    public void markProcessed(String orderId, long ttlSeconds) {
        String key = StreamConstants.IDEMPOTENT_PREFIX + orderId;
        stringRedisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds));
    }

    /**
     * 判断某个业务 ID 是否已处理。
     */
    public boolean isProcessed(String orderId) {
        String key = StreamConstants.IDEMPOTENT_PREFIX + orderId;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }
}
