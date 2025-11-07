package com.springleaf.easychat.model.dto.friend;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 处理好友申请请求
 */
@Data
public class HandleFriendRequestDTO {

    /**
     * 申请ID
     */
    @NotNull(message = "申请ID不能为空")
    private Long requestId;

    /**
     * 是否同意（true-同意，false-拒绝）
     */
    @NotNull(message = "处理结果不能为空")
    private Boolean accept;

    /**
     * 拒绝原因（拒绝时可填写）
     */
    @Length(max = 200, message = "拒绝原因不能超过200个字符")
    private String rejectReason;

    /**
     * 好友备注名（同意时可填写）
     */
    @Length(max = 100, message = "好友备注名不能超过100个字符")
    private String remarkName;
}
