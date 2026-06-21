package com.example.redisstream.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局跨域配置。
 *
 * 说明：
 * - 前端 Vue 开发服务器默认运行在 http://localhost:5173。
 * - 后端运行在 http://localhost:8080。
 * - 浏览器同源策略会阻止跨域请求，因此需要配置 CORS。
 *
 * 注意：生产环境请把 allowedOrigins 改为真实域名，不建议用 *。
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        // 允许前端开发服务器访问
                        .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
                        // 允许的 HTTP 方法
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        // 允许所有请求头
                        .allowedHeaders("*")
                        // 允许携带 cookie（本项目暂时不用，但开启无坏处）
                        .allowCredentials(true)
                        // 预检请求缓存 1 小时
                        .maxAge(3600);
            }
        };
    }
}
