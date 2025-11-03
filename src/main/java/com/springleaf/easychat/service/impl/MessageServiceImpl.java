package com.springleaf.easychat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.easychat.enums.ConversationTypeEnum;
import com.springleaf.easychat.enums.MessageStatusEnum;
import com.springleaf.easychat.exception.BusinessException;
import com.springleaf.easychat.mapper.ConversationMapper;
import com.springleaf.easychat.mapper.GroupMemberMapper;
import com.springleaf.easychat.mapper.MessageMapper;
import com.springleaf.easychat.model.dto.SendMessageDTO;
import com.springleaf.easychat.model.entity.Conversation;
import com.springleaf.easychat.model.entity.GroupMember;
import com.springleaf.easychat.model.entity.Message;
import com.springleaf.easychat.model.entity.User;
import com.springleaf.easychat.model.vo.MessageVO;
import com.springleaf.easychat.service.MessageService;
import com.springleaf.easychat.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息服务实现类
 */
@Slf4j
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private final UserService userService;
    private final ConversationMapper conversationMapper;
    private final GroupMemberMapper groupMemberMapper;

    public MessageServiceImpl(UserService userService,
                            ConversationMapper conversationMapper,
                            GroupMemberMapper groupMemberMapper) {
        this.userService = userService;
        this.conversationMapper = conversationMapper;
        this.groupMemberMapper = groupMemberMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageVO sendMessage(Long senderId, SendMessageDTO messageDTO) {
        // 验证参数
        validateMessageDTO(messageDTO);

        // 获取发送者信息
        User sender = userService.getById(senderId);
        if (sender == null) {
            throw new BusinessException("发送者不存在");
        }

        // 创建消息实体
        Message message = new Message();
        message.setSenderId(senderId);
        message.setMessageType(messageDTO.getMessageType());
        message.setContent(messageDTO.getContent());
        message.setMediaUrl(messageDTO.getMediaUrl());
        message.setFileName(messageDTO.getFileName());
        message.setFileSize(messageDTO.getFileSize());
        message.setStatus(MessageStatusEnum.NORMAL.getCode()); // 正常状态

        // 处理单聊或群聊，设置消息的基本信息（receiverId/groupId 和 conversationId）
        List<Conversation> conversationsToUpdate = new ArrayList<>();
        if (messageDTO.getConversationType() == ConversationTypeEnum.SINGLE.getCode()) {
            // 单聊
            conversationsToUpdate = handlePrivateMessage(message, senderId, messageDTO.getReceiverId());
        } else if (messageDTO.getConversationType() == ConversationTypeEnum.GROUP.getCode()) {
            // 群聊
            conversationsToUpdate = handleGroupMessage(message, senderId, messageDTO.getGroupId());
        } else {
            throw new BusinessException("无效的会话类型");
        }

        // 保存消息（此时会生成消息ID）
        this.save(message);

        // 消息保存成功后，更新相关会话的最后消息信息
        for (Conversation conversation : conversationsToUpdate) {
            updateConversation(conversation, message);
        }

        // 构建返回的 MessageVO
        MessageVO messageVO = new MessageVO();
        BeanUtils.copyProperties(message, messageVO);
        messageVO.setConversationType(messageDTO.getConversationType());
        messageVO.setSenderNickname(sender.getNickname());
        messageVO.setSenderAvatar(sender.getAvatarUrl());

        log.info("消息发送成功，消息ID: {}", message.getId());
        return messageVO;
    }

    /**
     * 处理单聊消息
     * @return 需要更新的会话列表
     */
    private List<Conversation> handlePrivateMessage(Message message, Long senderId, Long receiverId) {
        if (receiverId == null) {
            throw new BusinessException("接收者ID不能为空");
        }

        // 验证接收者是否存在
        User receiver = userService.getById(receiverId);
        if (receiver == null) {
            throw new BusinessException("接收者不存在");
        }

        message.setReceiverId(receiverId);

        // 查找或创建会话
        Conversation senderConversation = findOrCreateConversation(senderId, receiverId, ConversationTypeEnum.SINGLE.getCode());
        Conversation receiverConversation = findOrCreateConversation(receiverId, senderId, ConversationTypeEnum.SINGLE.getCode());

        message.setConversationId(senderConversation.getId());

        // 返回需要更新的会话列表（在消息保存后再更新）
        List<Conversation> conversations = new ArrayList<>();
        conversations.add(senderConversation);
        conversations.add(receiverConversation);
        return conversations;
    }

    /**
     * 处理群聊消息
     * @return 需要更新的会话列表
     */
    private List<Conversation> handleGroupMessage(Message message, Long senderId, Long groupId) {
        if (groupId == null) {
            throw new BusinessException("群组ID不能为空");
        }

        // 验证用户是否在群组中
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId)
               .eq(GroupMember::getUserId, senderId);
        GroupMember groupMember = groupMemberMapper.selectOne(wrapper);

        if (groupMember == null) {
            throw new BusinessException("您不在该群组中");
        }

        message.setGroupId(groupId);

        // 查找或创建发送者的会话
        Conversation senderConversation = findOrCreateConversation(senderId, groupId, ConversationTypeEnum.GROUP.getCode());
        message.setConversationId(senderConversation.getId());

        // 查询所有群成员，为每个成员创建或更新会话
        LambdaQueryWrapper<GroupMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(GroupMember::getGroupId, groupId);
        List<GroupMember> groupMembers = groupMemberMapper.selectList(memberWrapper);

        List<Conversation> conversations = new ArrayList<>();
        for (GroupMember member : groupMembers) {
            Conversation memberConversation = findOrCreateConversation(member.getUserId(), groupId, ConversationTypeEnum.GROUP.getCode());
            conversations.add(memberConversation);
        }

        return conversations;
    }

    /**
     * 查找或创建会话
     */
    private Conversation findOrCreateConversation(Long userId, Long targetId, Integer type) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId)
               .eq(Conversation::getTargetId, targetId)
               .eq(Conversation::getType, type)
               .eq(Conversation::getStatus, 1);

        Conversation conversation = conversationMapper.selectOne(wrapper);

        if (conversation == null) {
            conversation = new Conversation();
            conversation.setUserId(userId);
            conversation.setTargetId(targetId);
            conversation.setType(type);
            conversation.setUnreadCount(0);
            conversation.setStatus(1);
            conversationMapper.insert(conversation);
        }

        return conversation;
    }

    /**
     * 更新会话信息
     */
    private void updateConversation(Conversation conversation, Message message) {
        conversation.setLastMessageId(message.getId());
        conversation.setLastMessageTime(LocalDateTime.now());
        // 如果不是发送者的会话，增加未读数
        if (!conversation.getUserId().equals(message.getSenderId())) {
            conversation.setUnreadCount(conversation.getUnreadCount() + 1);
        }
        conversationMapper.updateById(conversation);
    }

    /**
     * 验证消息 DTO
     */
    private void validateMessageDTO(SendMessageDTO messageDTO) {
        if (messageDTO.getMessageType() == null) {
            throw new BusinessException("消息类型不能为空");
        }
        if (messageDTO.getConversationType() == null) {
            throw new BusinessException("会话类型不能为空");
        }

        // 单聊时必须有接收者ID
        if (messageDTO.getConversationType() == ConversationTypeEnum.SINGLE.getCode()
            && messageDTO.getReceiverId() == null) {
            throw new BusinessException("单聊时接收者ID不能为空");
        }

        // 群聊时必须有群组ID
        if (messageDTO.getConversationType() == ConversationTypeEnum.GROUP.getCode()
            && messageDTO.getGroupId() == null) {
            throw new BusinessException("群聊时群组ID不能为空");
        }
    }
}
