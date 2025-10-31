package com.springleaf.easychat.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话列表响应
 */
@Data
public class ConversationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    private Long id;

    /**
     * 会话类型：1-单聊，2-群聊
     */
    private Integer type;

    /**
     * 目标ID（单聊为对方user_id，群聊为group_id）
     */
    private Long targetId;

    /**
     * 会话名称（单聊显示对方昵称或备注名，群聊显示群名称）
     */
    private String conversationName;

    /**
     * 会话头像（单聊显示对方头像，群聊显示群头像）
     */
    private String avatarUrl;

    /**
     * 未读消息数
     */
    private Integer unreadCount;

    /**
     * 最后一条消息ID
     */
    private Long lastMessageId;

    /**
     * 最后一条消息内容
     */
    private String lastMessageContent;

    /**
     * 最后消息时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMessageTime;

    /**
     * 会话状态：0-已删除，1-正常
     */
    private Integer status;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
