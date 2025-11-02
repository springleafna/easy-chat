package com.springleaf.easychat.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.easychat.enums.GenderEnum;
import com.springleaf.easychat.enums.ResultCodeEnum;
import com.springleaf.easychat.enums.UserStatusEnum;
import com.springleaf.easychat.exception.BusinessException;
import com.springleaf.easychat.mapper.UserMapper;
import com.springleaf.easychat.model.dto.user.UserLoginRequest;
import com.springleaf.easychat.model.dto.user.UserRegisterRequest;
import com.springleaf.easychat.model.dto.user.UserUpdateRequest;
import com.springleaf.easychat.model.entity.User;
import com.springleaf.easychat.model.vo.UserLoginVO;
import com.springleaf.easychat.model.vo.UserVO;
import com.springleaf.easychat.service.UserService;
import com.springleaf.easychat.utils.AccountUtil;
import com.springleaf.easychat.utils.BeanCopyUtil;
import com.springleaf.easychat.utils.PasswordUtil;
import com.springleaf.easychat.utils.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterRequest request) {
        // 1. 检查手机号是否已被注册
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, request.getPhone());
        User existUser = this.getOne(queryWrapper);
        if (existUser != null) {
            throw new BusinessException(ResultCodeEnum.USER_ALREADY_EXIST, "该手机号已被注册");
        }

        // 2. 生成唯一账号
        String account = AccountUtil.generateAccount();

        // 3. 加密密码
        String encryptedPassword = PasswordUtil.encryptPassword(request.getPassword());

        // 4. 创建用户对象
        User user = new User();
        user.setAccount(account);
        user.setPhone(request.getPhone());
        user.setPassword(encryptedPassword);
        user.setNickname(request.getNickName());
        user.setStatus(UserStatusEnum.NORMAL.getCode());
        user.setGender(GenderEnum.UNKNOWN.getCode()); // 默认性别：未知
        user.setSignature(""); // 默认签名为空

        // 5. 保存到数据库
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED, "注册失败");
        }

        log.info("用户注册成功，账号：{}, 手机号：{}", account, request.getPhone());
    }

    @Override
    public UserLoginVO login(UserLoginRequest request) {
        // 1. 根据手机号查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, request.getPhone());
        User user = this.getOne(queryWrapper);

        // 2. 验证用户是否存在
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST);
        }

        // 3. 验证用户状态
        if (UserStatusEnum.DISABLED.getCode().equals(user.getStatus())) {
            throw new BusinessException(ResultCodeEnum.USER_DISABLED);
        }
        if (UserStatusEnum.DELETED.getCode().equals(user.getStatus())) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST, "该用户已注销");
        }

        // 4. 验证密码
        if (!PasswordUtil.verifyPassword(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCodeEnum.PASSWORD_ERROR);
        }

        // 5. 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        this.updateById(user);

        // 6. 使用Sa-Token进行登录，保存用户ID到session
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        // 7. 构建返回数据
        UserVO userVO = getUserVOById(user.getId());
        UserLoginVO loginVO = new UserLoginVO(token, userVO);

        log.info("用户登录成功，账号：{}, 手机号：{}", user.getAccount(), user.getPhone());
        return loginVO;
    }

    @Override
    public UserVO getCurrentUserInfo() {
        // 获取当前登录用户ID
        Long userId = UserContextUtil.getCurrentUserId();
        return getUserVOById(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateUserInfo(UserUpdateRequest request) {
        // 1. 获取当前登录用户ID
        Long userId = UserContextUtil.getCurrentUserId();

        // 2. 查询用户是否存在
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST);
        }

        // 3. 如果更新邮箱，检查邮箱是否已被其他用户使用
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getEmail, request.getEmail())
                    .ne(User::getId, userId);
            User existUser = this.getOne(queryWrapper);
            if (existUser != null) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "该邮箱已被使用");
            }
        }

        // 4. 更新用户信息
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setGender(request.getGender());
        user.setBirthday(request.getBirthday());
        user.setSignature(request.getSignature());

        // 5. 保存更新
        boolean updateResult = this.updateById(user);
        if (!updateResult) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED, "更新用户信息失败");
        }

        log.info("用户信息更新成功，用户ID：{}", userId);
        return true;
    }

    @Override
    public UserVO getUserVOById(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST);
        }
        return BeanCopyUtil.copy(user, UserVO.class);
    }

    @Override
    public void logout() {
        // 获取当前登录用户ID
        Long userId = UserContextUtil.getCurrentUserId();

        // 使用Sa-Token退出登录
        StpUtil.logout();

        log.info("用户退出登录，用户ID：{}", userId);
    }
}
