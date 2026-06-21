package com.example.redisstream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.example.redisstream.config.StreamProperties;

/**
 * 项目启动入口。
 *
 * 注解说明：
 * - @SpringBootApplication：开启自动配置、组件扫描、Spring MVC。
 * - @EnableScheduling：开启定时任务，用于 pending 消息巡检和监控上报。
 * - @EnableConfigurationProperties：将 StreamProperties 注册到 Spring 容器，
 *   使得 application.yml 中的 app.stream.* 配置可以被自动注入。
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(StreamProperties.class)
public class RedisStreamDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedisStreamDemoApplication.class, args);
    }
}
