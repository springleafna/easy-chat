package com.springleaf.easychat.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springleaf.easychat.mapper.GroupMemberMapper;
import com.springleaf.easychat.model.dto.SendMessageDTO;
import com.springleaf.easychat.model.entity.GroupMember;
import com.springleaf.easychat.model.vo.MessageVO;
import com.springleaf.easychat.service.MessageService;
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

    public ChatWebSocketHandler(MessageService messageService,
                               GroupMemberMapper groupMemberMapper,
                               ObjectMapper objectMapper) {
        this.messageService = messageService;
        this.groupMemberMapper = groupMemberMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * WebSocket 连接建立后
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
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
            if (messageVO.getConversationType() == 1 && messageVO.getReceiverId() != null) {
                WebSocketSession receiverSession = ONLINE_USERS.get(messageVO.getReceiverId());
                if (receiverSession != null && receiverSession.isOpen()) {
                    receiverSession.sendMessage(textMessage);
                    log.info("单聊消息已推送给用户 {}", messageVO.getReceiverId());
                } else {
                    log.info("接收者 {} 不在线，消息将存储到数据库", messageVO.getReceiverId());
                }
            }
            // 群聊：推送给所有在线群成员
            else if (messageVO.getConversationType() == 2 && messageVO.getGroupId() != null) {
                // 查询群成员列表
                LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(GroupMember::getGroupId, messageVO.getGroupId());
                List<GroupMember> groupMembers = groupMemberMapper.selectList(wrapper);

                int onlineCount = 0;
                for (GroupMember member : groupMembers) {
                    // 推送给所有在线成员（包括发送者）
                    WebSocketSession memberSession = ONLINE_USERS.get(member.getUserId());
                    if (memberSession != null && memberSession.isOpen()) {
                        try {
                            memberSession.sendMessage(textMessage);
                            onlineCount++;
                        } catch (IOException e) {
                            log.error("推送消息给群成员 {} 失败", member.getUserId(), e);
                        }
                    }
                }
                log.info("群聊消息已推送，群组ID: {}，在线成员数: {}/{}",
                         messageVO.getGroupId(), onlineCount, groupMembers.size());
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
