# Redis ZSet (有序集合) 数据结构经典应用场景实战更新说明

## 1. 更新概述
本次引入了 **ZSet（Sorted Set 有序集合）** 的实战代码，这是 Redis 中最为独特且强大的高级数据结构。它为集合中的每个元素都强制关联了一个 `Double` 类型的 `Score（分数）`，并在底层通过“跳跃表 (SkipList) + 哈希表”的数据结构组合，实现了基于分数的高效自动排序。

## 2. 核心场景说明
1. **排行榜 (Leaderboard)**：这是 ZSet 绝对的统治级场景。用户的 ID 作为 Value，积分作为 Score。使用 `ZINCRBY` 极速为用户加减分，系统自动重新排序；使用 `ZREVRANGE` 轻松获取全服前 N 名的高分列表。它完美胜任游戏积分榜、微博热搜热度榜、直播间礼物打赏榜。
2. **精准延时队列 (Delayed Queue)**：一种极为巧妙的用法。将具体的任务ID存为 Value，而**将任务预计要执行的【未来时间戳】存为 Score**。后台开启一个线程，不断使用 `ZRANGEBYSCORE` 拉取 Score 小于等于当前时间戳的任务（即已经到期的任务），拉取后立刻执行 `ZREM` 将任务删除防止重复执行。这非常适合实现诸如“订单创建30分钟未支付自动取消”的业务。

## 3. 测试接口指南 (入口路径 `/api/redis-usecases/zset`)
- **加分与排行榜**：
  - `POST /leaderboard/{boardName}/addScore?userId=xxx&score=100` 
  - `GET /leaderboard/{boardName}/top?top=10`
- **延时任务投递与消费**：
  - `POST /delayedQueue/{queueName}/send?taskId=task123&delaySeconds=30` 
  - `GET /delayedQueue/{queueName}/fetch` 
  *(测试技巧：调用发任务接口并指定延迟 30 秒，在这 30 秒内不断调用 fetch 接口，你会发现始终拉不到数据；等 30 秒倒计时结束，再次 fetch，到期的任务瞬间就会被拉取出来。这就是基于时间戳 Score 的排序魅力！)*
