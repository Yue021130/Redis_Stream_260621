package com.example.redisstream.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 演示 Redis Set 经典使用场景：去重统计、抽奖系统、社交关系(交集/并集/差集)
 */
@Slf4j
@Service
public class RedisSetUseCasesService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 1. 独立访客统计 (去重)
     * 利用 Set 元素不可重复的特性，记录文章或页面的 UV
     */
    public void recordUniqueVisit(String pageId, String userId) {
        String key = "page:uv:" + pageId;
        // 如果用户已存在，SADD 返回 0，否则返回 1
        Long added = stringRedisTemplate.opsForSet().add(key, userId);
        log.info("记录 UV: pageId={}, userId={}, isNewVisit={}", pageId, userId, added != null && added > 0);
    }

    public Long getUniqueVisitCount(String pageId) {
        String key = "page:uv:" + pageId;
        return stringRedisTemplate.opsForSet().size(key);
    }

    /**
     * 2. 抽奖系统场景
     */
    public void joinLottery(String activityId, String userId) {
        String key = "lottery:" + activityId;
        stringRedisTemplate.opsForSet().add(key, userId);
        log.info("用户参与抽奖: activity={}, userId={}", activityId, userId);
    }

    public List<String> drawLottery(String activityId, int count) {
        String key = "lottery:" + activityId;
        // 随机抽取指定数量的人 (SRANDMEMBER) - 抽完不剔除，可重复参与下轮
        // 如果要抽完即中奖后不能再中，应使用 SPOP (随机弹出)
        List<String> winners = stringRedisTemplate.opsForSet().randomMembers(key, count);
        log.info("抽奖结果揭晓: activity={}, winners={}", activityId, winners);
        return winners;
    }

    /**
     * 3. 社交关系 - 关注列表与共同好友 (交并差)
     */
    public void follow(String userId, String targetUserId) {
        String key = "user:" + userId + ":follows";
        stringRedisTemplate.opsForSet().add(key, targetUserId);
    }

    public Set<String> getCommonFollows(String userId1, String userId2) {
        String key1 = "user:" + userId1 + ":follows";
        String key2 = "user:" + userId2 + ":follows";
        // 求交集 (SINTER)
        Set<String> common = stringRedisTemplate.opsForSet().intersect(key1, key2);
        log.info("计算共同关注: user1={}, user2={}, common={}", userId1, userId2, common);
        return common;
    }
}
