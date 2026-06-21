package com.example.redisstream.config;

import com.example.redisstream.service.consumer.AbstractStreamConsumer;
import com.example.redisstream.support.RedisStreamOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Redis 核心配置类。
 *
 * 职责：
 * 1. 创建消费者组（若不存在）。
 * 2. 构建 StreamMessageListenerContainer，并注册所有 AbstractStreamConsumer 子类。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    private final RedisConnectionFactory redisConnectionFactory;
    private final StringRedisTemplate stringRedisTemplate;
    private final StreamProperties streamProperties;
    private final RedisStreamOperator redisStreamOperator;
    private final List<AbstractStreamConsumer> consumers;

    /**
     * 应用启动后，检查并创建所有消费者组。
     *
     * 为什么在这里创建？
     * - 消费者组必须存在，XREADGROUP 才能正常消费。
     * - 如果组已存在，Redis 会返回 BUSYGROUP 错误，这里做了捕获忽略。
     */
    @PostConstruct
    public void initConsumerGroups() {
        for (AbstractStreamConsumer consumer : consumers) {
            redisStreamOperator.createGroup(consumer.getGroupName());
        }
    }

    /**
     * 构建并注册 Stream 监听容器。
     *
     * 关键点：
     * - 每个消费者组/消费者对应一个 subscription。
     * - 使用自定义线程池，避免阻塞 Spring 公共线程。
     * - ReadOffset.lastConsumed() 表示从上次已消费位置继续（断点续传）。
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public StreamMessageListenerContainer<String,
            org.springframework.data.redis.connection.stream.MapRecord<String, String, String>> streamMessageListenerContainer() {

        // 每个消费者占用一个线程，用于轮询 Redis
        Executor executor = Executors.newFixedThreadPool(Math.max(2, consumers.size()));

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String,
                org.springframework.data.redis.connection.stream.MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .<String, org.springframework.data.redis.connection.stream.MapRecord<String, String, String>>builder()
                        .pollTimeout(Duration.ofSeconds(1))   // 每次轮询最多阻塞/等待 1 秒
                        .executor(executor)
                        // 出错时由 ErrorHandler 捕获，避免消费线程退出；
                        // 上下文关闭时的 "Connection closed" 属于正常销毁噪音，忽略。
                        .errorHandler(t -> {
                            if (t instanceof RedisSystemException
                                    && t.getMessage() != null
                                    && t.getMessage().contains("Connection closed")) {
                                log.debug("Stream 监听连接已关闭，忽略：{}", t.getMessage());
                            } else {
                                log.error("Stream 监听异常", t);
                            }
                        })
                        .build();

        StreamMessageListenerContainer<String,
                org.springframework.data.redis.connection.stream.MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(redisConnectionFactory, options);

        // 为每个消费者注册订阅
        for (AbstractStreamConsumer consumer : consumers) {
            Consumer c = Consumer.from(consumer.getGroupName(), consumer.getConsumerName());
            StreamOffset<String> offset = StreamOffset.create(streamProperties.getKey(), ReadOffset.lastConsumed());
            container.receive(c, offset, consumer);
            log.info("注册消费者：group={}, consumer={}", consumer.getGroupName(), consumer.getConsumerName());
        }

        return container;
    }
}
