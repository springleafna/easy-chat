package com.springleaf.easychat.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 消息历史查询 DTO
 */
@Data
public class MessageHistoryDTO {

    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String conversationId;

    /**
     * 每页大小（默认20条）
     */
    @Min(value = 1, message = "每页大小必须大于0")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer size = 20;

    /**
     * 最后一条消息ID（用于游标分页）
     * 如果提供此参数，则查询比此ID更早的消息
     * 如果为空，则查询最新的消息
     */
    private Long lastMessageId;
}
