# Redis List 数据结构经典应用场景实战更新说明

## 1. 更新概述
本次更新在项目中进一步完善了 Redis **List（列表）** 数据结构的实战场景。除了基础的**轻量级消息队列**和**最新列表（时间线 Feed 流）**外，补充了更具高级应用价值的**可靠消息队列（安全消费）**和**栈（LIFO）**场景。

## 2. 功能模块全景说明

### 2.1 业务逻辑层 (Service)
**文件路径**：`src/main/java/com/example/redisstream/service/RedisListUseCasesService.java`
该服务类基于 `StringRedisTemplate.opsForList()` 实现了以下四大核心场景：

1. **基础消息队列 (FIFO)**：利用 `LPUSH` (左压) 和 `BRPOP` / `RPOP` (右弹) 构成的经典队列模式。
2. **最新列表 / 时间线**：利用 `LPUSH` + `LTRIM` (截断) 组合，实现在高并发下也能保持有限长度的动态 Feed 流。
3. **可靠消息队列 (Safe Queue)**：
   - **解决痛点**：普通 `RPOP` 一旦把消息取走，如果此时消费者立刻崩溃（例如断电或进程强杀），消息就永远丢失了，因为业务还没来得及处理它，而 Redis 中也早没了它的身影。
   - **实现方案**：通过 Redis 提供的一个原子指令 **`RPOPLPUSH`**（或 Redis 6.2 之后的 `LMOVE` 指令）。它可以原子性地将消息从“主队列”弹出的瞬间，立刻塞入一个“处理中队列（Processing Queue）”中。
   - **确认机制（ACK）**：当消费者把业务跑完、入库成功后，再调用 `LREM` 手动将该条消息从“处理中队列”剔除，实现 At-Least-Once (至少一次) 投递保证。如果业务崩溃未确认，定时任务还可以扫描处理中队列将死信重新丢回主队列。
4. **栈 (LIFO 后进先出)**：
   - **实现方案**：数据的输入和输出全在同一侧操作。使用 `LPUSH` 和 `LPOP`。
   - **应用场景**：浏览器历史记录后退回退、软件的撤销操作记录、或者最近搜索记录展示等。

### 2.2 API 接口层 (Controller)
**文件路径**：`src/main/java/com/example/redisstream/controller/RedisListUseCasesController.java`
暴露的所有测试 API 如下：

**1. 消息队列测试**
- `POST /api/redis-usecases/list/queue/{queueName}/send`
- `GET /api/redis-usecases/list/queue/{queueName}/receive`

**2. 最新列表测试**
- `POST /api/redis-usecases/list/timeline/{timelineKey}/add`
- `GET /api/redis-usecases/list/timeline/{timelineKey}`

**3. 可靠队列测试 (高级新增)**
- **安全接收并转入处理中**：`GET /api/redis-usecases/list/queue/reliable/{queueName}/receive`
- **业务处理成功后手动 ACK 确认**：`DELETE /api/redis-usecases/list/queue/reliable/{queueName}/ack?message=xxx`
*(你可以尝试：向主队列发送消息后调用 reliable receive 接口，随后去 Redis 客户端看，消息是不是安全地躺在以 `_processing` 结尾的备份队列里，直到你调用 ack 删除它。这就是可靠机制的本质。)*

**4. 栈模型测试 (新增)**
- **压栈操作**：`POST /api/redis-usecases/list/stack/{stackName}/push?item=页面A` (连压几次 页面A、页面B)
- **弹栈（撤销）操作**：`GET /api/redis-usecases/list/stack/{stackName}/pop` (由于 LIFO 特性，会发现最后压入的页面B先被弹出来)

## 3. 运行与验证
现在项目中已经全面覆盖了 List 双向链表结构的精华特性。通过调整入队和出队的两端（左或右），List 被奇妙地赋予了定长流、FIFO 队列、LIFO 栈，甚至是安全转移数据的不同业务语义。
