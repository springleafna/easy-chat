package com.springleaf.easychat.model.dto.friend;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 删除好友请求
 */
@Data
public class DeleteFriendRequest {

    /**
     * 好友ID
     */
    @NotNull(message = "好友ID不能为空")
    private Long friendId;
}
