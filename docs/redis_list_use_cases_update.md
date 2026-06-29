# Redis List 数据结构经典应用场景实战更新说明

## 1. 更新概述
本次更新在项目中引入了 Redis **List（列表）** 数据结构的经典应用场景的后端实战代码。重点演示了如何利用 List 实现**轻量级消息队列**，以及如何构建高效的**最新列表（时间线 Feed 流）** 场景。

## 2. 新增文件及功能模块说明

### 2.1 业务逻辑层 (Service)
**文件路径**：`src/main/java/com/example/redisstream/service/RedisListUseCasesService.java`
该服务类基于 `StringRedisTemplate.opsForList()` 实现了以下两个核心场景：
1. **轻量级消息队列**：List 的底层是一个双向链表，非常适合做 FIFO（先进先出）队列。
   - **生产者**：使用 `LPUSH` 将消息推入列表的最左侧。
   - **消费者**：使用阻塞式的 `BRPOP`（Spring 中通过带 `timeout` 参数的 `rightPop` 实现）从最右侧消费消息。如果没有消息，消费者会挂起等待指定的超时时间，这避免了 `RPOP` 轮询造成的 CPU 资源浪费。
2. **最新列表 / 时间线**：例如微博动态、论坛的最新回复列表等。
   - **新增动态并截断**：每次有新记录，使用 `LPUSH` 插入到头部，紧接着使用 `LTRIM` 限制整个列表的最大长度（如始终只保留最新的 50 条）。这样既保证了列表的“新鲜度”，又防止了无效旧数据无限堆积占用过多内存。
   - **分页获取**：使用 `LRANGE` 基于索引范围（start, end）来拉取数据，非常契合前端的下拉分页请求。

### 2.2 API 接口层 (Controller)
**文件路径**：`src/main/java/com/example/redisstream/controller/RedisListUseCasesController.java`
暴露了测试 API：

**消息队列测试：**
- **生产消息**：`POST /api/redis-usecases/list/queue/{queueName}/send?message=hello`
- **消费消息（阻塞式）**：`GET /api/redis-usecases/list/queue/{queueName}/receive?timeoutSeconds=10`
*(测试技巧：先开一个终端或窗口请求消费接口，由于没消息它会卡住等待（阻塞）；再开另一个窗口请求生产消息接口，观察第一个窗口瞬间得到响应的效果。)*

**最新列表测试：**
- **发布新动态**：`POST /api/redis-usecases/list/timeline/{timelineKey}/add?record=文章1的评论&maxKeep=3`
- **获取最新动态**：`GET /api/redis-usecases/list/timeline/{timelineKey}?start=0&end=9`
*(测试技巧：你可以尝试向相同的 `timelineKey` 连续 POST 4 条以上的记录，因为 `maxKeep=3`，然后再 GET 获取列表，你会发现最早发出的那条已经被自动剔除了，列表中永远只有最新的 3 条。)*

## 3. 运行与验证
项目运行后，使用 Postman 或是浏览器按照文档中的 URL 直接发起对应请求即可，所有接口均已配置好并可以真实联动 Redis 服务测试效果。
