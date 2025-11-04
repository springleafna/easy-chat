package com.springleaf.easychat.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springleaf.easychat.enums.ConversationTypeEnum;
import com.springleaf.easychat.mapper.GroupMemberMapper;
import com.springleaf.easychat.model.dto.SendMessageDTO;
import com.springleaf.easychat.model.entity.GroupMember;
import com.springleaf.easychat.model.vo.MessageVO;
import com.springleaf.easychat.service.MessageService;
import com.springleaf.easychat.service.UnreadService;
import com.springleaf.easychat.utils.ConversationIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 聊天处理器
 */
@Slf4j
@Component
public class ChatWebSocketHandler implements WebSocketHandler {

    /**
     * 存储所有在线用户的 WebSocket 会话
     * key: userId, value: WebSocketSession
     */
    private static final Map<Long, WebSocketSession> ONLINE_USERS = new ConcurrentHashMap<>();

    private final MessageService messageService;
    private final GroupMemberMapper groupMemberMapper;
    private final ObjectMapper objectMapper;
    private final UnreadService unreadService;

    public ChatWebSocketHandler(MessageService messageService,
                               GroupMemberMapper groupMemberMapper,
                               ObjectMapper objectMapper,
                               UnreadService unreadService) {
        this.messageService = messageService;
        this.groupMemberMapper = groupMemberMapper;
        this.objectMapper = objectMapper;
        this.unreadService = unreadService;
    }

