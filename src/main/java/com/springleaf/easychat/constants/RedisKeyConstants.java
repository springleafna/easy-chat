package com.springleaf.easychat.constants;

/**
 * Redis Key 常量类
 * 统一管理所有 Redis Key 的生成规则
 */
public class RedisKeyConstants {

    /**
     * 未读消息数 Key 前缀
     * 格式：unread:{user_id}:{conversation_id}
     * 值类型：String (数字)
     * 过期时间：无（手动删除）
     */
    private static final String UNREAD_PREFIX = "unread:";

    /**
     * 活跃会话 Key 前缀
     * 格式：active_chat:{user_id}
     * 值类型：String (conversation_id)
     * 过期时间：60 秒
     */
    private static final String ACTIVE_CHAT_PREFIX = "active_chat:";

    /**
     * 活跃会话过期时间（秒）
     */
    public static final long ACTIVE_CHAT_EXPIRE_SECONDS = 60;

    /**
     * 生成未读消息数 Key
     *
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @return Redis Key
     */
    public static String getUnreadKey(Long userId, String conversationId) {
        return UNREAD_PREFIX + userId + ":" + conversationId;
    }

    /**
     * 生成活跃会话 Key
     *
     * @param userId 用户ID
     * @return Redis Key
     */
    public static String getActiveChatKey(Long userId) {
        return ACTIVE_CHAT_PREFIX + userId;
    }

    /**
     * 生成未读消息数 Key 的模式（用于批量查询）
     *
     * @param userId 用户ID
     * @return Redis Key 模式
     */
    public static String getUnreadKeyPattern(Long userId) {
        return UNREAD_PREFIX + userId + ":*";
    }
}
