# Redis Stream 消息队列学习项目（Java 8 + Spring Boot 2.7）

> 一个**完整、可运行、带详细中文注释**的 Redis Stream 消息队列学习项目。
> 通过模拟“电商订单 -> 扣库存 + 发短信”的真实业务场景，覆盖 Redis Stream 的核心知识点。
> 
> **🆕 最新更新**：为了打造更全面的 Redis 实战学习库，项目中现已额外补充了 Redis **五大经典数据结构（String, Hash, List, Set, ZSet）**在真实业务场景下的完整实战代码。详见下文对应章节及 `docs/` 目录。

## 目录

- [技术栈](#技术栈)
- [前置条件](#前置条件)
- [快速启动](#快速启动)
- [项目结构](#项目结构)
- [核心知识点索引](#核心知识点索引)
- [业务场景说明](#业务场景说明)
- [REST API 列表](#rest-api-列表)
- [五大数据结构经典应用场景](#五大数据结构经典应用场景)
- [手动测试示例](#手动测试示例)
- [观察重试与死信队列](#观察重试与死信队列)
- [常见问题](#常见问题)
- [学习建议](#学习建议)

## 技术栈

### 后端技术栈
- Java 8
- Spring Boot 2.7.18
- Spring Data Redis 2.7.x（Lettuce 驱动）
- Maven 3.6+
- Redis 5.0+（本地安装，默认 `localhost:6379`）

### 前端技术栈
- Vue 3 (Composition API)
- Vite (构建与本地开发服务，配置开发跨域代理)
- Element Plus (UI 组件库，定制科技风深色主题)
- Axios (异步请求，与后端 REST API 交互)
- CSS (原生 CSS，支持脉冲动画与流转高亮效果)

## 前置条件

1. 安装 JDK 8 并配置 `JAVA_HOME`。
2. 安装 Maven。
3. 安装 Node.js (推荐 16+) 和 npm，用于启动前端监控台。
4. 安装并启动 Redis：
   ```bash
   redis-server
   ```
   确认 Redis 可访问：
   ```bash
   redis-cli ping
   # 返回 PONG 表示正常
   ```

## 快速启动

本项目为前后端分离架构，需要分别启动后端服务和前端监控台。

### 1. 启动后端服务 (Spring Boot)

```bash
# 运行后端
mvn clean spring-boot:run
```

启动成功后，控制台会打印消费者注册日志：

```
注册消费者：group=order:group:inventory, consumer=consumer-inventory
注册消费者：group=order:group:sms, consumer=consumer-sms
```

后端默认运行在 `http://localhost:8080`。

### 2. 启动前端监控台 (Vue 3 + Vite)

```bash
# 1. 进入前端目录
cd web

# 2. 安装依赖
npm install

# 3. 启动开发服务器
npm run dev
```

启动成功后，在浏览器访问控制台输出的地址（通常是 `http://localhost:5173`）。
*提示：前端已配置开发跨域代理，会自动将 `/api` 请求转发到后端的 `http://localhost:8080`。*

## 项目结构

```
├── pom.xml
├── README.md
├── docs/                                # 核心设计与说明文档
│   ├── 01-redis-stream-theory.md        # Stream 理论知识
│   ├── 02-architecture.md               # 架构与时序图
│   └── 03-api-and-test.md               # API 与测试说明
├── src/                                 # 后端 Java 源码
│   ├── main/java/com/example/redisstream/
│   │   ├── config/RedisConfig.java              # Redis、监听容器配置
│   │   ├── constants/StreamConstants.java       # 常量
│   │   ├── controller/MessageController.java    # REST 调试与监控接口
│   │   ├── dto/                                 # 数据传输对象
│   │   ├── exception/GlobalExceptionHandler.java
│   │   ├── service/producer/OrderProducerService.java
│   │   ├── service/consumer/AbstractStreamConsumer.java
│   │   ├── service/consumer/InventoryConsumer.java
│   │   ├── service/consumer/SmsConsumer.java
│   │   ├── support/RedisStreamOperator.java
│   │   ├── support/PendingMessageProcessor.java # pending 巡检/重试/DLQ
│   │   ├── support/IdempotentService.java       # 业务幂等
│   │   └── task/StreamMonitorTask.java          # 定时监控
│   └── main/resources/application.yml
└── web/                                 # 前端监控台 Web 项目
    ├── index.html                       # 前端入口 HTML
    ├── package.json                     # 依赖与脚本
    ├── vite.config.js                   # Vite 配置与跨域代理
    └── src/
        ├── App.vue                      # 根组件，页面整体科技风栅格布局与故障模拟控制器
        ├── main.js                      # 前端入口 JavaScript
        ├── style.css                    # 全局样式与自定义 Element Plus 黑色科技皮肤
        ├── api/
        │   └── order.js                 # Axios API 封装，与后端进行通信
        ├── components/                  # 前端核心可视化组件
        │   ├── SendPanel.vue            # 消息生产控制面板 (单条/批量)
        │   ├── FlowVisualization.vue    # 消息流转链路拓扑图 (含脉冲高亮动画)
        │   ├── StatsCards.vue           # 全局数据看板 (总长度/各组 Pending 数/DLQ 数)
        │   ├── StreamEntries.vue        # 实时消息流查看器 (支持状态推算与展开行 JSON)
        │   ├── PendingTable.vue         # Pending 队列与多消费组管理
        │   ├── DlqTable.vue             # DLQ 死信队列查看列表
        │   └── EventLog.vue             # 实时业务动作事件日志列表
        └── composables/
            └── useStreamDashboard.js    # 全局数据 3 秒轮询与配置同步逻辑
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
| GET  | `/api/order/dlq` | 查看死信队列最近 50 条消息 |
| GET  | `/api/order/stats` | 查看 stream 统计信息 |
| GET  | `/api/order/logs` | 获取供前端展示的实时事件日志 |
| POST | `/api/order/logs/clear` | 清空前端实时事件日志 |
| GET  | `/api/order/recent` | 获取最近生产的 50 条 Stream 消息 |
| GET  | `/api/order/config` | 获取当前系统配置参数（模拟、故障率、重试、巡检间隔、Claim 空闲时间） |
| POST | `/api/order/config` | 动态更新系统配置参数（模拟、故障率、最大重试） |

## 五大数据结构经典应用场景

除了核心的 Stream 消息队列，本项目在 `com.example.redisstream.service` 和 `controller` 包中，额外为你准备了 Redis 五大基础数据结构的经典业务场景代码演示：

| 数据结构 | 特性/场景 | 核心实战功能演示 | 对应说明文档 |
|----------|-----------|------------------|----------|
| **String** (字符串) | 基础 KV，原子计数 | 1. **缓存 (Cache)**<br>2. **计数器 (文章阅读量)**<br>3. **Session 集中管理**<br>4. **API 限流 (Rate Limiting)** | [`redis_string_use_cases_update.md`](docs/redis_string_use_cases_update.md) |
| **Hash** (哈希) | 对象属性高效管理 | 1. **对象结构化缓存** (局部更新如积分)<br>2. **购物车** (数量增减、移除) | [`redis_hash_use_cases_update.md`](docs/redis_hash_use_cases_update.md) |
| **List** (列表) | 双向链表，队列/栈 | 1. **简单消息队列** (FIFO)<br>2. **最新列表/时间线** (定长截断)<br>3. **可靠消息队列** (RPOPLPUSH防丢)<br>4. **栈** (LIFO撤销操作) | [`redis_list_use_cases_update.md`](docs/redis_list_use_cases_update.md) |
| **Set** (无序集合) | 唯一性，集合运算 | 1. **独立访客 UV 统计**<br>2. **公平抽奖系统**<br>3. **社交网络** (共同关注、交集计算) | [`redis_set_use_cases_update.md`](docs/redis_set_use_cases_update.md) |
| **ZSet** (有序集合) | 绑定分数自动排序 | 1. **排行榜 (Leaderboard)**<br>2. **精准延时队列** (基于时间戳 Score 轮询) | [`redis_zset_use_cases_update.md`](docs/redis_zset_use_cases_update.md) |
| **Bitmap** (位图) | 按位操作极省内存 | 1. **海量用户签到** (SETBIT/BITCOUNT) | [`redis_advanced_use_cases_update.md`](docs/redis_advanced_use_cases_update.md) |
| **HyperLogLog** | 极低内存概率统计 | 1. **亿级 UV 去重统计** (PFADD/PFCOUNT 固定占用 12KB) | [`redis_advanced_use_cases_update.md`](docs/redis_advanced_use_cases_update.md) |

*(提示：以上所有的实战场景均已暴露出完整的 REST API 测试接口，你可以直接启动项目后使用 Postman 进行测试和体验。)*

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

本项目提供了**两种**方式来观察和验证 Redis Stream 的重试与死信流转：

### 方式 A：通过前端可视化监控界面（推荐）
1. 访问前端页面 `http://localhost:5173`。
2. 在左侧的**“异常消费与死信模拟器”**面板中，打开“开启模拟消费失败”开关，可以自由拉动调节“故障发生率”（例如 50%）以及“进入死信前最大重试”次数。
3. 在顶部的**“消息发布台”**中，点击“发送测试消息”或“批量发送消息”。
4. 在右侧的**“消息流转拓扑”**中可以实时看到消息产生时的脉冲闪烁，以及消费者组红色的故障报错信号。
5. 在左下角的**“系统事件日志”**和下方的**“Pending 队列与消费组管理”**中，可以观察到：
   - 消息处理失败进入 Pending。
   - `PendingMessageProcessor` 巡检认领消息（`XCLAIM`）并触发重新处理。
   - 当重试次数超过上限后，事件日志将打印“移入死信队列”，并在“DLQ 死信队列查看”表格中出现该死信消息。

### 方式 B：通过配置文件与命令行
1. 修改后端项目中的 `application.yml`：
   ```yaml
   app:
     stream:
       simulate-failure: true
       failure-rate: 0.5
       max-retries: 2
   ```
2. 重新启动后端服务，并使用 `curl` 发送消息：
   ```bash
   curl -X POST http://localhost:8080/api/order/send/demo
   ```
3. 观察控制台日志：
   - 消费者打印“业务处理失败，消息进入 pending”。
   - `PendingMessageProcessor` 定时 claim 并重试。
   - 超过 `max-retries` 后，消息被写入 DLQ，并 ACK 移除 pending。
4. 访问 `/api/order/dlq` 可看到死信消息：
   ```bash
   curl http://localhost:8080/api/order/dlq
   ```

## 常见问题

**Q: 启动前端时提示端口冲突或无法与后端通信？**

A: 请确保运行在 `http://localhost:8080` 的后端服务已完全启动。Vite 在开发阶段配置了反向代理，若后端运行端口不是 `8080`，需要修改 `web/vite.config.js` 中的代理目标（`target`）。

**Q: 启动时报 `NOGROUP No such key 'order:stream' or consumer group ...`？**

A: 确认 `RedisConfig.initConsumerGroups()` 已执行。如果 Redis 中 stream 不存在，createGroup 会自动创建空 stream。若仍报错，可能是配置中 group 名称不一致，或者 Redis 服务未正常启动。

**Q: 消息一直不消费？**

A: 检查 `app.stream.simulate-failure` 是否开启且异常未被正确抛出。如果 `AbstractStreamConsumer` catch 了异常但没抛出，消息会被 ACK，不会进入 pending。同时可以观察前端拓扑图上的脉冲，或使用 `redis-cli` 工具执行 `XINFO STREAM order:stream` 命令排查。

**Q: 为什么不用 ObjectRecord 而用 MapRecord？**

A: 为了降低学习门槛并避免序列化配置带来的兼容性问题。本项目把 JSON 作为字符串存在 `payload` 字段中，MapRecord 更直观。文档中也说明了 ObjectRecord 的用法。

**Q: 怎么清空测试数据？**

A: 连接 redis-cli 执行以下命令：

```bash
DEL order:stream order:stream:dlq order:group:inventory order:group:sms order:ui:logs order:processed:*
```
*(注意：这里增加了 `order:ui:logs` 实时事件日志，以及 `order:processed:*` 业务幂等键的清理。)*

## 学习建议

1. **一键跑通**：先启动后端和前端，在浏览器打开前端控制台，发送一条 demo 消息，观察流转动画和实时事件日志。
2. **可视化故障模拟**：通过前端“异常消费与死信模拟器”面板，动态开启故障模拟，调节故障率与最大重试次数，感受 `XCLAIM` 的异步认领和死信队列转移过程，这是项目最核心的闭环。
3. **结合命令行探究**：在前端操作的同时，配合使用 `redis-cli` 运行 `XINFO STREAM order:stream`、`XPENDING order:stream order:group:inventory`，在底层数据变化与前端 UI 的印证中加深理解。
4. **阅读核心文档**：阅读 `docs/` 目录下的三篇文档，分别从理论背景、系统架构设计、API 接口和测试几个维度系统梳理知识。

---

祝学习愉快！如有问题，欢迎在代码注释中继续探索。
