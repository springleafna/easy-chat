package com.springleaf.easychat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.easychat.enums.FriendRequestStatusEnum;
import com.springleaf.easychat.enums.FriendStatusEnum;
import com.springleaf.easychat.enums.ResultCodeEnum;
import com.springleaf.easychat.exception.BusinessException;
import com.springleaf.easychat.mapper.FriendMapper;
import com.springleaf.easychat.mapper.FriendRequestMapper;
import com.springleaf.easychat.model.dto.friend.HandleFriendRequestDTO;
import com.springleaf.easychat.model.dto.friend.SendFriendRequestDTO;
import com.springleaf.easychat.model.entity.Friend;
import com.springleaf.easychat.model.entity.FriendRequest;
import com.springleaf.easychat.model.entity.User;
import com.springleaf.easychat.model.vo.FriendRequestVO;
import com.springleaf.easychat.service.FriendRequestService;
import com.springleaf.easychat.service.UserService;
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
 * 好友申请服务实现类
 */
@Slf4j
@Service
public class FriendRequestServiceImpl extends ServiceImpl<FriendRequestMapper, FriendRequest> implements FriendRequestService {

    @Resource
    private UserService userService;

    @Resource
    private FriendMapper friendMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendFriendRequest(SendFriendRequestDTO request) {
        // 1. 获取当前登录用户ID
        Long currentUserId = UserContextUtil.getCurrentUserId();
        Long targetId = request.getTargetId();

        // 2. 验证不能向自己发送申请
        if (currentUserId.equals(targetId)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "不能向自己发送好友申请");
        }

