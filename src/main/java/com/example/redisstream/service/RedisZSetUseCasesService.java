package com.example.redisstream.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 演示 Redis ZSet (有序集合) 经典使用场景：排行榜、延时队列
 */
@Slf4j
@Service
public class RedisZSetUseCasesService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 1. 排行榜场景：增加或修改积分 (ZINCRBY)
     * 例如游戏积分排行榜，阅读量排行榜等。如果用户不存在会自动添加。
     */
    public void addScore(String boardName, String userId, double score) {
        String key = "leaderboard:" + boardName;
        // 增量添加积分
        stringRedisTemplate.opsForZSet().incrementScore(key, userId, score);
        log.info("排行榜积分增加: board={}, userId={}, addScore={}", boardName, userId, score);
    }

    /**
     * 1. 排行榜场景：获取 TOP N 排行 (ZREVRANGE)
     * 降序排列获取前 N 名及其分数
     */
    public Set<String> getTopPlayers(String boardName, int topN) {
        String key = "leaderboard:" + boardName;
        // ZREVRANGE: 从大到小排序返回 (带上 score)
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, 0, topN - 1);
        
        log.info("获取排行榜 TOP {}: {}", topN, tuples);
        
        if (tuples == null) return null;
        
        // 格式化输出方便接口查看
        return tuples.stream()
                .map(tuple -> "User: " + tuple.getValue() + " -> Score: " + tuple.getScore())
                .collect(Collectors.toSet());
    }

    /**
     * 2. 延时队列场景：发送延时任务 (ZADD)
     * 核心思想：将预计要执行的未来【时间戳】作为 score 存入 ZSet
     */
    public void sendDelayedTask(String queueName, String taskId, long delaySeconds) {
        String key = "delayed:queue:" + queueName;
        long executeTime = System.currentTimeMillis() + (delaySeconds * 1000);
        stringRedisTemplate.opsForZSet().add(key, taskId, executeTime);
        log.info("投递延时任务成功: queue={}, taskId={}, 将在 {} 秒后执行", queueName, taskId, delaySeconds);
    }

    /**
     * 2. 延时队列场景：轮询获取到期的任务 (ZRANGEBYSCORE)
     * 实际业务中这个方法通常在一个定时任务 (如 Spring 的 @Scheduled) 中每秒不断轮询
     */
    public Set<String> fetchDueTasks(String queueName) {
        String key = "delayed:queue:" + queueName;
        long now = System.currentTimeMillis();
        
        // 获取 score (时间戳) 小于等于当前时间的所有任务，代表已经到期需要执行
        Set<String> dueTasks = stringRedisTemplate.opsForZSet().rangeByScore(key, 0, now);
        
        if (dueTasks != null && !dueTasks.isEmpty()) {
            // 取出后需要将其从队列中删除 (ZREM)，防止重复执行
            // (注：生产高并发环境中，"rangeByScore + remove" 为了防止并发抢夺，通常会使用 Lua 脚本保证原子性)
            stringRedisTemplate.opsForZSet().remove(key, dueTasks.toArray());
            log.info("获取并移除了到期的延时任务: {}", dueTasks);
        }
        
        return dueTasks;
    }
}
