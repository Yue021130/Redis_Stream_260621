package com.example.redisstream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * application.yml 中 app.stream.* 配置项的映射类。
 *
 * 通过 @ConfigurationProperties 批量绑定，避免在代码中写死 key/group。
 * 所有字段都有默认值，即使不配也能跑起来。
 */
@Data
@ConfigurationProperties(prefix = "app.stream")
public class StreamProperties {

    /** Stream 主 key */
    private String key = "order:stream";

    /** 多个消费者组，key 为业务标识，value 为 Redis 中的 group name */
    private Map<String, String> groups = new HashMap<String, String>() {{
        put("inventory", "order:group:inventory");
        put("sms", "order:group:sms");
    }};

    /** 消费者名称前缀 */
    private String consumerPrefix = "consumer";

    /** Stream 最大长度，超过会被 XTRIM 截断 */
    private Long maxLen = 10000L;

    /** 业务失败最大重试次数，超过后进 DLQ */
    private Integer maxRetries = 3;

    /** 幂等键在 Redis 中的过期时间（秒） */
    private Long idempotentTtlSeconds = 86400L;

    /** pending 消息巡检间隔（毫秒） */
    private Long pendingIntervalMs = 10000L;

    /** 消息 idle 超过该值即可被 claim（毫秒） */
    private Long claimIdleMs = 30000L;

    /** 是否模拟业务失败，用于学习观察重试链路 */
    private Boolean simulateFailure = false;

    /** 模拟失败的概率 0.0~1.0 */
    private Double failureRate = 0.3;

    /**
     * 死信队列（DLQ）Stream key。
     * 这里采用派生方式，保证与主 stream 名称一致。
     */
    public String getDlqKey() {
        return key + ":dlq";
    }
}
