package com.springleaf.easychat.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginVO {

    /**
     * token
     */
    private String token;

    /**
     * 用户信息
     */
    private UserVO userInfo;
}
