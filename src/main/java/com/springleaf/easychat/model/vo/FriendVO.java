package com.springleaf.easychat.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 好友信息响应
 */
@Data
public class FriendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 好友关系ID
     */
    private Long id;

    /**
     * 好友ID
     */
    private Long friendId;

    /**
     * 好友账号
     */
    private String account;

    /**
     * 好友昵称
     */
    private String nickname;

    /**
     * 好友备注名
     */
    private String remarkName;

    /**
     * 好友手机号
     */
    private String phone;

    /**
     * 好友邮箱
     */
    private String email;

    /**
     * 好友头像URL
     */
    private String avatarUrl;

    /**
     * 好友性别：0-未知，1-男，2-女
     */
    private Integer gender;

    /**
     * 好友生日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    /**
     * 好友个性签名
     */
    private String signature;

    /**
     * 关系状态：0-已删除，1-正常，2-黑名单
     */
    private Integer status;

    /**
     * 添加好友时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
