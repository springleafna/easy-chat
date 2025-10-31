package com.springleaf.easychat.controller;

import com.springleaf.easychat.common.Result;
import com.springleaf.easychat.model.dto.user.UserLoginRequest;
import com.springleaf.easychat.model.dto.user.UserRegisterRequest;
import com.springleaf.easychat.model.dto.user.UserUpdateRequest;
import com.springleaf.easychat.model.vo.UserLoginVO;
import com.springleaf.easychat.model.vo.UserVO;
import com.springleaf.easychat.service.UserService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 用户ID
     */
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody UserRegisterRequest request) {
        Long userId = userService.register(request);
        return Result.success(userId);
    }

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应（token和用户信息）
     */
    @PostMapping("/login")
    public Result<UserLoginVO> login(@Valid @RequestBody UserLoginRequest request) {
        UserLoginVO loginVO = userService.login(request);
        return Result.success(loginVO);
    }

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息
     */
    @GetMapping("/info")
    public Result<UserVO> getCurrentUserInfo() {
        UserVO userVO = userService.getCurrentUserInfo();
        return Result.success(userVO);
    }

    /**
     * 更新用户信息
     *
     * @param request 更新请求
     * @return 是否成功
     */
    @PutMapping("/update")
    public Result<Boolean> updateUserInfo(@Valid @RequestBody UserUpdateRequest request) {
        Boolean result = userService.updateUserInfo(request);
        return Result.success(result);
    }

    /**
     * 退出登录
     *
     * @return 是否成功
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.success();
    }
}
