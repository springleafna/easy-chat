package com.springleaf.easychat.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息实体类
 */
@Data
@TableName("messages")
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 接收者ID（单聊时使用）
     */
    private Long receiverId;

    /**
     * 群组ID（群聊时使用）
     */
    private Long groupId;

    /**
     * 消息类型：1-文本，2-图片，3-语音，4-视频，5-文件，6-位置，7-系统消息
     */
    private Integer messageType;

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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
