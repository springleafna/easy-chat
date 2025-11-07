package com.springleaf.easychat.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 群组成员实体类
 */
@Data
@TableName("group_members")
public class GroupMember {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 群组ID
     */
    private Long groupId;

    /**
     * 成员ID
     */
    private Long userId;

    /**
     * 邀请人ID（谁拉你进群的，创建群时为NULL）
     */
    private Long inviterId;

    /**
     * 群内昵称
     */
    private String nickname;

    /**
     * 成员角色：1-普通成员，2-管理员，3-群主
     */
    private Integer role;

    /**
     * 成员状态：0-已退出，1-正常
     */
    private Integer status;

    /**
     * 加入时间
     */
    private LocalDateTime joinedAt;
}
