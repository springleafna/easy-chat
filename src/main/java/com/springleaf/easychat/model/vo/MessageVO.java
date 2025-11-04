package com.springleaf.easychat.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * WebSocket 消息 VO
 */
@Data
public class MessageVO {

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 发送者昵称
     */
    private String senderNickname;

    /**
     * 发送者头像
     */
    private String senderAvatar;

    /**
     * 消息类型：1-文本，2-图片，3-语音，4-视频，5-文件，6-位置，7-系统消息
     */
    private Integer messageType;

    /**
     * 会话类型：1-单聊，2-群聊
     */
    private Integer conversationType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 媒体文件URL（图片/语音/视频等）
     */
    private String mediaUrl;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Integer fileSize;

    /**
     * 消息状态：0-已撤回，1-正常，2-已删除
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
