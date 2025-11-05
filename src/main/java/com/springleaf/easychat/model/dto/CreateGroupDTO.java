package com.springleaf.easychat.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建群组请求 DTO
 */
@Data
public class CreateGroupDTO {

    /**
     * 群名称
     */
    @NotBlank(message = "群名称不能为空")
    @Size(max = 50, message = "群名称长度不能超过50个字符")
    private String groupName;

    /**
     * 群头像URL
     */
    private String avatarUrl;

    /**
     * 群公告
     */
    @Size(max = 500, message = "群公告长度不能超过500个字符")
    private String announcement;

    /**
     * 最大成员数（默认500）
     */
    private Integer maxMembers;

    /**
     * 初始成员ID列表（不包括创建者自己）
     */
    @NotEmpty(message = "至少需要添加一个群成员")
    private List<Long> memberIds;
}
