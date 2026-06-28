package com.example.redisstream.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 演示 Redis List 经典使用场景：简单消息队列、最新列表(时间线)
 */
@Slf4j
@Service
public class RedisListUseCasesService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 1. 简单消息队列场景：生产者发送消息 (LPUSH)
     * 队列先进先出 (FIFO): 左侧压入，右侧弹出
     */
    public void sendMessageToQueue(String queueName, String message) {
        String key = "queue:" + queueName;
        // 左压入 (LPUSH)
        stringRedisTemplate.opsForList().leftPush(key, message);
        log.info("消息推送到队列成功: queue={}, message={}", queueName, message);
    }

    /**
     * 1. 简单消息队列场景：消费者接收消息 (BRPOP)
     * 这里演示阻塞式弹出，如果没有消息最多阻塞等待 timeoutSeconds 秒
     */
    public String receiveMessageFromQueue(String queueName, long timeoutSeconds) {
        String key = "queue:" + queueName;
        // 阻塞式右弹出 (对应 BRPOP)
        String message = stringRedisTemplate.opsForList().rightPop(key, timeoutSeconds, TimeUnit.SECONDS);
        log.info("从队列获取消息: queue={}, message={}", queueName, message);
        return message;
    }

    /**
     * 2. 最新列表/时间线场景：发布新内容并维持列表定长 (LPUSH + LTRIM)
     * 例如文章的最新评论列表，每次新增评论放到最前面，并保留最新的 maxKeep 条记录
     */
    public void addLatestRecord(String timelineKey, String record, int maxKeep) {
        String key = "timeline:" + timelineKey;
        // 1. 把新记录插到头部 (LPUSH)
        stringRedisTemplate.opsForList().leftPush(key, record);
        // 2. 截断列表 (LTRIM)，只保留索引 0 到 maxKeep - 1 的元素，防止内存无限膨胀
        stringRedisTemplate.opsForList().trim(key, 0, maxKeep - 1);
        log.info("新增最新记录并截断: timeline={}, record={}, 保持最多={}条", timelineKey, record, maxKeep);
    }

    /**
     * 2. 最新列表/时间线场景：获取最新列表 (LRANGE)
     * 可以配合 start 和 end 实现简单的分页
     */
    public List<String> getLatestRecords(String timelineKey, long start, long end) {
        String key = "timeline:" + timelineKey;
        List<String> records = stringRedisTemplate.opsForList().range(key, start, end);
        log.info("获取最新记录列表: timeline={}, start={}, end={}, size={}", timelineKey, start, end, (records != null ? records.size() : 0));
        return records;
    }
}
