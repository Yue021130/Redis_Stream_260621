# Redis String 数据结构经典应用场景实战更新说明

## 1. 更新概述
本次更新在项目中引入了 Redis 的四种经典应用场景的后端代码实现。目的是通过直接在项目内整合实战代码，演示如何在常见的实际业务中运用 Redis 提升性能、简化分布式开发。

## 2. 新增文件及功能模块说明

### 2.1 业务逻辑层 (Service)
**文件路径**：`src/main/java/com/example/redisstream/service/RedisUseCasesService.java`
该服务类基于 Spring Boot 内置的 `StringRedisTemplate` 实现了以下四个核心功能：
1. **缓存（Cache）**：模拟了典型的缓存旁路访问模式（Cache-Aside）。逻辑为：先查询 Redis 缓存，若未命中则模拟查询数据库并回写 Redis，同时为缓存数据设置了过期时间（1小时）以防止缓存数据永久滞留。
2. **计数器（Counter）**：利用 Redis 原生的 `INCR` 指令的单线程原子性，实现了对文章阅读量的精确递增操作，在高并发下也不会丢失计数。
3. **Session 集中管理（分布式 Session）**：提供了一套基于自定义 Token 的会话状态管理机制。模拟用户登录后生成全局唯一 Token，将 Token 为 Key 存入 Redis 并设置 30 分钟过期时间；同时也支持校验 Token 有效期及自动续期。
4. **限流（Rate Limiting）**：基于简单时间窗口和计数器算法，针对特定的用户行为（如调用测试接口）限制请求频率（例如：设定同一用户 60 秒内最多允许调用 5 次接口）。

### 2.2 API 接口层 (Controller)
**文件路径**：`src/main/java/com/example/redisstream/controller/RedisUseCasesController.java`
提供了一组基于 REST 风格的测试接口，方便直接体验上述功能：
- **缓存测试**：`GET /api/redis-usecases/cache/user/{userId}` 
- **计数器测试**：`POST /api/redis-usecases/counter/article/{articleId}/view`
- **发放 Token**：`POST /api/redis-usecases/session/login?userId={userId}`
- **验证 Token**：`GET /api/redis-usecases/session/check` (必须在 Header 附加 `Authorization: <Token>`)
- **限流测试**：`GET /api/redis-usecases/ratelimit/test?userId={userId}` (连续访问超过设定阈值会返回 HTTP 429 错误)

## 3. 运行与验证
1. 确保本地或远程的 Redis 服务器已正常启动，且项目 `application.yml` 中的 Redis 连接配置正确。
2. 启动本项目 (`RedisStreamDemoApplication`)。
3. 使用浏览器或 Postman 依次调用上述 API，观察控制台（Console）打印的日志，即可清晰感受到缓存命中/未命中、限流拦截等不同业务场景下的流转过程。
