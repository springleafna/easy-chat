package com.springleaf.easychat.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * WebSocket 发送消息请求 DTO
 */
@Data
public class SendMessageDTO {

    /**
     * 消息类型：1-文本，2-图片，3-语音，4-视频，5-文件，6-位置，7-系统消息
     */
    @NotNull(message = "消息类型不能为空")
    private Integer messageType;

    /**
     * 会话类型：1-单聊，2-群聊
     */
    @NotNull(message = "会话类型不能为空")
    private Integer conversationType;

    /**
     * 接收者ID（单聊时使用）
     */
    private Long receiverId;

    /**
     * 群组ID（群聊时使用）
     */
    private Long groupId;

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
}
