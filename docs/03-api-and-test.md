# 03 - API 列表与测试说明

## 1. REST API 详情

### 1.1 发送单条消息

```bash
POST /api/order/send
Content-Type: application/json
```

请求体：

```json
{
  "orderId": "O202406210001",
  "userId": "U10086",
  "sku": "SKU-001",
  "quantity": 2,
  "amount": 199.99
}
```

响应：

```json
{
  "code": 200,
  "message": "发送成功",
  "data": "1719999999999-0"
}
```

### 1.2 批量发送

```bash
POST /api/order/send/batch
Content-Type: application/json
```

请求体：

```json
{
  "message": {
    "orderId": "O202406210002",
    "userId": "U10086",
    "sku": "SKU-002",
    "quantity": 1,
    "amount": 99.99
  },
  "count": 10
}
```

说明：每次发送都会生成新的 orderId，避免幂等导致只处理一次。

### 1.3 发送测试消息

```bash
curl -X POST http://localhost:8080/api/order/send/demo
```

### 1.4 查看 pending 消息

```bash
curl "http://localhost:8080/api/order/pending?group=order:group:inventory"
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "1719999999999-0",
      "consumer": "consumer-inventory",
      "idleTime": 35000,
      "deliveryCount": 2
    }
  ]
}
```

### 1.5 查看死信队列

```bash
curl http://localhost:8080/api/order/dlq
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "1720000000000-0",
      "originalId": "1719999999999-0",
      "payload": "{...}",
      "retryCount": "4",
      "reason": "超过最大重试次数 3"
    }
  ]
}
```

### 1.6 查看统计信息

```bash
curl http://localhost:8080/api/order/stats
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "streamKey": "order:stream",
    "length": 15,
    "pendingCounts": {
      "order:group:inventory": 0,
      "order:group:sms": 0
    },
    "consumers": {
      "order:group:inventory": ["consumer-inventory"],
      "order:group:sms": ["consumer-sms"]
    },
    "dlqLength": 1
  }
}
```

## 2. 单元测试与集成测试

### 2.1 运行所有测试

```bash
mvn test
```

### 2.2 测试说明

| 测试类 | 类型 | 说明 |
|--------|------|------|
| `RedisStreamDemoApplicationTests` | 集成测试 | 验证 Spring Boot 上下文能正常加载 |
| `OrderProducerServiceTest` | 集成测试 | 发送消息后验证 stream 长度 +1 |
| `StreamConsumerTest` | 集成测试 | 发送消息后等待消费，验证各组 pending 归零 |

### 2.3 测试前注意

- 确保本地 Redis 已启动。
- 测试会往 `order:stream` 写真实数据，测试完成后不会自动清理。
- 如需清理，可执行：

```bash
redis-cli DEL order:stream order:stream:dlq
redis-cli DEL order:group:inventory order:group:sms
```

## 3. 手动压测

批量发送 100 条消息，观察消费情况：

```bash
# 批量发送
curl -X POST http://localhost:8080/api/order/send/batch \
  -H "Content-Type: application/json" \
  -d '{
    "message": {
      "orderId": "O-BULK-001",
      "userId": "U999",
      "sku": "SKU-999",
      "quantity": 1,
      "amount": 9.99
    },
    "count": 100
  }'

# 观察统计
curl http://localhost:8080/api/order/stats
```

## 4. redis-cli 调试命令

```bash
# 查看 stream 基本信息
XINFO STREAM order:stream

# 查看最近 10 条消息
XRANGE order:stream - + COUNT 10

# 查看消费者组
XINFO GROUPS order:stream

# 查看库存组的 pending 摘要
XPENDING order:stream order:group:inventory

# 查看库存组的 pending 详情（最多 10 条）
XPENDING order:stream order:group:inventory - + 10

# 查看 DLQ
XRANGE order:stream:dlq - + COUNT 10

# 手动 ack 一条消息
XACK order:stream order:group:inventory 1719999999999-0

# 手动 claim 一条消息
XCLAIM order:stream order:group:inventory consumer-inventory 30000 1719999999999-0

# 清空所有数据（慎用）
DEL order:stream order:stream:dlq order:group:inventory order:group:sms
```

## 5. 推荐学习路径

1. 阅读 `docs/01-redis-stream-theory.md` 建立理论概念。
2. 启动项目，发送 demo 消息，查看日志和 redis-cli。
3. 开启 `simulate-failure`，发送消息，观察 pending 和 DLQ。
4. 用 `XPENDING`、`XCLAIM`、`XRANGE` 命令对照代码理解流程。
5. 阅读 `docs/02-architecture.md` 理解架构设计。
6. 尝试扩展：增加一个消费者组、增加 Micrometer 监控、引入 Testcontainers。
