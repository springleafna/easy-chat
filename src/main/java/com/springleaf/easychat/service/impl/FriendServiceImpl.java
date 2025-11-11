package com.springleaf.easychat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.easychat.enums.FriendStatusEnum;
import com.springleaf.easychat.enums.ResultCodeEnum;
import com.springleaf.easychat.exception.BusinessException;
import com.springleaf.easychat.mapper.FriendMapper;
import com.springleaf.easychat.model.dto.friend.AddFriendRequest;
import com.springleaf.easychat.model.dto.friend.DeleteFriendRequest;
import com.springleaf.easychat.model.entity.Friend;
import com.springleaf.easychat.model.entity.User;
import com.springleaf.easychat.model.vo.FriendVO;
import com.springleaf.easychat.service.FriendService;
import com.springleaf.easychat.service.UserService;
import com.springleaf.easychat.utils.BeanCopyUtil;
import com.springleaf.easychat.utils.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 好友服务实现类
 */
@Slf4j
@Service
public class FriendServiceImpl extends ServiceImpl<FriendMapper, Friend> implements FriendService {

    @Resource
    private UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addFriend(AddFriendRequest request) {
        // 1. 获取当前登录用户ID
        Long userId = UserContextUtil.getCurrentUserId();
        Long friendId = request.getFriendId();

        // 2. 验证不能添加自己为好友
        if (userId.equals(friendId)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "不能添加自己为好友");
        }

