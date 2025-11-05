package com.springleaf.easychat.service.impl;

import com.springleaf.easychat.constants.RedisKeyConstants;
import com.springleaf.easychat.model.dto.ActiveChatDTO;
import com.springleaf.easychat.service.UnreadService;
import com.springleaf.easychat.utils.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 未读消息服务实现类
 */
@Slf4j
@Service
public class UnreadServiceImpl implements UnreadService {

    private final StringRedisTemplate stringRedisTemplate;

    public UnreadServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void setActiveChat(ActiveChatDTO dto) {
        Long userId = UserContextUtil.getCurrentUserId();
        String conversationId = dto.getConversationId();
        String key = RedisKeyConstants.getActiveChatKey(userId);
        stringRedisTemplate.opsForValue().set(
            key,
            conversationId,
            RedisKeyConstants.ACTIVE_CHAT_EXPIRE_SECONDS,
            TimeUnit.SECONDS
        );
        log.debug("设置活跃会话，用户ID: {}, 会话ID: {}", userId, conversationId);
    }

    @Override
    public void deleteActiveChat(Long userId) {
        String key = RedisKeyConstants.getActiveChatKey(userId);
        stringRedisTemplate.delete(key);
        log.debug("删除活跃会话，用户ID: {}", userId);
    }

    @Override
    public String getActiveChat(Long userId) {
        String key = RedisKeyConstants.getActiveChatKey(userId);
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public void renewActiveChat(Long userId) {
        String key = RedisKeyConstants.getActiveChatKey(userId);
        // 续期 - 重新设置过期时间
        stringRedisTemplate.expire(key, RedisKeyConstants.ACTIVE_CHAT_EXPIRE_SECONDS, TimeUnit.SECONDS);
        log.debug("续期活跃会话，用户ID: {}", userId);
    }

    @Override
    public Long incrementUnread(Long userId, String conversationId) {
        String key = RedisKeyConstants.getUnreadKey(userId, conversationId);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        log.debug("增加未读数，用户ID: {}, 会话ID: {}, 当前未读数: {}", userId, conversationId, count);
        return count;
    }

    @Override
    public void clearUnread(Long userId, String conversationId) {
        String key = RedisKeyConstants.getUnreadKey(userId, conversationId);
        stringRedisTemplate.delete(key);
        log.debug("清除未读数，用户ID: {}, 会话ID: {}", userId, conversationId);
    }

    @Override
    public Integer getUnreadCount(Long userId, String conversationId) {
        String key = RedisKeyConstants.getUnreadKey(userId, conversationId);
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.error("解析未读数失败，key: {}, value: {}", key, value, e);
            return 0;
        }
    }

    @Override
    public Map<String, Integer> batchGetUnreadCounts(Long userId, List<String> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return new HashMap<>();
        }

        // 构建所有 Redis Key
        List<String> keys = conversationIds.stream()
                .map(conversationId -> RedisKeyConstants.getUnreadKey(userId, conversationId))
                .collect(Collectors.toList());

        // 批量查询（MGET）
        List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);

        // 组装结果
        Map<String, Integer> result = new HashMap<>(conversationIds.size());
        for (int i = 0; i < conversationIds.size(); i++) {
            String conversationId = conversationIds.get(i);
            String value = values != null && i < values.size() ? values.get(i) : null;

            int unreadCount = 0;
            if (value != null) {
                try {
                    unreadCount = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    log.error("解析未读数失败，conversationId: {}, value: {}", conversationId, value, e);
                }
            }

            result.put(conversationId, unreadCount);
        }

        log.debug("批量查询未读数，用户ID: {}, 会话数: {}", userId, conversationIds.size());
        return result;
    }
}
