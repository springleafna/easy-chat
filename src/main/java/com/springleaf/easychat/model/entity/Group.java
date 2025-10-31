package com.springleaf.easychat.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 群组实体类
 */
@Data
@TableName("groups")
public class Group implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 群组ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 群名称
     */
    private String groupName;

    /**
     * 群主ID
     */
    private Long ownerId;

    /**
     * 群头像URL
     */
    private String avatarUrl;

    /**
     * 群公告
     */
    private String announcement;

    /**
     * 最大成员数
     */
    private Integer maxMembers;

    /**
     * 群状态：0-已解散，1-正常
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