        // 3. 验证好友用户是否存在
        User friendUser = userService.getById(friendId);
        if (friendUser == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST, "要添加的好友不存在");
        }

        // 4. 检查是否已经是好友关系
        LambdaQueryWrapper<Friend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId);
        Friend existFriend = this.getOne(queryWrapper);

        if (existFriend != null) {
            // 如果已经是好友关系
            if (FriendStatusEnum.NORMAL.getCode().equals(existFriend.getStatus())) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "对方已经是你的好友");
            } else if (FriendStatusEnum.DELETED.getCode().equals(existFriend.getStatus())) {
                // 如果之前删除过，恢复好友关系
                existFriend.setStatus(FriendStatusEnum.NORMAL.getCode());
                existFriend.setRemarkName(request.getRemarkName());
                this.updateById(existFriend);
                log.info("恢复好友关系成功，用户ID：{}, 好友ID：{}", userId, friendId);
                return;
            } else if (FriendStatusEnum.BLACKLIST.getCode().equals(existFriend.getStatus())) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "对方在你的黑名单中");
            }
        }

        // 5. 创建好友关系（双向关系）
        // 5.1 创建当前用户到好友的关系
        Friend friend = new Friend();
        friend.setUserId(userId);
        friend.setFriendId(friendId);
        friend.setRemarkName(request.getRemarkName());
        friend.setStatus(FriendStatusEnum.NORMAL.getCode());
        this.save(friend);

        // 5.2 创建好友到当前用户的关系
        Friend reverseFriend = new Friend();
        reverseFriend.setUserId(friendId);
        reverseFriend.setFriendId(userId);
        reverseFriend.setStatus(FriendStatusEnum.NORMAL.getCode());

        // 检查反向关系是否存在
        LambdaQueryWrapper<Friend> reverseQueryWrapper = new LambdaQueryWrapper<>();
        reverseQueryWrapper.eq(Friend::getUserId, friendId)
                .eq(Friend::getFriendId, userId);
        Friend existReverseFriend = this.getOne(reverseQueryWrapper);

        if (existReverseFriend != null) {
            // 如果反向关系存在但被删除，则恢复
            if (FriendStatusEnum.DELETED.getCode().equals(existReverseFriend.getStatus())) {
                existReverseFriend.setStatus(FriendStatusEnum.NORMAL.getCode());
                this.updateById(existReverseFriend);
            }
        } else {
            // 反向关系不存在，则创建
            this.save(reverseFriend);
        }

        log.info("添加好友成功，用户ID：{}, 好友ID：{}", userId, friendId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFriend(DeleteFriendRequest request) {
        // 1. 获取当前登录用户ID
        Long userId = UserContextUtil.getCurrentUserId();
        Long friendId = request.getFriendId();

        // 2. 查询好友关系是否存在
        LambdaQueryWrapper<Friend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId)
                .eq(Friend::getStatus, FriendStatusEnum.NORMAL.getCode());
        Friend friend = this.getOne(queryWrapper);

        if (friend == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "好友关系不存在");
        }

        // 3. 删除好友关系（软删除，修改状态为已删除）
        friend.setStatus(FriendStatusEnum.DELETED.getCode());
        this.updateById(friend);

        // 4. 同时删除反向好友关系
        LambdaQueryWrapper<Friend> reverseQueryWrapper = new LambdaQueryWrapper<>();
        reverseQueryWrapper.eq(Friend::getUserId, friendId)
                .eq(Friend::getFriendId, userId)
                .eq(Friend::getStatus, FriendStatusEnum.NORMAL.getCode());
        Friend reverseFriend = this.getOne(reverseQueryWrapper);

        if (reverseFriend != null) {
            reverseFriend.setStatus(FriendStatusEnum.DELETED.getCode());
            this.updateById(reverseFriend);
        }

        log.info("删除好友成功，用户ID：{}, 好友ID：{}", userId, friendId);
    }

    @Override
    public List<FriendVO> getFriendList() {
        // 1. 获取当前登录用户ID
        Long userId = UserContextUtil.getCurrentUserId();

        // 2. 查询当前用户的所有正常状态的好友关系
        LambdaQueryWrapper<Friend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Friend::getUserId, userId)
                .eq(Friend::getStatus, FriendStatusEnum.NORMAL.getCode())
                .orderByDesc(Friend::getCreatedAt);
        List<Friend> friendList = this.list(queryWrapper);

        if (friendList.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 获取所有好友的ID列表
        List<Long> friendIds = friendList.stream()
                .map(Friend::getFriendId)
                .collect(Collectors.toList());

        // 4. 批量查询好友用户信息
        List<User> friendUsers = userService.listByIds(friendIds);
        Map<Long, User> friendUserMap = friendUsers.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 5. 组装FriendVO
        List<FriendVO> friendVOList = new ArrayList<>();
        for (Friend friend : friendList) {
            User friendUser = friendUserMap.get(friend.getFriendId());
            if (friendUser != null) {
                FriendVO friendVO = new FriendVO();
                friendVO.setId(friend.getId());
                friendVO.setFriendId(friendUser.getId());
                friendVO.setAccount(friendUser.getAccount());
                friendVO.setNickname(friendUser.getNickname());
                friendVO.setRemarkName(friend.getRemarkName());
                friendVO.setPhone(friendUser.getPhone());
                friendVO.setEmail(friendUser.getEmail());
                friendVO.setAvatarUrl(friendUser.getAvatarUrl());
                friendVO.setRegion(friendUser.getRegion());
                friendVO.setGender(friendUser.getGender());
                friendVO.setBirthday(friendUser.getBirthday());
                friendVO.setSignature(friendUser.getSignature());
                friendVO.setStatus(friend.getStatus());
                friendVO.setCreatedAt(friend.getCreatedAt());
                friendVOList.add(friendVO);
            }
        }

        log.info("查询好友列表成功，用户ID：{}, 好友数量：{}", userId, friendVOList.size());
        return friendVOList;
    }

    @Override
    public FriendVO getFriendInfo(Long id) {
        // 1. 获取当前登录用户ID
        Long userId = UserContextUtil.getCurrentUserId();
        // 2. 查询好友关系是否存在
        LambdaQueryWrapper<Friend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, id);
        Friend friend = this.getOne(queryWrapper);
        if (friend == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "好友关系不存在");
        }
        // 3. 查询好友用户信息
        User friendUser = userService.getById(id);
        if (friendUser == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "好友用户不存在");
        }
        // 4. 组装FriendVO
        FriendVO friendVO = new FriendVO();
        friendVO.setId(friend.getId());
        friendVO.setFriendId(friendUser.getId());
        friendVO.setAccount(friendUser.getAccount());
        friendVO.setNickname(friendUser.getNickname());
        friendVO.setRemarkName(friend.getRemarkName());
        friendVO.setPhone(friendUser.getPhone());
        friendVO.setEmail(friendUser.getEmail());
        friendVO.setAvatarUrl(friendUser.getAvatarUrl());
        friendVO.setRegion(friendUser.getRegion());
        friendVO.setGender(friendUser.getGender());
        friendVO.setBirthday(friendUser.getBirthday());
        friendVO.setSignature(friendUser.getSignature());
        friendVO.setStatus(friend.getStatus());
        friendVO.setCreatedAt(friend.getCreatedAt());
        log.info("查询好友信息成功，用户ID：{}, 好友ID：{}", userId, id);
        return friendVO;
    }
}
