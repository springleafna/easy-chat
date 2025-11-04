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
     * 切换会话置顶状态
     *
     * @param conversationId 会话ID
     */
    void togglePin(String conversationId);

    /**
     * 切换会话免打扰状态
     *
     * @param conversationId 会话ID
     */
    void toggleMute(String conversationId);
}
