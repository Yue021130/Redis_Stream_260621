# Redis Stream 监控台 (前端界面)

本项目是 Redis Stream 消息队列的配套前端可视化监控界面，旨在将不可见的消息流转过程转化为直观的、全透明的 UI 交互，帮助开发者和学习者更好地理解 Redis Stream 的工作原理（生产者、消费者组、ACK机制、Pending、XCLAIM、死信队列等）。

## 🛠️ 技术栈

- **框架**: Vue 3 (Composition API) + Vite
- **UI 组件库**: Element Plus
- **网络请求**: Axios
- **主题风格**: 极简科技风深色主题 (Dark Theme)

## ✨ 核心功能特性

1. **消息生产控制面板 (`SendPanel.vue`)**
   - 支持发送单条测试订单消息。
   - 支持并发批量发送（可配置数量），用于压测和观察多消费者组并发处理情况。

2. **消息流转可视化 (`FlowVisualization.vue`)**
   - 将 `Producer -> Stream -> 消费者组 -> DLQ` 的链路以拓扑图形式展示。
   - 具备脉冲高亮动画（Pulse Animation），在消息生产、消费瞬间实时闪烁，体现流转过程。

3. **Stream 全局数据看板 (`StatsCards.vue`)**
   - 实时监控 Stream 总长度、各消费组的 Pending 积压数、死信队列 (DLQ) 堆积数。

4. **实时消息流查看 (`StreamEntries.vue`)**
   - 展示最近进入 Stream 的 50 条消息列表。
   - 支持动态状态推算：`已消费 (ACK)` / `处理中待重试 (Pending)` / `死信 (DLQ)`。
   - 支持行展开，可直接查看原始的 JSON Payload 数据。

5. **Pending 队列与消费组管理 (`PendingTable.vue`)**
   - 展示因业务异常/处理超时未 ACK 的消息。
   - 支持多消费者组切换（如“库存组”、“短信组”），独立查看各组的积压状态。

6. **DLQ 死信队列查看 (`DlqTable.vue`)**
   - 展示超过最大重试次数（Max Retries）后被遗弃的消息，辅助业务排查严重故障。

7. **系统事件日志 (`EventLog.vue`)**
   - 实时拉取后端产生的业务动作日志（成功扣减、模拟失败报错、XCLAIM 抢占、移入死信队列等）。

## 📂 目录结构

```text
frontend/
├── public/                 # 静态资源
├── src/
│   ├── api/
│   │   └── order.js        # Axios API 封装，与后端进行通信
│   ├── assets/             # 全局样式与图片
│   ├── components/         # 核心 Vue 组件库 (按模块拆分)
│   ├── composables/
│   │   └── useStreamDashboard.js # 全局状态管理与 3 秒轮询核心逻辑
│   ├── App.vue             # 根组件，页面骨架
│   ├── main.js             # Vue 应用入口
│   └── style.css           # 全局 CSS 与深色主题变量、Element Plus 样式覆盖
├── index.html
├── package.json
└── vite.config.js          # Vite 构建与开发服务器配置（含代理跨域）
```

## 🚀 核心逻辑解析

### 1. 数据同步机制 (短轮询)
为了兼顾轻量化和实时性，前端在 `useStreamDashboard.js` 中采用了 **3秒短轮询** 的机制。
```javascript
const intervalId = setInterval(fetchAll, 3000);
```
每次轮询并发执行多个 API 请求，获取最新统计、Pending 列表、DLQ 列表、近期消息及事件日志，并将数据分发给各个下级组件。

### 2. 消息状态计算 (`StreamEntries.vue`)
消息的状态并非写死在数据库中，而是基于当前 Stream 中的消息与 Pending 列表、DLQ 列表的交集动态推算出的：
- 如果一条消息 ID 出现在 `dlqList` 中 $\rightarrow$ `死信 (DLQ)`
- 如果一条消息 ID 出现在 `pendingList` 中 $\rightarrow$ `处理中/失败待重试`
- 如果均未出现且已经进入过 Stream $\rightarrow$ `已消费 (ACK)`

### 3. 主题与样式覆盖 (`style.css`)
为了实现沉浸式的科技风体验，前端重写了 Element Plus 默认的亮色样式：
- 全局注入了深色渐变背景。
- 覆盖了 `.el-table` 的斑马纹 (`stripe`) 样式、悬浮样式 (`hover`) 以及展开行背景，防止文字在浅色背景下不可见。

## 📦 启动指引

1. **安装依赖**
   ```bash
   npm install
   ```

2. **运行开发服务器**
   ```bash
   npm run dev
   ```
   *注意： Vite 已配置代理，会自动将前端的 `/api` 请求转发到后端的 `http://localhost:8080` 以解决跨域问题。启动前端前，请确保 Spring Boot 后端服务已启动。*

3. **打包生产环境构建**
   ```bash
   npm run build
   ```
