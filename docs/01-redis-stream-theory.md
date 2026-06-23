# 01 - Redis Stream 理论知识

## 1. 什么是 Redis Stream？

Redis Stream 是 Redis 5.0 引入的一种新的数据结构，专门用于实现**日志型消息队列**。

它兼具以下特点：

- **持久化**：消息会保存在 Redis 中，直到被显式删除或截断。
- **有序**：每条消息都有一个全局唯一的 ID，按时间顺序递增。
- **支持消费者组（Consumer Group）**：多个消费者可以组成一个组，组内消息只会被其中一个消费者处理。
- **ACK 机制**：消费者处理完消息后需要发送 ACK，否则消息会进入 pending list。
- **支持消息追踪**：可以查看哪些消息被谁消费、是否已确认、空闲多久。

## 2. Stream 的数据结构

```
stream key: order:stream

| entry ID          | fields                        |
|-------------------|-------------------------------|
| 1719999999999-0   | payload={...JSON...}          |
| 1719999999999-1   | payload={...JSON...}          |
| 1720000000000-0   | payload={...JSON...}          |
```

- **entry ID**：`<毫秒时间戳>-<序列号>`，全局递增。
- **fields**：键值对，可以存多个字段。本项目中只用一个 `payload` 字段存放 JSON。

## 3. 核心命令速查

| 命令 | 作用 | 本项目对应 |
|------|------|------------|
| `XADD key * field value` | 向 stream 添加消息 | `OrderProducerService.send()` |
| `XREADGROUP GROUP g c STREAMS key >` | 消费者组读取新消息 | `StreamMessageListenerContainer.receive()` |
| `XACK key group id` | 确认消息已处理 | `AbstractStreamConsumer.ack()` |
| `XPENDING key group` | 查看 pending 消息摘要 | `RedisStreamOperator.pendingSummary()` |
| `XPENDING key group start end count` | 查看 pending 消息详情 | `RedisStreamOperator.pending()` |
| `XCLAIM key group newOwner minIdle id ...` | 把 idle 消息转移给其他消费者 | `PendingMessageProcessor` |
| `XLEN key` | 查看 stream 长度 | `RedisStreamOperator.len()` |
| `XTRIM key MAXLEN count` | 截断 stream | `RedisStreamOperator.trimStream()` |
| `XRANGE key start end [COUNT n]` | 按 ID 范围查询消息 | `RedisStreamOperator.range()` |
| `XINFO GROUPS key` | 查看消费者组信息 | redis-cli 调试 |
| `XINFO CONSUMERS key group` | 查看消费者信息 | redis-cli 调试 |

## 4. 消费者组模型

```
┌─────────────────┐
│   order:stream  │
└────────┬────────┘
         │
    ┌────┴────┐
    ▼         ▼
 group A   group B
    │         │
 ┌──┴──┐   ┌──┴──┐
 c1 c2  │   c3 c4 │
```

- 一条消息可以被多个消费者组同时消费（广播给不同业务）。
- 同一消费者组内，一条消息只会被一个消费者处理（负载均衡）。
- 每个消费者组维护自己的消费进度（last delivered ID）。

## 5. Pending List 与 ACK

当消费者通过 `XREADGROUP` 读到消息后，消息会被“分配”给该消费者，同时进入 pending list。

- **ACK 后**：消息从 pending list 移除，表示已正确处理。
- **未 ACK**：消息一直留在 pending list，可以通过 `XPENDING` 查看。

为什么需要 ACK？

> 保证消息至少被处理一次。如果消费者处理到一半崩溃，消息不会丢失，重启或迁移后可以继续处理。

## 6. 消息重试：XCLAIM

如果某条消息长时间未 ACK（idle 时间超过阈值），可以认为原消费者出现了问题。

`XCLAIM` 允许其他消费者“抢走”这条消息的所有权，重新处理。

流程：

1. 定时扫描 pending list。
2. 找出 idle 时间超过阈值的消息。
3. 调用 `XCLAIM` 转移所有权。
4. 重新执行业务逻辑。
5. 成功后 ACK，失败则继续留在 pending。

## 7. 死信队列（DLQ）

如果一条消息反复处理都失败，不能无限重试，否则会阻塞 pending list。

常见的处理方式是：

- 记录重试次数。
- 超过最大重试次数后，把消息写入另一个 stream（DLQ）。
- 对原消息执行 ACK，从 pending list 移除。
- 后续由人工或专门程序处理 DLQ 中的消息。

## 8. 幂等性

ACK 能保证消息不丢失，但**不能保证消息只被处理一次**。

例如：业务处理成功，但 ACK 之前应用崩溃，消息会被重新投递，导致业务重复执行。

因此需要**业务级幂等**：

- 使用唯一业务键（如 orderId）。
- 处理前检查是否已处理过（Redis SETNX）。
- 已处理过的消息直接 ACK，不再执行业务。

## 9. Stream 长度控制

Stream 不会自动删除消息，需要主动控制长度：

- 生产者端使用 `XADD key MAXLEN ~ count * ...` 限制长度。
- 或使用 `XTRIM key MAXLEN count` 定期截断。

本项目在每次 `XADD` 后调用 `XTRIM`，保留最近 `app.stream.max-len` 条消息。

## 10. Stream vs List vs Pub/Sub

| 特性 | List | Pub/Sub | Stream |
|------|------|---------|--------|
| 持久化 | 是 | 否 | 是 |
| 支持消费组 | 否 | 否 | 是 |
| ACK 机制 | 无 | 无 | 有 |
| 消息回溯 | 有限 | 无 | 有 |
| 适用场景 | 简单队列 | 实时广播 | 可靠消息队列 |

## 11. 参考命令

```bash
# 查看 stream 信息
XINFO STREAM order:stream

# 查看消费者组
XINFO GROUPS order:stream

# 查看某组 pending
XPENDING order:stream order:group:inventory

# 查看 pending 详情
XPENDING order:stream order:group:inventory - + 10

# 查看死信队列
XRANGE order:stream:dlq - + COUNT 10
```

## 12. 结合前端监控台进行可视化学习

为了帮助开发者和学习者将上述抽象命令与实际数据流转建立直观联系，本项目配备了 **Vue 3 科技风深色主题监控台**。在控制台界面中，你可以：
1. **直观观察流转拓扑**：直观看到 `Producer -> Stream -> Consumer Groups -> DLQ` 整个消息流转链路的拓扑动态。
2. **状态动态推算**：前端通过将 `order:stream` 消息流、各消费组的 `Pending` 消息与 `DLQ` 消息取交集，动态推算并展示出消息的实时流转状态（如 `已消费 (ACK)`、`重试中 (Pending)`、`死信 (DLQ)`）。
3. **配置热调优**：通过前端“异常消费与死信模拟器”，你可以在不停机的情况下实时调整丢包率和重试限制，并在界面上实时看到 `XCLAIM` 的异步认领和死信移入效果。
