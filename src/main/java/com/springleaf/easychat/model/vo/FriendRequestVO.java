package com.springleaf.easychat.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友申请响应
 */
@Data
public class FriendRequestVO {

    /**
     * 申请ID
     */
    private Long id;

    /**
     * 申请人ID
     */
    private Long requesterId;

    /**
     * 申请人账号
     */
    private String requesterAccount;

    /**
     * 申请人昵称
     */
    private String requesterNickname;

    /**
     * 申请人头像URL
     */
    private String requesterAvatarUrl;

    /**
     * 目标用户ID
     */
    private Long targetId;

    /**
     * 状态：0-待处理，1-已同意，2-已拒绝，3-已撤回
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 申请备注
     */
    private String applyMessage;

    /**
     * 拒绝原因
     */
    private String rejectReason;

    /**
     * 申请时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 处理时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
