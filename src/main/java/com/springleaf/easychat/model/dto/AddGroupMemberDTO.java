package com.springleaf.easychat.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 添加群成员请求 DTO
 */
@Data
public class AddGroupMemberDTO {

    /**
     * 群组ID
     */
    @NotNull(message = "群组ID不能为空")
    private Long groupId;

    /**
     * 要添加的用户ID列表
     */
    @NotEmpty(message = "成员ID列表不能为空")
    private List<Long> userIds;
}
