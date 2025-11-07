package com.springleaf.easychat.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 搜索用户请求
 */
@Data
public class SearchUserRequest {

    /**
     * 搜索关键词（手机号/账号/邮箱）
     */
    @NotBlank(message = "搜索关键词不能为空")
    private String keyword;
}
