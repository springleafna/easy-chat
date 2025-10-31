package com.springleaf.easychat.model.dto.friend;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 删除好友请求
 */
@Data
public class DeleteFriendRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 好友ID
     */
    @NotNull(message = "好友ID不能为空")
    private Long friendId;
}
