package com.springleaf.easychat.model.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 用户信息更新请求
 */
@Data
public class UserUpdateRequest {

    /**
     * 昵称（必填）
     */
    @NotBlank(message = "昵称不能为空")
    @Size(min = 1, max = 100, message = "昵称长度为1-100个字符")
    private String nickname;

    /**
     * 邮箱（选填）
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 头像URL（选填）
     */
    private String avatarUrl;

    /**
     * 地区（选填）
     */
    private String region;

    /**
     * 性别：0-未知，1-男，2-女（选填）
     */
    private Integer gender;

    /**
     * 生日（选填）
     */
    private LocalDate birthday;

    /**
     * 个性签名（选填）
     */
    @Size(max = 200, message = "个性签名最多200个字符")
    private String signature;
}
