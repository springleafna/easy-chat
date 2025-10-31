package com.springleaf.easychat.utils;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 用户上下文工具类
 */
public class UserContextUtil {

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     */
    public static Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 判断是否已登录
     *
     * @return 是否已登录
     */
    public static boolean isLogin() {
        return StpUtil.isLogin();
    }
}
