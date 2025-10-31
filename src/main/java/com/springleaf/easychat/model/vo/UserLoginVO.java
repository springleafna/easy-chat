package com.springleaf.easychat.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户登录响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * token
     */
    private String token;

    /**
     * 用户信息
     */
    private UserVO userInfo;
}
