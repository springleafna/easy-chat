package com.springleaf.easychat.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友申请实体类
 */
@Data
@TableName("friend_requests")
public class FriendRequest {

    /**
     * 申请ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 申请人ID（发起好友申请的用户）
     */
    private Long requesterId;

    /**
     * 目标用户ID（被申请添加为好友的用户）
     */
    private Long targetId;

    /**
     * 状态：0-待处理，1-已同意，2-已拒绝，3-已撤回
     */
    private Integer status;

    /**
     * 申请备注（如：我是xxx）
     */
    private String applyMessage;

    /**
     * 拒绝原因（可选）
     */
    private String rejectReason;

    /**
     * 申请时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 处理时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
