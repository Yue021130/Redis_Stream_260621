# 02 - 项目架构与时序图

## 1. 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    可视化监控前端监控台 (Vue 3)              │
│   (3秒轮询获取系统状态、消息流、事件日志，动态读取与修改配置)     │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTP 请求 (经 Vite 开发代理)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                        Spring Boot 应用                      │
│  ┌─────────────────┐        ┌───────────────────────────┐  │
│  │ MessageController│───────▶│ OrderProducerService      │  │
│  └─────────────────┘        └────────────┬──────────────┘  │
│                                           │                 │
│                                           ▼ XADD            │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Redis Server (localhost:6379)           │   │
│  │  ┌─────────────────────────────────────────────┐    │   │
│  │  │  Stream: order:stream                       │    │   │
│  │  │  ├─ 1719999999999-0 payload={...}           │    │   │
│  │  │  └─ ...                                     │    │   │
│  │  └─────────────────────────────────────────────┘    │   │
│  │                         │                           │   │
│  │        ┌────────────────┼────────────────┐          │   │
│  │        ▼                ▼                ▼          │   │
│  │  order:group:inventory  order:group:sms  order:stream:dlq │
│  │                         │                           │   │
│  │                         └─ order:ui:logs (UI日志列表)│   │
│  │  └─────────────────────────────────────────────────────┘   │
│  │                            │                                │
│  │        ┌───────────────────┘                                │
│  │        ▼ XREADGROUP                                         │
│  │  ┌─────────────────────────────────────────────────────┐   │
│  │  │         StreamMessageListenerContainer               │   │
│  │  │  ┌─────────────────────┐  ┌─────────────────────┐   │   │
│  │  │  │ InventoryConsumer   │  │ SmsConsumer         │   │   │
│  │  │  │ (group: inventory)  │  │ (group: sms)        │   │   │
│  │  │  └─────────────────────┘  └─────────────────────┘   │   │
│  │  └─────────────────────────────────────────────────────┘   │
│  │                            │                                │
│  │        ┌───────────────────┴───────────────────┐            │
│  │        ▼                                       ▼            │
│  │  IdempotentService                    PendingMessageProcessor│
│  │  (SETNX 幂等)                         (XPENDING/XCLAIM/DLQ) │
│  └─────────────────────────────────────────────────────────────┘
```

## 2. 消息发送时序

```
用户/Controller
    │
    ▼ POST /api/order/send
MessageController
    │
    ▼ send(OrderMessage)
OrderProducerService
    │
    ▼ objectMapper.writeValueAsString(message)
    │
    ▼ MapRecord.create(key, {"payload": json})
    │
    ▼ stringRedisTemplate.opsForStream().add(record)
Redis Server ──XADD──▶ order:stream
```

## 3. 消息消费时序

```
Redis Server
    │
    ▼ XREADGROUP GROUP order:group:inventory consumer-inventory STREAMS order:stream >
StreamMessageListenerContainer
    │
    ▼ onMessage(MapRecord)
AbstractStreamConsumer
    │
    ├─▶ 解析 payload 为 OrderMessage
    ├─▶ 幂等检查（SETNX order:processed:{orderId}）
    ├─▶ handleBusiness(orderMessage)
    │       │
    │       ▼ 扣库存 / 发短信
    │       ▼ 成功 / 抛异常
    │
    ├─▶ 成功：ack(id) 即 XACK
    └─▶ 失败：抛 RuntimeException，不 ACK，消息进入 pending
```

## 4. Pending 重试时序

```
PendingMessageProcessor (定时 @Scheduled)
    │
    ▼ XPENDING order:stream order:group:inventory - + 100
Redis Server
    │
    ▼ 返回 pending 消息列表
PendingMessageProcessor
    │
    ├─▶ 检查 idle 时间 > claimIdleMs？
    │       │
    │       ▼ 否：跳过
    │       ▼ 是：继续
    │
    ├─▶ 检查 deliveryCount > maxRetries？
    │       │
    │       ▼ 是：moveToDlq() → XADD DLQ + XACK
    │       ▼ 否：继续
    │
    ▼ XCLAIM order:stream order:group:inventory consumer-inventory minIdle id
Redis Server
    │
    ▼ 转移消息所有权
PendingMessageProcessor
    │
    ▼ 调用对应 consumer.onMessage(record) 重新处理
    │
    ▼ 成功：ACK；失败：继续 pending，等待下次巡检
```

## 5. 关键设计决策

### 5.1 为什么一个订单需要两个消费者组？

- **库存扣减**和**短信通知**是两个独立的业务。
- 使用两个消费者组，可以让两个业务各自独立消费、独立重试、互不影响。
- 如果只用一组，两个业务强耦合，一个失败会影响另一个。

### 5.2 为什么 pending 处理器里要再调 consumer.onMessage？

- `XCLAIM` 只是把消息所有权转移过来，并不会自动触发业务处理。
- 因此 claim 成功后，需要手动调用消费者的 `onMessage` 重新走完整流程：幂等、业务、ACK。
- 实际生产中可以改为把消息投递到本地队列，由线程池异步处理。

### 5.3 幂等键为什么要用 orderId 而不是 messageId？

- 同一条业务消息可能因为重试、网络抖动被投递多次，但业务上它是同一个订单。
- 用 `orderId` 作为幂等键，可以保证“同一个订单只被处理一次”。
- 用 `messageId` 只能保证同一条 stream entry 不重复处理，无法防止业务重复。

### 5.4 为什么用 MapRecord 存 JSON 字符串？

- 简化 RedisTemplate 序列化配置，避免 ObjectRecord 在不同版本 Spring Data Redis 中的兼容性问题。
- 便于人类阅读：用 `XRANGE` 查出的内容直接就是 JSON 字符串。
- 同时也演示了 MapRecord 的使用方式。

### 5.5 前后端协作的实时观测设计

- **数据同步决策**：前端未采用复杂的 WebSocket 连接，而是通过 `3秒定时短轮询`（Vue Composition API 结合 `setInterval`）方式获取系统状态。这种方案实现成本极低，天然具备幂等性与容错性，适合学习与演示项目。
- **实时日志同步**：后端系统在各个业务阶段（消费成功、失败模拟、`XCLAIM` 认领、转入 DLQ 等）将日志格式化为 JSON 并通过 `RPUSH` 写入 Redis 的 `order:ui:logs` 列表。前端轮询时仅需要 `LRANGE 0 49` 读取并解析，既保证了时效性，也做到了前后端完全解耦。
- **状态交集推算**：前端没有在数据库中存储每条消息的生命周期状态，而是通过读取 `order:stream` 里的最新消息列表，分别与当前的 `Pending` 消息列表、`DLQ` 消息列表计算交集，从而在前端动态且高能效地推断出某条消息到底是“已消费 (ACK)”、“重试中 (Pending)”还是“死信 (DLQ)”。
- **运行期参数热更新**：后端支持通过 `POST /api/order/config` 在线更新 `streamProperties` 中的失败率和模拟配置，使前端的故障开关调节能够立即生效，极大简化了传统教学中修改代码/配置文件并重启的冗长链路。

## 6. 扩展方向

- 引入 **Micrometer + Prometheus** 监控指标（pending 数量、DLQ 数量、消费延迟）。
- 使用 **多个应用实例** 验证消费者组负载均衡。
- 实现 **手动 ACK / NACK / 跳过** 的管理后台。
- 把 DLQ 消息通过 **补偿任务** 重新处理或告警。
