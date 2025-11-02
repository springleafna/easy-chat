package com.springleaf.easychat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.springleaf.easychat.model.dto.user.UserLoginRequest;
import com.springleaf.easychat.model.dto.user.UserRegisterRequest;
import com.springleaf.easychat.model.dto.user.UserUpdateRequest;
import com.springleaf.easychat.model.entity.User;
import com.springleaf.easychat.model.vo.UserLoginVO;
import com.springleaf.easychat.model.vo.UserVO;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param request 注册请求
     */
    void register(UserRegisterRequest request);

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应（包含token和用户信息）
     */
    UserLoginVO login(UserLoginRequest request);

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息
     */
    UserVO getCurrentUserInfo();

    /**
     * 更新用户信息
     *
     * @param request 更新请求
     * @return 是否成功
     */
    Boolean updateUserInfo(UserUpdateRequest request);

    /**
     * 根据用户ID获取用户VO
     *
     * @param userId 用户ID
     * @return 用户VO
     */
    UserVO getUserVOById(Long userId);

    /**
     * 退出登录
     */
    void logout();
}