    /**
     * WebSocket 连接建立后
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            ONLINE_USERS.put(userId, session);
            log.info("用户 {} 建立 WebSocket 连接，当前在线人数: {}", userId, ONLINE_USERS.size());
        }
    }

    /**
     * 处理接收到的消息
     */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage) {
            String payload = ((TextMessage) message).getPayload();
            log.info("收到消息: {}", payload);

            try {
                // 解析消息
                SendMessageDTO messageDTO = objectMapper.readValue(payload, SendMessageDTO.class);
                Long senderId = (Long) session.getAttributes().get("userId");
                messageDTO.setSenderId(senderId);

                // 发送消息（通过 Service 层处理业务逻辑）
                MessageVO messageVO = messageService.sendMessage(messageDTO);

                // 推送消息给接收者
                pushMessage(messageVO);

            } catch (Exception e) {
                log.error("处理消息失败", e);
                sendErrorMessage(session, "消息发送失败: " + e.getMessage());
            }
        }
    }

    /**
     * 处理传输错误
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        log.error("WebSocket 传输错误，用户ID: {}", userId, exception);
    }

    /**
     * WebSocket 连接关闭后
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            ONLINE_USERS.remove(userId);
            log.info("用户 {} 断开 WebSocket 连接，当前在线人数: {}", userId, ONLINE_USERS.size());
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 推送消息给接收者
     */
    private void pushMessage(MessageVO messageVO) {
        try {
            String messageJson = objectMapper.writeValueAsString(messageVO);
            TextMessage textMessage = new TextMessage(messageJson);

            // 单聊：推送给接收者
            if (ConversationTypeEnum.SINGLE.getCode().equals(messageVO.getConversationType())) {
                // 从会话ID中提取对方用户ID
                Long receiverId = ConversationIdUtil.extractTargetId(
                    messageVO.getConversationId(),
                    messageVO.getSenderId()
                );

                // 检查接收者的活跃会话
                String activeConversationId = unreadService.getActiveChat(receiverId);
                boolean isActiveChat = messageVO.getConversationId().equals(activeConversationId);

                WebSocketSession receiverSession = ONLINE_USERS.get(receiverId);
                if (receiverSession != null && receiverSession.isOpen()) {
                    // 接收者在线
                    if (isActiveChat) {
                        // 活跃会话匹配：续期活跃状态，推送消息，不增加未读数
                        unreadService.renewActiveChat(receiverId);
                        receiverSession.sendMessage(textMessage);
                        log.info("单聊消息已推送给在线用户（活跃会话），未增加未读数，用户ID: {}, 会话ID: {}",
                                receiverId, messageVO.getConversationId());
                    } else {
                        // 活跃会话不匹配：推送消息，增加未读数
                        unreadService.incrementUnread(receiverId, messageVO.getConversationId());
                        receiverSession.sendMessage(textMessage);
                        log.info("单聊消息已推送给在线用户（非活跃会话），已增加未读数，用户ID: {}, 会话ID: {}",
                                receiverId, messageVO.getConversationId());
                    }
                } else {
                    // 接收者离线：增加未读数
                    unreadService.incrementUnread(receiverId, messageVO.getConversationId());
                    log.info("接收者离线，已增加未读数，用户ID: {}, 会话ID: {}", receiverId, messageVO.getConversationId());
                }
            }
            // 群聊：推送给所有在线群成员
            else if (ConversationTypeEnum.GROUP.getCode().equals(messageVO.getConversationType())) {
                // 从会话ID中提取群组ID
                Long groupId = ConversationIdUtil.extractGroupIdFromGroupChat(messageVO.getConversationId());

                // 查询群成员列表
                LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(GroupMember::getGroupId, groupId);
                List<GroupMember> groupMembers = groupMemberMapper.selectList(wrapper);

                int onlineCount = 0;
                int unreadIncrementCount = 0;
                for (GroupMember member : groupMembers) {
                    // 跳过发送者自己（发送者不需要接收自己的消息）
                    if (member.getUserId().equals(messageVO.getSenderId())) {
                        continue;
                    }

                    // 检查该成员的活跃会话
                    String activeChatId = unreadService.getActiveChat(member.getUserId());
                    boolean isActiveChat = messageVO.getConversationId().equals(activeChatId);

                    // 推送给所有在线成员
                    WebSocketSession memberSession = ONLINE_USERS.get(member.getUserId());
                    if (memberSession != null && memberSession.isOpen()) {
                        try {
                            if (isActiveChat) {
                                // 活跃会话匹配：续期，推送，不增加未读数
                                unreadService.renewActiveChat(member.getUserId());
                                memberSession.sendMessage(textMessage);
                                onlineCount++;
                            } else {
                                // 活跃会话不匹配：推送，增加未读数
                                unreadService.incrementUnread(member.getUserId(), messageVO.getConversationId());
                                memberSession.sendMessage(textMessage);
                                onlineCount++;
                                unreadIncrementCount++;
                            }
                        } catch (IOException e) {
                            log.error("推送消息给群成员 {} 失败", member.getUserId(), e);
                        }
                    } else {
                        // 成员离线：增加未读数
                        unreadService.incrementUnread(member.getUserId(), messageVO.getConversationId());
                        unreadIncrementCount++;
                    }
                }
                log.info("群聊消息已推送，会话ID: {}, 群组ID: {}, 在线成员数: {}/{}, 增加未读数成员数: {}",
                         messageVO.getConversationId(), groupId, onlineCount, groupMembers.size() - 1, unreadIncrementCount);
            }

        } catch (Exception e) {
            log.error("推送消息失败", e);
        }
    }

    /**
     * 发送错误消息
     */
    private void sendErrorMessage(WebSocketSession session, String errorMsg) {
        try {
            Map<String, String> error = Map.of("error", errorMsg);
            String errorJson = objectMapper.writeValueAsString(error);
            session.sendMessage(new TextMessage(errorJson));
        } catch (IOException e) {
            log.error("发送错误消息失败", e);
        }
    }

    /**
     * 获取在线用户数量
     */
    public static int getOnlineUserCount() {
        return ONLINE_USERS.size();
    }

    /**
     * 判断用户是否在线
     */
    public static boolean isUserOnline(Long userId) {
        return ONLINE_USERS.containsKey(userId);
    }
}
