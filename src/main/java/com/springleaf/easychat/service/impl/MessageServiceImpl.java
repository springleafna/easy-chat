package com.springleaf.easychat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.easychat.enums.ConversationTypeEnum;
import com.springleaf.easychat.enums.MessageStatusEnum;
import com.springleaf.easychat.exception.BusinessException;
import com.springleaf.easychat.mapper.ConversationMapper;
import com.springleaf.easychat.mapper.GroupMemberMapper;
import com.springleaf.easychat.mapper.MessageMapper;
import com.springleaf.easychat.model.dto.MessageHistoryDTO;
import com.springleaf.easychat.model.dto.SendMessageDTO;
import com.springleaf.easychat.model.entity.Conversation;
import com.springleaf.easychat.model.entity.GroupMember;
import com.springleaf.easychat.model.entity.Message;
import com.springleaf.easychat.model.entity.User;
import com.springleaf.easychat.model.vo.MessageVO;
import com.springleaf.easychat.service.MessageService;
import com.springleaf.easychat.service.UserService;
import com.springleaf.easychat.utils.ConversationIdUtil;
import com.springleaf.easychat.utils.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public MessageVO sendMessage(SendMessageDTO messageDTO) {
        // 验证参数
        validateMessageDTO(messageDTO);
        Long senderId = messageDTO.getSenderId();

        // 获取发送者信息
        User sender = userService.getById(senderId);
        if (sender == null) {
            throw new BusinessException("发送者不存在");
        }

        // 生成会话ID
        String conversationId;
        List<Conversation> conversationsToUpdate;

        if (messageDTO.getConversationType().equals(ConversationTypeEnum.SINGLE.getCode())) {
            // 单聊
            if (messageDTO.getReceiverId() == null) {
                throw new BusinessException("单聊时接收者ID不能为空");
            }

            // 验证接收者是否存在
            User receiver = userService.getById(messageDTO.getReceiverId());
            if (receiver == null) {
                throw new BusinessException("接收者不存在");
            }

            conversationId = ConversationIdUtil.generateSingleChatId(senderId, messageDTO.getReceiverId());
            conversationsToUpdate = handlePrivateMessage(conversationId, senderId, messageDTO.getReceiverId());
        } else if (messageDTO.getConversationType().equals(ConversationTypeEnum.GROUP.getCode())) {
            // 群聊
            if (messageDTO.getGroupId() == null) {
                throw new BusinessException("群聊时群组ID不能为空");
            }

            // 验证用户是否在群组中
            LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(GroupMember::getGroupId, messageDTO.getGroupId())
                   .eq(GroupMember::getUserId, senderId);
            GroupMember groupMember = groupMemberMapper.selectOne(wrapper);

            if (groupMember == null) {
                throw new BusinessException("您不在该群组中");
            }

            conversationId = ConversationIdUtil.generateGroupChatId(messageDTO.getGroupId());
            conversationsToUpdate = handleGroupMessage(conversationId, messageDTO.getGroupId());
        } else {
            throw new BusinessException("无效的会话类型");
        }

        // 创建消息实体
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setMessageType(messageDTO.getMessageType());
        message.setContent(messageDTO.getContent());
        message.setMediaUrl(messageDTO.getMediaUrl());
        message.setFileName(messageDTO.getFileName());
        message.setFileSize(messageDTO.getFileSize());
        message.setStatus(MessageStatusEnum.NORMAL.getCode());

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

        log.info("消息发送成功，消息ID: {}, 会话ID: {}", message.getId(), conversationId);
        return messageVO;
    }

    /**
     * 处理单聊消息
     * @return 需要更新的会话列表
     */
    private List<Conversation> handlePrivateMessage(String conversationId, Long senderId, Long receiverId) {
        // 查找或创建双方的会话
        Conversation senderConversation = findOrCreateConversation(conversationId, senderId, receiverId, ConversationTypeEnum.SINGLE.getCode());
        Conversation receiverConversation = findOrCreateConversation(conversationId, receiverId, senderId, ConversationTypeEnum.SINGLE.getCode());

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
    private List<Conversation> handleGroupMessage(String conversationId, Long groupId) {
        // 查询所有群成员，为每个成员创建或更新会话
        LambdaQueryWrapper<GroupMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(GroupMember::getGroupId, groupId);
        List<GroupMember> groupMembers = groupMemberMapper.selectList(memberWrapper);

        List<Conversation> conversations = new ArrayList<>();
        for (GroupMember member : groupMembers) {
            Conversation memberConversation = findOrCreateConversation(conversationId, member.getUserId(), groupId, ConversationTypeEnum.GROUP.getCode());
            conversations.add(memberConversation);
        }

        return conversations;
    }

    /**
     * 查找或创建会话
     */
    private Conversation findOrCreateConversation(String conversationId, Long userId, Long targetId, Integer type) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId)
               .eq(Conversation::getConversationId, conversationId)
               .eq(Conversation::getStatus, 1);

        Conversation conversation = conversationMapper.selectOne(wrapper);

        if (conversation == null) {
            conversation = new Conversation();
            conversation.setConversationId(conversationId);
            conversation.setUserId(userId);
            conversation.setTargetId(targetId);
            conversation.setType(type);
            conversation.setStatus(1);
            conversation.setPinned(false);
            conversation.setMuted(false);
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

        // 使用联合主键条件更新，因为 conversations 表使用 (user_id, conversation_id) 作为联合主键
        LambdaQueryWrapper<Conversation> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(Conversation::getUserId, conversation.getUserId())
                    .eq(Conversation::getConversationId, conversation.getConversationId());
        conversationMapper.update(conversation, updateWrapper);
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
        if (messageDTO.getConversationType().equals(ConversationTypeEnum.SINGLE.getCode())
            && messageDTO.getReceiverId() == null) {
            throw new BusinessException("单聊时接收者ID不能为空");
        }

        // 群聊时必须有群组ID
        if (messageDTO.getConversationType().equals(ConversationTypeEnum.GROUP.getCode())
            && messageDTO.getGroupId() == null) {
            throw new BusinessException("群聊时群组ID不能为空");
        }
    }

    @Override
    public Page<MessageVO> getMessageHistory(MessageHistoryDTO queryDTO) {
        // 1. 获取当前登录用户ID
        Long currentUserId = UserContextUtil.getCurrentUserId();

        // 2. 验证会话ID格式
        if (!ConversationIdUtil.isValid(queryDTO.getConversationId())) {
            throw new BusinessException("无效的会话ID格式");
        }

        // 3. 查询会话，验证会话是否存在且用户有权限访问
        LambdaQueryWrapper<Conversation> conversationWrapper = new LambdaQueryWrapper<>();
        conversationWrapper.eq(Conversation::getUserId, currentUserId)
                          .eq(Conversation::getConversationId, queryDTO.getConversationId())
                          .eq(Conversation::getStatus, 1);
        Conversation conversation = conversationMapper.selectOne(conversationWrapper);

        if (conversation == null) {
            throw new BusinessException("会话不存在或无权访问");
        }

        // 4. 构建查询条件
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Message::getConversationId, queryDTO.getConversationId())
                    .ne(Message::getStatus, MessageStatusEnum.DELETED.getCode()) // 排除已删除的消息
                    .orderByDesc(Message::getCreatedAt); // 按时间倒序（最新的在前）

        // 5. 如果提供了 lastMessageId，使用游标分页（推荐）
        if (queryDTO.getLastMessageId() != null) {
            Message lastMessage = this.getById(queryDTO.getLastMessageId());
            if (lastMessage != null) {
                // 查询比这条消息更早的消息（ID 更小或时间更早）
                queryWrapper.lt(Message::getId, queryDTO.getLastMessageId());
            }
        }

        // 6. 执行分页查询
        Page<Message> messagePage = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        messagePage = this.page(messagePage, queryWrapper);

        // 7. 转换为 MessageVO 并填充发送者信息
        List<MessageVO> messageVOList = convertToMessageVOList(messagePage.getRecords(), conversation.getType());

        // 8. 构建分页结果
        Page<MessageVO> resultPage = new Page<>(messagePage.getCurrent(), messagePage.getSize(), messagePage.getTotal());
        resultPage.setRecords(messageVOList);

        log.info("查询历史消息成功，会话ID: {}, 用户ID: {}, 当前页: {}, 每页大小: {}, 总数: {}",
                queryDTO.getConversationId(), currentUserId, messagePage.getCurrent(),
                messagePage.getSize(), messagePage.getTotal());

        return resultPage;
    }

    @Override
    public void deleteMessage(Long messageId) {
        Long currentUserId = UserContextUtil.getCurrentUserId();
        Message message = this.getById(messageId);
        if (message == null) {
            throw new BusinessException("消息不存在");
        }
        if (!message.getSenderId().equals(currentUserId)) {
            throw new BusinessException("无权删除此消息");
        }
        message.setStatus(MessageStatusEnum.DELETED.getCode());
        this.updateById(message);
    }

    @Override
    public void recallMessage(Long messageId) {
        Long currentUserId = UserContextUtil.getCurrentUserId();
        Message message = this.getById(messageId);
        if (message == null) {
            throw new BusinessException("消息不存在");
        }
        if (!message.getSenderId().equals(currentUserId)) {
            throw new BusinessException("无权撤回此消息");
        }
        message.setStatus(MessageStatusEnum.WITHDRAWN.getCode());
        this.updateById(message);
    }

    /**
     * 将 Message 列表转换为 MessageVO 列表，并填充发送者信息
     */
    private List<MessageVO> convertToMessageVOList(List<Message> messages, Integer conversationType) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 收集所有发送者ID
        List<Long> senderIds = messages.stream()
                .map(Message::getSenderId)
                .distinct()
                .collect(Collectors.toList());

        // 2. 批量查询发送者信息
        Map<Long, User> userMap = userService.listByIds(senderIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 3. 转换为 MessageVO 并填充信息
        List<MessageVO> messageVOList = new ArrayList<>();
        for (Message message : messages) {
            MessageVO messageVO = new MessageVO();
            BeanUtils.copyProperties(message, messageVO);

            // 设置会话类型
            messageVO.setConversationType(conversationType);

            // 填充发送者信息
            User sender = userMap.get(message.getSenderId());
            if (sender != null) {
                messageVO.setSenderNickname(sender.getNickname());
                messageVO.setSenderAvatar(sender.getAvatarUrl());
            }

            messageVOList.add(messageVO);
        }

        return messageVOList;
    }
}
