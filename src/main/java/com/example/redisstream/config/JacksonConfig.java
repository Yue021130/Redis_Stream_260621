package com.example.redisstream.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 配置单独放在一个配置类，避免和 RedisConfig 产生循环依赖。
 *
 * 循环依赖场景：
 * - RedisConfig 需要注入 List<AbstractStreamConsumer>
 * - AbstractStreamConsumer 子类需要注入 ObjectMapper
 * - 如果 ObjectMapper 也定义在 RedisConfig 中，就会形成循环。
 */
@Configuration
public class JacksonConfig {

    /**
     * 自定义 ObjectMapper，支持 JDK8 日期时间序列化。
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // 禁用把时间写成时间戳，配合 @JsonFormat 使用字符串格式
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
