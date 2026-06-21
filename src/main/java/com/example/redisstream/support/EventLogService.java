package com.example.redisstream.support;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 将关键业务日志记录到 Redis 中，供前端界面展示。
 */
@Service
@RequiredArgsConstructor
public class EventLogService {

    private final StringRedisTemplate stringRedisTemplate;
    private static final String LOG_KEY = "order:ui:logs";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * @param type    日志类型 (success, warning, danger, info)
     * @param message 描述内容
     */
    public void log(String type, String message) {
        String time = LocalDateTime.now().format(FMT);
        // 为了方便，直接拼 JSON 存入 List
        // 转义 message 中的双引号以防 JSON 语法错误
        String safeMessage = message.replace("\"", "\\\"");
        String json = String.format("{\"type\":\"%s\",\"message\":\"%s\",\"time\":\"%s\"}", type, safeMessage, time);
        
        stringRedisTemplate.opsForList().leftPush(LOG_KEY, json);
        // 只保留最新的 50 条日志
        stringRedisTemplate.opsForList().trim(LOG_KEY, 0, 49);
    }
}
