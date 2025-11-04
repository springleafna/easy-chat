package com.springleaf.easychat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.easychat.enums.ConversationStatusEnum;
import com.springleaf.easychat.enums.ConversationTypeEnum;
import com.springleaf.easychat.enums.MessageTypeEnum;
import com.springleaf.easychat.exception.BusinessException;
import com.springleaf.easychat.mapper.ConversationMapper;
import com.springleaf.easychat.model.entity.Conversation;
import com.springleaf.easychat.model.entity.Friend;
import com.springleaf.easychat.model.entity.Group;
import com.springleaf.easychat.model.entity.Message;
import com.springleaf.easychat.model.entity.User;
import com.springleaf.easychat.model.vo.ConversationVO;
import com.springleaf.easychat.service.ConversationService;
import com.springleaf.easychat.service.FriendService;
import com.springleaf.easychat.service.GroupService;
import com.springleaf.easychat.service.MessageService;
import com.springleaf.easychat.service.UnreadService;
import com.springleaf.easychat.service.UserService;
import com.springleaf.easychat.utils.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 会话服务实现类
 */
@Slf4j
@Service
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation> implements ConversationService {

    @Resource
    private UserService userService;

    @Resource
    private GroupService groupService;

    @Resource
    private FriendService friendService;

    @Resource
    private MessageService messageService;

    @Resource
    private UnreadService unreadService;

    @Override
    public List<ConversationVO> getConversationList() {
        // 1. 获取当前登录用户ID
        Long userId = UserContextUtil.getCurrentUserId();

        // 2. 查询当前用户的所有正常状态的会话
        LambdaQueryWrapper<Conversation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Conversation::getUserId, userId)
                .eq(Conversation::getStatus, ConversationStatusEnum.NORMAL.getCode())
                .orderByDesc(Conversation::getPinned) // 置顶的排在前面
                .orderByDesc(Conversation::getLastMessageTime); // 按最后消息时间倒序
        List<Conversation> conversationList = this.list(queryWrapper);

        if (conversationList.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 分离单聊和群聊的targetId
        List<Long> singleChatTargetIds = new ArrayList<>();
        List<Long> groupChatTargetIds = new ArrayList<>();

        for (Conversation conversation : conversationList) {
            if (ConversationTypeEnum.SINGLE.getCode().equals(conversation.getType())) {
                singleChatTargetIds.add(conversation.getTargetId());
            } else if (ConversationTypeEnum.GROUP.getCode().equals(conversation.getType())) {
                groupChatTargetIds.add(conversation.getTargetId());
            }
        }

        // 4. 批量查询单聊好友信息和备注名
        Map<Long, User> userMap = null;
        Map<Long, String> remarkNameMap = null;
        if (!singleChatTargetIds.isEmpty()) {
            List<User> users = userService.listByIds(singleChatTargetIds);
            userMap = users.stream().collect(Collectors.toMap(User::getId, user -> user));

            // 查询好友备注名
            LambdaQueryWrapper<Friend> friendQueryWrapper = new LambdaQueryWrapper<>();
            friendQueryWrapper.eq(Friend::getUserId, userId)
                    .in(Friend::getFriendId, singleChatTargetIds);
            List<Friend> friendList = friendService.list(friendQueryWrapper);
            remarkNameMap = friendList.stream()
                    .filter(friend -> friend.getRemarkName() != null && !friend.getRemarkName().isEmpty())
                    .collect(Collectors.toMap(Friend::getFriendId, Friend::getRemarkName));
        }

        // 5. 批量查询群组信息
        Map<Long, Group> groupMap = null;
        if (!groupChatTargetIds.isEmpty()) {
            List<Group> groups = groupService.listByIds(groupChatTargetIds);
            groupMap = groups.stream().collect(Collectors.toMap(Group::getId, group -> group));
        }

        // 6. 批量查询最后一条消息
        List<Long> lastMessageIds = conversationList.stream()
                .map(Conversation::getLastMessageId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Message> messageMap = null;
        if (!lastMessageIds.isEmpty()) {
            List<Message> messages = messageService.listByIds(lastMessageIds);
            messageMap = messages.stream().collect(Collectors.toMap(Message::getId, message -> message));
        }

        // 7. 批量查询未读数
        List<String> conversationIds = conversationList.stream()
                .map(Conversation::getConversationId)
                .collect(Collectors.toList());
        Map<String, Integer> unreadCountMap = unreadService.batchGetUnreadCounts(userId, conversationIds);

        // 8. 组装ConversationVO
        List<ConversationVO> conversationVOList = new ArrayList<>();
        for (Conversation conversation : conversationList) {
            ConversationVO conversationVO = new ConversationVO();
            conversationVO.setConversationId(conversation.getConversationId());
            conversationVO.setType(conversation.getType());
            conversationVO.setTargetId(conversation.getTargetId());
            conversationVO.setLastMessageId(conversation.getLastMessageId());
            conversationVO.setLastMessageTime(conversation.getLastMessageTime());
            conversationVO.setStatus(conversation.getStatus());
            conversationVO.setPinned(conversation.getPinned());
            conversationVO.setMuted(conversation.getMuted());
            conversationVO.setCreatedAt(conversation.getCreatedAt());
            conversationVO.setUpdatedAt(conversation.getUpdatedAt());

            // 设置未读数
            conversationVO.setUnreadCount(unreadCountMap.getOrDefault(conversation.getConversationId(), 0));

            // 根据会话类型设置会话名称和头像
            if (ConversationTypeEnum.SINGLE.getCode().equals(conversation.getType())) {
                // 单聊：显示好友信息
                if (userMap != null) {
                    User friendUser = userMap.get(conversation.getTargetId());
                    if (friendUser != null) {
                        // 优先显示备注名，没有备注名则显示昵称
                        String displayName = friendUser.getNickname();
                        if (remarkNameMap != null && remarkNameMap.containsKey(conversation.getTargetId())) {
                            displayName = remarkNameMap.get(conversation.getTargetId());
                        }
                        conversationVO.setConversationName(displayName);
                        conversationVO.setAvatarUrl(friendUser.getAvatarUrl());
                    }
                }
            } else if (ConversationTypeEnum.GROUP.getCode().equals(conversation.getType())) {
                // 群聊：显示群组信息
                if (groupMap != null) {
                    Group group = groupMap.get(conversation.getTargetId());
                    if (group != null) {
                        conversationVO.setConversationName(group.getGroupName());
                        conversationVO.setAvatarUrl(group.getAvatarUrl());
                    }
                }
            }

            // 设置最后一条消息内容
            if (conversation.getLastMessageId() != null && messageMap != null) {
                Message lastMessage = messageMap.get(conversation.getLastMessageId());
                if (lastMessage != null) {
                    conversationVO.setLastMessageContent(formatMessageContent(lastMessage));
                } else {
                    conversationVO.setLastMessageContent("");
                }
            } else {
                conversationVO.setLastMessageContent("");
            }

            conversationVOList.add(conversationVO);
        }

        log.info("查询会话列表成功，用户ID：{}, 会话数量：{}", userId, conversationVOList.size());
        return conversationVOList;
    }

    @Override
    public void togglePin(String conversationId) {
        Long userId = UserContextUtil.getCurrentUserId();

        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId)
               .eq(Conversation::getConversationId, conversationId)
               .eq(Conversation::getStatus, ConversationStatusEnum.NORMAL.getCode());
        Conversation conversation = this.getOne(wrapper);

        if (conversation == null) {
            throw new BusinessException("会话不存在");
        }

        // 切换置顶状态
        conversation.setPinned(!conversation.getPinned());

        // 使用 update 方法而不是 updateById，因为没有单独的 id 主键
        LambdaQueryWrapper<Conversation> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(Conversation::getUserId, userId)
                    .eq(Conversation::getConversationId, conversationId);
        this.update(conversation, updateWrapper);

        log.info("切换会话置顶状态成功，用户ID: {}, 会话ID: {}, 置顶: {}", userId, conversationId, conversation.getPinned());
    }

    @Override
    public void toggleMute(String conversationId) {
        Long userId = UserContextUtil.getCurrentUserId();

        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId)
               .eq(Conversation::getConversationId, conversationId)
               .eq(Conversation::getStatus, ConversationStatusEnum.NORMAL.getCode());
        Conversation conversation = this.getOne(wrapper);

        if (conversation == null) {
            throw new BusinessException("会话不存在");
        }

        // 切换免打扰状态
        conversation.setMuted(!conversation.getMuted());

        // 使用 update 方法而不是 updateById，因为没有单独的 id 主键
        LambdaQueryWrapper<Conversation> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(Conversation::getUserId, userId)
                    .eq(Conversation::getConversationId, conversationId);
        this.update(conversation, updateWrapper);

        log.info("切换会话免打扰状态成功，用户ID: {}, 会话ID: {}, 免打扰: {}", userId, conversationId, conversation.getMuted());
    }

    /**
     * 格式化消息内容用于会话列表显示
     * 根据不同的消息类型显示不同的提示文本
     */
    private String formatMessageContent(Message message) {
        if (message == null || message.getMessageType() == null) {
            return "";
        }

        MessageTypeEnum messageType = MessageTypeEnum.getByCode(message.getMessageType());
        if (messageType == null) {
            return "";
        }

        switch (messageType) {
            case TEXT:
                // 文本消息：直接显示内容，如果过长则截取
                String content = message.getContent();
                if (content != null && content.length() > 50) {
                    return content.substring(0, 50) + "...";
                }
                return content != null ? content : "";

            case IMAGE:
                return "[图片]";

            case VOICE:
                return "[语音]";

            case VIDEO:
                return "[��频]";

            case FILE:
                // 文件消息：显示文件名
                String fileName = message.getFileName();
                if (fileName != null && !fileName.isEmpty()) {
                    return "[文件] " + fileName;
                }
                return "[文件]";

            case LOCATION:
                return "[位置]";

            case SYSTEM:
                // 系统消息：显示内容
                return message.getContent() != null ? message.getContent() : "[系统消息]";

            default:
                return "";
        }
    }
}
