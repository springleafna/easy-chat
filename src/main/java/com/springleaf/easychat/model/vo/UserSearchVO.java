package com.springleaf.easychat.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/**
 * 用户搜索结果响应
 */
@Data
public class UserSearchVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 账号
     */
    private String account;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 手机号（部分隐藏）
     */
    private String phone;

    /**
     * 邮箱（部分隐藏）
     */
    private String email;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 性别：0-未知，1-男，2-女
     */
    private Integer gender;

    /**
     * 生日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    /**
     * 个性签名
     */
    private String signature;

    /**
     * 是否已是好友
     */
    private Boolean isFriend;

    /**
     * 好友状态：0-已删除，1-正常，2-黑名单（仅当isFriend为true时有值）
     */
    private Integer friendStatus;

    /**
     * 好友备注名（仅当isFriend为true时有值）
     */
    private String remarkName;
}
