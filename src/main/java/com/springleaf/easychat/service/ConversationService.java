package com.springleaf.easychat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.springleaf.easychat.model.entity.Conversation;
import com.springleaf.easychat.model.vo.ConversationVO;

import java.util.List;

/**
 * 会话服务接口
 */
public interface ConversationService extends IService<Conversation> {

    /**
     * 获取会话列表
     *
     * @return 会话列表
     */
    List<ConversationVO> getConversationList();

    /**
     * 标记会话为已读（清除未读消息数）
     *
     * @param conversationId 会话ID
     */
    void markAsRead(Long conversationId);
}
