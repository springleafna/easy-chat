package com.springleaf.easychat.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 活跃会话 DTO
 * 客户端通知服务端当前活跃的会话ID
 */
@Data
public class ActiveChatDTO {

    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String conversationId;
}
