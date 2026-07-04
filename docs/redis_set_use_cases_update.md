# Redis Set 数据结构经典应用场景实战更新说明

## 1. 更新概述
本次引入了 **Set（无序集合）** 的实战代码，主要利用其**元素天然唯一性**以及强大的**集合数学运算（交、并、差集）**特性。

## 2. 核心场景说明
1. **去重统计 (如页面 UV)**：利用 `SADD` 命令。不论同一个用户点进该页面多少次，Set 里只会存一份他的 ID。最终通过 `SCARD` 即可极其快速地获取独立访客数量。
2. **公平抽奖系统**：用户报名抽奖时 ID 入 Set，防止了利用同一账号刷单重复报名。抽奖时利用 `SRANDMEMBER` (随机返回但保留，可反复玩) 或 `SPOP` (随机弹出并剔除数据，保证一人只能中一次) 实现真正的高性能随机抽取。
3. **社交关系网络**：基于集合运算。比如利用 `SINTER`（交集）一步算出两个用户的**共同关注好友**，利用 `SDIFF`（差集）计算出 **“可能认识的人 / 你没有关注的人”**。

## 3. 测试接口指南 (入口路径 `/api/redis-usecases/set`)
- **UV 访问与统计**：
  - `POST /uv/{pageId}/visit?userId=xxx`
  - `GET /uv/{pageId}/count`
- **参与抽奖与开奖**：
  - `POST /lottery/{activityId}/join?userId=xxx` 
  - `GET /lottery/{activityId}/draw?count=3`
- **关注与求共同关注**：
  - `POST /social/{userId}/follow?targetUserId=xxx` 
  - `GET /social/common?userId1=A&userId2=B`
