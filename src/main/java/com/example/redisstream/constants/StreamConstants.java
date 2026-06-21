package com.example.redisstream.constants;

/**
 * 集中管理 Redis Stream 相关的常量。
 *
 * 说明：
 * - 真实项目中，Stream key / group 建议通过配置中心下发，这里用常量+配置组合。
 * - 使用常量可以避免代码中散落字符串，便于维护和查找。
 */
public final class StreamConstants {

    /** 私有构造方法，防止实例化 */
    private StreamConstants() {
    }

    /** Stream 中存放 JSON 字符串的字段名 */
    public static final String FIELD_PAYLOAD = "payload";

    /** DLQ 中记录原消息 ID 的字段名 */
    public static final String FIELD_ORIGINAL_ID = "originalId";

    /** DLQ 中记录失败原因的字段名 */
    public static final String FIELD_REASON = "reason";

    /** DLQ 中记录重试次数的字段名 */
    public static final String FIELD_RETRY_COUNT = "retryCount";

    /** Redis hash key 前缀：记录某个业务 ID 已被处理（业务级幂等） */
    public static final String IDEMPOTENT_PREFIX = "order:processed:";

    /** Redis hash key 前缀：记录某个消费者组对某条消息的重试次数 */
    public static final String RETRY_PREFIX = "order:retry:";
}
