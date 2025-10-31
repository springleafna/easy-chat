package com.springleaf.easychat.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话实体类
 */
@Data
@TableName("conversations")
public class Conversation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    @TableId(type = IdType.AUTO)
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
     * 用户ID
     */
    private Long userId;

    /**
     * 未读消息数
     */
    private Integer unreadCount;

    /**
     * 最后一条消息ID
     */
    private Long lastMessageId;

    /**
     * 最后消息时间
     */
    private LocalDateTime lastMessageTime;

    /**
     * 会话状态：0-已删除，1-正常
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
