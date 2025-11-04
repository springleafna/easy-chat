package com.springleaf.easychat.service;

import java.util.List;
import java.util.Map;

/**
 * 未读消息服务接口
 */
public interface UnreadService {

    /**
     * 设置用户的活跃会话
     *
     * @param userId 用户ID
     * @param conversationId 会话ID
     */
    void setActiveChat(Long userId, String conversationId);

    /**
     * 获取用户的活跃会话ID
     *
     * @param userId 用户ID
     * @return 会话ID，如果不存在则返回 null
     */
    String getActiveChat(Long userId);

    /**
     * 续期活跃会话（重新设置 60 秒过期时间）
     *
     * @param userId 用户ID
     */
    void renewActiveChat(Long userId);

    /**
     * 增加未读消息数
     *
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @return 增加后的未读数
     */
    Long incrementUnread(Long userId, String conversationId);

    /**
     * 清除未读消息数（删除 Redis Key）
     *
     * @param userId 用户ID
     * @param conversationId 会话ID
     */
    void clearUnread(Long userId, String conversationId);

    /**
     * 获取单个会话的未读数
     *
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @return 未读数，如果不存在则返回 0
     */
    Integer getUnreadCount(Long userId, String conversationId);

    /**
     * 批量获取多个会话的未读数
     *
     * @param userId 用户ID
     * @param conversationIds 会话ID列表
     * @return Map<conversationId, unreadCount>，不存在的会话未读数为 0
     */
    Map<String, Integer> batchGetUnreadCounts(Long userId, List<String> conversationIds);
}
