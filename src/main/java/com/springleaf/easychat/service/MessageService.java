package com.springleaf.easychat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.springleaf.easychat.model.dto.SendMessageDTO;
import com.springleaf.easychat.model.entity.Message;
import com.springleaf.easychat.model.vo.MessageVO;

/**
 * 消息服务接口
 */
public interface MessageService extends IService<Message> {

    /**
     * 发送消息（单聊或群聊）
     *
     * @param senderId 发送者ID
     * @param messageDTO 消息DTO
     * @return 消息VO
     */
    MessageVO sendMessage(Long senderId, SendMessageDTO messageDTO);
}
