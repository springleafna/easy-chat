package com.springleaf.easychat.utils;

import com.springleaf.easychat.enums.ConversationTypeEnum;
import com.springleaf.easychat.exception.BusinessException;

/**
 * 会话ID工具类
 * 用于生成和解析会话ID
 *
 * 规则：
 * - 单聊：s_{min}_{max} (min和max是对话双方的用户ID，按大小排序)
 * - 群聊：g_{group_id}
 */
public class ConversationIdUtil {

    private static final String SINGLE_CHAT_PREFIX = "s_";
    private static final String GROUP_CHAT_PREFIX = "g_";
    private static final String SEPARATOR = "_";

    /**
     * 生成单聊会话ID
     *
     * @param userId1 用户1的ID
     * @param userId2 用户2的ID
     * @return 会话ID，格式：s_{min}_{max}
     */
    public static String generateSingleChatId(Long userId1, Long userId2) {
        if (userId1 == null || userId2 == null) {
            throw new BusinessException("用户ID不能为空");
        }

        // 取较小和较大的ID，保证会话ID的唯一性
        long min = Math.min(userId1, userId2);
        long max = Math.max(userId1, userId2);

        return SINGLE_CHAT_PREFIX + min + SEPARATOR + max;
    }

    /**
     * 生成群聊会话ID
     *
     * @param groupId 群组ID
     * @return 会话ID，格式：g_{group_id}
     */
    public static String generateGroupChatId(Long groupId) {
        if (groupId == null) {
            throw new BusinessException("群组ID不能为空");
        }

        return GROUP_CHAT_PREFIX + groupId;
    }

    /**
     * 根据会话类型和目标ID生成会话ID
     *
     * @param conversationType 会话类型
     * @param userId 当前用户ID（单聊时使用）
     * @param targetId 目标ID（单聊为对方user_id，群聊为group_id）
     * @return 会话ID
     */
    public static String generateConversationId(Integer conversationType, Long userId, Long targetId) {
        if (conversationType == null) {
            throw new BusinessException("会话类型不能为空");
        }

        if (ConversationTypeEnum.SINGLE.getCode().equals(conversationType)) {
            return generateSingleChatId(userId, targetId);
        } else if (ConversationTypeEnum.GROUP.getCode().equals(conversationType)) {
            return generateGroupChatId(targetId);
        } else {
            throw new BusinessException("无效的会话类型");
        }
    }

    /**
     * 判断会话类型
     *
     * @param conversationId 会话ID
     * @return 会话类型：1-单聊，2-群聊
     */
    public static Integer getConversationType(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            throw new BusinessException("会话ID不能为空");
        }

        if (conversationId.startsWith(SINGLE_CHAT_PREFIX)) {
            return ConversationTypeEnum.SINGLE.getCode();
        } else if (conversationId.startsWith(GROUP_CHAT_PREFIX)) {
            return ConversationTypeEnum.GROUP.getCode();
        } else {
            throw new BusinessException("无效的会话ID格式");
        }
    }

    /**
     * 从单聊会话ID中提取对方用户ID
     *
     * @param conversationId 会话ID
     * @param currentUserId 当前用户ID
     * @return 对方用户ID
     */
    public static Long extractTargetUserIdFromSingleChat(String conversationId, Long currentUserId) {
        if (!conversationId.startsWith(SINGLE_CHAT_PREFIX)) {
            throw new BusinessException("不是单聊会话ID");
        }

        String[] parts = conversationId.split(SEPARATOR);
        if (parts.length != 3) {
            throw new BusinessException("会话ID格式错误");
        }

        try {
            long userId1 = Long.parseLong(parts[1]);
            long userId2 = Long.parseLong(parts[2]);

            // 返回对方的用户ID
            if (userId1 == currentUserId) {
                return userId2;
            } else if (userId2 == currentUserId) {
                return userId1;
            } else {
                throw new BusinessException("当前用户不在该会话中");
            }
        } catch (NumberFormatException e) {
            throw new BusinessException("会话ID格式错误");
        }
    }

    /**
     * 从群聊会话ID中提取群组ID
     *
     * @param conversationId 会话ID
     * @return 群组ID
     */
    public static Long extractGroupIdFromGroupChat(String conversationId) {
        if (!conversationId.startsWith(GROUP_CHAT_PREFIX)) {
            throw new BusinessException("不是群聊会话ID");
        }

        String groupIdStr = conversationId.substring(GROUP_CHAT_PREFIX.length());
        try {
            return Long.parseLong(groupIdStr);
        } catch (NumberFormatException e) {
            throw new BusinessException("会话ID格式错误");
        }
    }

    /**
     * 从会话ID中提取目标ID
     *
     * @param conversationId 会话ID
     * @param currentUserId 当前用户ID（单聊时需要）
     * @return 目标ID（单聊返回对方用户ID，群聊返回群组ID）
     */
    public static Long extractTargetId(String conversationId, Long currentUserId) {
        Integer type = getConversationType(conversationId);

        if (ConversationTypeEnum.SINGLE.getCode().equals(type)) {
            return extractTargetUserIdFromSingleChat(conversationId, currentUserId);
        } else if (ConversationTypeEnum.GROUP.getCode().equals(type)) {
            return extractGroupIdFromGroupChat(conversationId);
        } else {
            throw new BusinessException("无效的会话类型");
        }
    }

    /**
     * 验证会话ID格式是否正确
     *
     * @param conversationId 会话ID
     * @return true-格式正确，false-格式错误
     */
    public static boolean isValid(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            return false;
        }

        try {
            if (conversationId.startsWith(SINGLE_CHAT_PREFIX)) {
                String[] parts = conversationId.split(SEPARATOR);
                if (parts.length != 3) {
                    return false;
                }
                Long.parseLong(parts[1]);
                Long.parseLong(parts[2]);
                return true;
            } else if (conversationId.startsWith(GROUP_CHAT_PREFIX)) {
                String groupIdStr = conversationId.substring(GROUP_CHAT_PREFIX.length());
                Long.parseLong(groupIdStr);
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
