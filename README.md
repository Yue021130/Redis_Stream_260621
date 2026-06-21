# Redis Stream 消息队列学习项目（Java 8 + Spring Boot 2.7）

> 一个**完整、可运行、带详细中文注释**的 Redis Stream 消息队列学习项目。
> 通过模拟“电商订单 -> 扣库存 + 发短信”的真实业务场景，覆盖 Redis Stream 的核心知识点。

## 目录

- [技术栈](#技术栈)
- [前置条件](#前置条件)
- [快速启动](#快速启动)
- [项目结构](#项目结构)
- [核心知识点索引](#核心知识点索引)
- [业务场景说明](#业务场景说明)
- [REST API 列表](#rest-api-列表)
- [手动测试示例](#手动测试示例)
- [观察重试与死信队列](#观察重试与死信队列)
- [常见问题](#常见问题)
- [学习建议](#学习建议)

## 技术栈

- Java 8
- Spring Boot 2.7.18
- Spring Data Redis 2.7.x（Lettuce 驱动）
- Maven 3.6+
- Redis 5.0+（本地安装，默认 `localhost:6379`）

## 前置条件

1. 安装 JDK 8 并配置 `JAVA_HOME`。
2. 安装 Maven。
3. 安装并启动 Redis：
   ```bash
   redis-server
   ```
   确认 Redis 可访问：
   ```bash
   redis-cli ping
   # 返回 PONG 表示正常
   ```

## 快速启动

```bash
# 1. 进入项目目录
cd C:\Users\86159\Desktop\ZSX_yue021130

# 2. 编译并运行
mvn clean spring-boot:run
```

启动成功后，控制台会打印：

```
注册消费者：group=order:group:inventory, consumer=consumer-inventory
注册消费者：group=order:group:sms, consumer=consumer-sms
```

## 项目结构

```
├── pom.xml
├── README.md
├── docs/
│   ├── 01-redis-stream-theory.md    # Stream 理论知识
│   ├── 02-architecture.md           # 架构与时序图
│   └── 03-api-and-test.md           # API 与测试
└── src/
    ├── main/java/com/example/redisstream/
    │   ├── config/RedisConfig.java              # Redis、监听容器配置
    │   ├── constants/StreamConstants.java       # 常量
    │   ├── controller/MessageController.java    # REST 调试接口
    │   ├── dto/                                 # 数据传输对象
    │   ├── exception/GlobalExceptionHandler.java
    │   ├── service/producer/OrderProducerService.java
    │   ├── service/consumer/AbstractStreamConsumer.java
    │   ├── service/consumer/InventoryConsumer.java
    │   ├── service/consumer/SmsConsumer.java
    │   ├── support/RedisStreamOperator.java
    │   ├── support/PendingMessageProcessor.java # pending 巡检/重试/DLQ
    │   ├── support/IdempotentService.java       # 业务幂等
    │   └── task/StreamMonitorTask.java          # 定时监控
    ├── main/resources/application.yml
    └── test/...
```

## 核心知识点索引

| 知识点 | 对应代码/文档 | 说明 |
|--------|---------------|------|
| Stream 基本结构 | `docs/01-redis-stream-theory.md` | stream key、entry id、field-value |
| XADD 生产者 | `OrderProducerService.java` | 把订单消息写入 `order:stream` |
| 消费者组 | `RedisConfig.java`、`InventoryConsumer.java`、`SmsConsumer.java` | 两个组独立消费同一 stream |
| ACK 确认 | `AbstractStreamConsumer.java` | 业务成功后 `XACK`，异常不 ACK |
| Pending List | `PendingMessageProcessor.java` | `XPENDING` 查询未确认消息 |
| XCLAIM 重试 | `PendingMessageProcessor.java` | 把 idle 过长的消息 claim 给其他消费者 |
| 死信队列 DLQ | `PendingMessageProcessor.java`、`RedisStreamOperator.java` | 超最大重试次数后写入 `order:stream:dlq` |
| 业务幂等 | `IdempotentService.java` | 使用 Redis SETNX 防止重复处理 |
| Stream 长度控制 | `RedisStreamOperator.trimStream()` | `XTRIM MAXLEN` 防止无限增长 |
| 监控 | `StreamMonitorTask.java`、`/api/order/stats` | 定时输出 stream 长度与 pending 数量 |
| 异常处理 | `GlobalExceptionHandler.java` | REST 统一异常封装 |

## 业务场景说明

```
用户下单
   │
   ▼
OrderProducerService.send()
   │
   ▼
XADD order:stream * payload {...订单 JSON...}
   │
   ├──────────────┐
   ▼              ▼
库存消费者组    短信消费者组
order:group:inventory  order:group:sms
   │                      │
   ▼                      ▼
扣减库存              发送短信
   │                      │
   ▼                      ▼
XACK                   XACK
```

- 一个订单消息会同时被两个消费者组消费，互不影响。
- 每个消费者组内部可以启动多个应用实例，Redis 会自动做负载均衡。
- 如果业务处理失败，消息不 ACK，进入 pending list，等待重试或进入死信队列。

## REST API 列表

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/order/send` | 发送单条订单消息 |
| POST | `/api/order/send/batch` | 批量发送消息 |
| POST | `/api/order/send/demo` | 发送一条测试消息 |
| GET  | `/api/order/pending?group=order:group:inventory` | 查看某组 pending 消息 |
| GET  | `/api/order/dlq` | 查看死信队列 |
| GET  | `/api/order/stats` | 查看 stream 统计信息 |

## 手动测试示例

### 1. 发送一条测试消息

```bash
curl -X POST http://localhost:8080/api/order/send/demo
```

返回示例：

```json
{
  "code": 200,
  "message": "测试消息发送成功",
  "data": "1719999999999-0"
}
```

### 2. 查看消费状态

```bash
curl http://localhost:8080/api/order/stats
```

### 3. 查看 pending 消息

```bash
curl "http://localhost:8080/api/order/pending?group=order:group:inventory"
```

## 观察重试与死信队列

修改 `application.yml`：

```yaml
app:
  stream:
    simulate-failure: true
    failure-rate: 0.5
    max-retries: 2
```

重新启动后发送消息，观察日志：

1. 消费者打印“业务处理失败，消息进入 pending”。
2. `PendingMessageProcessor` 定时 claim 并重试。
3. 超过 `max-retries` 后，消息被写入 DLQ，并 ACK 移除 pending。
4. 访问 `/api/order/dlq` 可看到死信消息。

## 常见问题

**Q: 启动时报 `NOGROUP No such key 'order:stream' or consumer group ...`？**

A: 确认 `RedisConfig.initConsumerGroups()` 已执行。如果 Redis 中 stream 不存在，createGroup 会自动创建空 stream。若仍报错，可能是配置中 group 名称不一致。

**Q: 消息一直不消费？**

A: 检查 `app.stream.simulate-failure` 是否开启且异常未被正确抛出。如果 `AbstractStreamConsumer` catch 了异常但没抛出，消息会被 ACK，不会进入 pending。

**Q: 为什么不用 ObjectRecord 而用 MapRecord？**

A: 为了降低学习门槛并避免序列化配置带来的兼容性问题。本项目把 JSON 作为字符串存在 `payload` 字段中，MapRecord 更直观。文档中也说明了 ObjectRecord 的用法。

**Q: 怎么清空测试数据？**

A: 连接 redis-cli 执行：

```bash
DEL order:stream order:stream:dlq order:group:inventory order:group:sms
```

## 学习建议

1. 先跑通 `mvn spring-boot:run`，发送 demo 消息，看日志。
2. 打开 `application.yml` 开启 `simulate-failure`，观察 pending、claim、DLQ 链路。
3. 用 redis-cli 配合 `XINFO STREAM order:stream`、`XPENDING order:stream order:group:inventory` 查看底层数据结构。
4. 阅读 `docs/` 目录下的三篇文档，系统梳理理论知识。

---

祝学习愉快！如有问题，欢迎在代码注释中继续探索。
