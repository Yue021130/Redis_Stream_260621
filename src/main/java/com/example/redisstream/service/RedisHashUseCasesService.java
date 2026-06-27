package com.example.redisstream.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 演示 Redis Hash 经典使用场景：对象结构化缓存、购物车
 */
@Slf4j
@Service
public class RedisHashUseCasesService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 1. 对象缓存场景：存储和部分更新用户对象属性
     * Hash 结构：Key: user:hash:{userId}, Field: 属性名(name, age, points等), Value: 属性值
     */
    public void saveUserObject(String userId, Map<String, String> userInfo) {
        String key = "user:hash:" + userId;
        stringRedisTemplate.opsForHash().putAll(key, userInfo);
        log.info("保存用户对象信息到 Hash 成功: userId={}, info={}", userId, userInfo);
    }

    /**
     * 演示 Hash 的优势：部分字段更新（比如只更新用户的积分）
     * 不需要取出整个对象反序列化，直接在 Redis 端进行原子累加
     */
    public long updateUserPoint(String userId, long pointsDelta) {
        String key = "user:hash:" + userId;
        long newPoints = stringRedisTemplate.opsForHash().increment(key, "points", pointsDelta);
        log.info("更新用户积分成功: userId={}, 新积分={}", userId, newPoints);
        return newPoints;
    }

    public Map<Object, Object> getUserObject(String userId) {
        String key = "user:hash:" + userId;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        log.info("获取用户对象信息: userId={}, info={}", userId, entries);
        return entries;
    }

    /**
     * 2. 购物车场景
     * Hash 结构：Key: cart:{userId}, Field: productId, Value: quantity
     */
    public void addToCart(String userId, String productId, int quantity) {
        String key = "cart:" + userId;
        // 如果商品已存在，则累加数量；否则新增
        stringRedisTemplate.opsForHash().increment(key, productId, quantity);
        log.info("添加商品到购物车成功: userId={}, productId={}, 增加数量={}", userId, productId, quantity);
    }

    public void removeFromCart(String userId, String productId) {
        String key = "cart:" + userId;
        stringRedisTemplate.opsForHash().delete(key, productId);
        log.info("从购物车移除商品成功: userId={}, productId={}", userId, productId);
    }

    public Map<Object, Object> getCartItems(String userId) {
        String key = "cart:" + userId;
        Map<Object, Object> cartItems = stringRedisTemplate.opsForHash().entries(key);
        log.info("获取购物车所有商品: userId={}, items={}", userId, cartItems);
        return cartItems;
    }
}
