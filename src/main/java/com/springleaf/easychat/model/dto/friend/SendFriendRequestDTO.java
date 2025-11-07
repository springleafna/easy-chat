package com.springleaf.easychat.model.dto.friend;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 发送好友申请请求
 */
@Data
public class SendFriendRequestDTO {

    /**
     * 目标用户ID
     */
    @NotNull(message = "目标用户ID不能为空")
    private Long targetId;

    /**
     * 申请备注（如：我是xxx）
     */
    @Length(max = 200, message = "申请备注不能超过200个字符")
    private String applyMessage;
}