        // 3. 验证目标用户是否存在
        User targetUser = userService.getById(targetId);
        if (targetUser == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST, "目标用户不存在");
        }

        // 4. 检查是否已经是好友关系
        LambdaQueryWrapper<Friend> friendQueryWrapper = new LambdaQueryWrapper<>();
        friendQueryWrapper.eq(Friend::getUserId, currentUserId)
                .eq(Friend::getFriendId, targetId)
                .eq(Friend::getStatus, FriendStatusEnum.NORMAL.getCode());
        Friend existFriend = friendMapper.selectOne(friendQueryWrapper);

        if (existFriend != null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "对方已经是你的好友");
        }

        // 5. 检查是否已有待处理的申请
        LambdaQueryWrapper<FriendRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FriendRequest::getRequesterId, currentUserId)
                .eq(FriendRequest::getTargetId, targetId)
                .eq(FriendRequest::getStatus, FriendRequestStatusEnum.PENDING.getCode());
        FriendRequest existingRequest = this.getOne(queryWrapper);

        if (existingRequest != null) {
            // throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "已存在待处理的好友申请，请勿重复提交");
            log.info("已存在待处理的好友申请，申请人ID：{}, 目标用户ID：{}", currentUserId, targetId);
            return;
        }

        // 6. 创建好友申请
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setRequesterId(currentUserId);
        friendRequest.setTargetId(targetId);
        friendRequest.setStatus(FriendRequestStatusEnum.PENDING.getCode());
        friendRequest.setApplyMessage(request.getApplyMessage());

        boolean saveResult = this.save(friendRequest);
        if (!saveResult) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED, "发送好友申请失败");
        }

        log.info("发送好友申请成功，申请人ID：{}, 目标用户ID：{}", currentUserId, targetId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleFriendRequest(HandleFriendRequestDTO request) {
        // 1. 获取当前登录用户ID
        Long currentUserId = UserContextUtil.getCurrentUserId();

        // 2. 查询申请记录
        FriendRequest friendRequest = this.getById(request.getRequestId());
        if (friendRequest == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "好友申请不存在");
        }

        // 3. 验证是否有权处理该申请（只有目标用户才能处理）
        if (!friendRequest.getTargetId().equals(currentUserId)) {
            throw new BusinessException("无权处理该好友申请");
        }

        // 4. 验证申请状态（只能处理待处理的申请）
        if (!FriendRequestStatusEnum.PENDING.getCode().equals(friendRequest.getStatus())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "该好友申请已被处理");
        }

        // 5. 处理申请
        if (request.getAccept()) {
            // 同意申请
            friendRequest.setStatus(FriendRequestStatusEnum.ACCEPTED.getCode());

            // 创建双向好友关系
            Long requesterId = friendRequest.getRequesterId();

            // 5.1 创建申请人到目标用户的关系
            Friend friend1 = new Friend();
            friend1.setUserId(requesterId);
            friend1.setFriendId(currentUserId);
            friend1.setStatus(FriendStatusEnum.NORMAL.getCode());
            friendMapper.insert(friend1);

            // 5.2 创建目标用户到申请人的关系
            Friend friend2 = new Friend();
            friend2.setUserId(currentUserId);
            friend2.setFriendId(requesterId);
            friend2.setRemarkName(request.getRemarkName());
            friend2.setStatus(FriendStatusEnum.NORMAL.getCode());
            friendMapper.insert(friend2);

            log.info("同意好友申请成功，申请人ID：{}, 目标用户ID：{}", requesterId, currentUserId);
        } else {
            // 拒绝申请
            friendRequest.setStatus(FriendRequestStatusEnum.REJECTED.getCode());
            friendRequest.setRejectReason(request.getRejectReason());
            log.info("拒绝好友申请成功，申请人ID：{}, 目标用户ID：{}", friendRequest.getRequesterId(), currentUserId);
        }

        // 6. 更新申请状态
        boolean updateResult = this.updateById(friendRequest);
        if (!updateResult) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED, "处理好友申请失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawFriendRequest(Long requestId) {
        // 1. 获取当前登录用户ID
        Long currentUserId = UserContextUtil.getCurrentUserId();

        // 2. 查询申请记录
        FriendRequest friendRequest = this.getById(requestId);
        if (friendRequest == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "好友申请不存在");
        }

        // 3. 验证是否有权撤回（只有申请人才能撤回）
        if (!friendRequest.getRequesterId().equals(currentUserId)) {
            throw new BusinessException("无权撤回该好友申请");
        }

        // 4. 验证申请状态（只能撤回待处理的申请）
        if (!FriendRequestStatusEnum.PENDING.getCode().equals(friendRequest.getStatus())) {
            throw new BusinessException("该好友申请已被处理，无法撤回");
        }

        // 5. 更新申请状态为已撤回
        friendRequest.setStatus(FriendRequestStatusEnum.WITHDRAWN.getCode());
        boolean updateResult = this.updateById(friendRequest);
        if (!updateResult) {
            throw new BusinessException("撤回好友申请失败");
        }

        log.info("撤回好友申请成功，申请ID：{}, 申请人ID：{}", requestId, currentUserId);
    }

    @Override
    public List<FriendRequestVO> getReceivedRequests() {
        // 1. 获取当前登录用户ID
        Long currentUserId = UserContextUtil.getCurrentUserId();

        // 2. 查询收到的好友申请（按时间倒序）
        LambdaQueryWrapper<FriendRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FriendRequest::getTargetId, currentUserId)
                .orderByDesc(FriendRequest::getCreatedAt);
        List<FriendRequest> requestList = this.list(queryWrapper);

        if (requestList.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 获取所有申请人的ID列表
        List<Long> requesterIds = requestList.stream()
                .map(FriendRequest::getRequesterId)
                .collect(Collectors.toList());

        // 4. 批量查询申请人信息
        List<User> requesterUsers = userService.listByIds(requesterIds);
        Map<Long, User> requesterUserMap = requesterUsers.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 5. 组装FriendRequestVO
        List<FriendRequestVO> requestVOList = new ArrayList<>();
        for (FriendRequest friendRequest : requestList) {
            User requester = requesterUserMap.get(friendRequest.getRequesterId());
            if (requester != null) {
                FriendRequestVO requestVO = buildFriendRequestVO(friendRequest, requester);
                requestVOList.add(requestVO);
            }
        }

        log.info("查询收到的好友申请成功，用户ID：{}, 申请数量：{}", currentUserId, requestVOList.size());
        return requestVOList;
    }

    @Override
    public List<FriendRequestVO> getSentRequests() {
        // 1. 获取当前登录用户ID
        Long currentUserId = UserContextUtil.getCurrentUserId();

        // 2. 查询发出的好友申请（按时间倒序）
        LambdaQueryWrapper<FriendRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FriendRequest::getRequesterId, currentUserId)
                .orderByDesc(FriendRequest::getCreatedAt);
        List<FriendRequest> requestList = this.list(queryWrapper);

        if (requestList.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 获取所有目标用户的ID列表
        List<Long> targetIds = requestList.stream()
                .map(FriendRequest::getTargetId)
                .collect(Collectors.toList());

        // 4. 批量查询目标用户信息
        List<User> targetUsers = userService.listByIds(targetIds);
        Map<Long, User> targetUserMap = targetUsers.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 5. 组装FriendRequestVO（这里requester是当前用户，但显示的是target信息）
        List<FriendRequestVO> requestVOList = new ArrayList<>();
        for (FriendRequest friendRequest : requestList) {
            User targetUser = targetUserMap.get(friendRequest.getTargetId());
            if (targetUser != null) {
                FriendRequestVO requestVO = new FriendRequestVO();
                requestVO.setId(friendRequest.getId());
                requestVO.setRequesterId(friendRequest.getRequesterId());
                // 显示目标用户的信息（对方）
                requestVO.setRequesterAccount(targetUser.getAccount());
                requestVO.setRequesterNickname(targetUser.getNickname());
                requestVO.setRequesterAvatarUrl(targetUser.getAvatarUrl());
                requestVO.setTargetId(friendRequest.getTargetId());
                requestVO.setStatus(friendRequest.getStatus());
                requestVO.setStatusDesc(FriendRequestStatusEnum.getByCode(friendRequest.getStatus()).getDesc());
                requestVO.setApplyMessage(friendRequest.getApplyMessage());
                requestVO.setRejectReason(friendRequest.getRejectReason());
                requestVO.setCreatedAt(friendRequest.getCreatedAt());
                requestVO.setUpdatedAt(friendRequest.getUpdatedAt());
                requestVOList.add(requestVO);
            }
        }

        log.info("查询发出的好友申请成功，用户ID：{}, 申请数量：{}", currentUserId, requestVOList.size());
        return requestVOList;
    }

    @Override
    public Long getUnhandledCount() {
        // 获取当前登录用户ID
        Long currentUserId = UserContextUtil.getCurrentUserId();

        // 查询未处理的好友申请数量
        LambdaQueryWrapper<FriendRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FriendRequest::getTargetId, currentUserId)
                .eq(FriendRequest::getStatus, FriendRequestStatusEnum.PENDING.getCode());
        Long count = this.count(queryWrapper);

        log.info("查询未处理好友申请数量，用户ID：{}, 数量：{}", currentUserId, count);
        return count;
    }

    /**
     * 构建FriendRequestVO对象
     */
    private FriendRequestVO buildFriendRequestVO(FriendRequest friendRequest, User requester) {
        FriendRequestVO requestVO = new FriendRequestVO();
        requestVO.setId(friendRequest.getId());
        requestVO.setRequesterId(friendRequest.getRequesterId());
        requestVO.setRequesterAccount(requester.getAccount());
        requestVO.setRequesterNickname(requester.getNickname());
        requestVO.setRequesterAvatarUrl(requester.getAvatarUrl());
        requestVO.setTargetId(friendRequest.getTargetId());
        requestVO.setStatus(friendRequest.getStatus());
        requestVO.setStatusDesc(FriendRequestStatusEnum.getByCode(friendRequest.getStatus()).getDesc());
        requestVO.setApplyMessage(friendRequest.getApplyMessage());
        requestVO.setRejectReason(friendRequest.getRejectReason());
        requestVO.setCreatedAt(friendRequest.getCreatedAt());
        requestVO.setUpdatedAt(friendRequest.getUpdatedAt());
        return requestVO;
    }
}
