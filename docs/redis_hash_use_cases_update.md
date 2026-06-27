# Redis Hash 数据结构经典应用场景实战更新说明

## 1. 更新概述
本次更新在项目中引入了 Redis **Hash（哈希）** 数据结构的经典应用场景实战代码。重点演示了 Hash 在**存储对象数据**以及构建**购物车**等场景下，相比普通 String 数据结构在部分更新效率上的显著优势。

## 2. 新增文件及功能模块说明

### 2.1 业务逻辑层 (Service)
**文件路径**：`src/main/java/com/example/redisstream/service/RedisHashUseCasesService.java`
该服务类基于 `StringRedisTemplate.opsForHash()` 实现了以下两个核心场景：
1. **对象结构化缓存**：将用户对象的各个属性打散存储在 Hash 的 Field-Value 中。相比将整个 JSON 字符串存入 String，Hash 最大的优势是可以直接利用 `HINCRBY` 等指令在服务端**原位修改特定的字段**（比如单单增加用户的积分 `points`）。这避免了将整个大对象拉回应用服务反序列化、修改后再序列化写回的过程，大幅降低网络开销及并发冲突可能。
2. **购物车系统**：把当前用户 ID 作为 Hash 的大 Key，每件商品的 ID 作为 Field，购买的数量作为 Value。
    - **加车/修改数量**：通过 `HINCRBY` 给商品增减数量（不存在则自动创建为指定的数量）。
    - **删除商品**：使用 `HDEL` 移除单个 Field。
    - **获取购物车清单**：使用 `HGETALL` 获取所有商品及其数量。

### 2.2 API 接口层 (Controller)
**文件路径**：`src/main/java/com/example/redisstream/controller/RedisHashUseCasesController.java`
对外暴露了一组方便测试的 RESTful 接口：

**对象缓存测试：**
- **保存完整对象**：`POST /api/redis-usecases/hash/user/{userId}` (Body 中传入 JSON 字典，如 `{"name":"张三", "points":"100"}`)
- **局部修改某字段（例如增加积分）**：`POST /api/redis-usecases/hash/user/{userId}/points?delta=50` 
- **获取完整对象**：`GET /api/redis-usecases/hash/user/{userId}`

**购物车测试：**
- **加入购物车 / 增加商品数量**：`POST /api/redis-usecases/hash/cart/{userId}/add?productId=p1001&quantity=2` (再次调用同接口，数量会累加)
- **从购物车中移除商品**：`DELETE /api/redis-usecases/hash/cart/{userId}/remove?productId=p1001`
- **查看用户的购物车全貌**：`GET /api/redis-usecases/hash/cart/{userId}`

## 3. 运行与验证
项目运行后，使用 API 调试工具调用上述接口即可。建议重点测试“局部修改某字段”以及“购物车增加商品数量”接口，去观察 Redis 服务端的轻量化操作。
