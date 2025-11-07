package com.springleaf.easychat.controller;

import com.springleaf.easychat.common.Result;
import com.springleaf.easychat.model.dto.friend.AddFriendRequest;
import com.springleaf.easychat.model.dto.friend.DeleteFriendRequest;
import com.springleaf.easychat.model.dto.friend.HandleFriendRequestDTO;
import com.springleaf.easychat.model.dto.friend.SendFriendRequestDTO;
import com.springleaf.easychat.model.vo.FriendRequestVO;
import com.springleaf.easychat.model.vo.FriendVO;
import com.springleaf.easychat.service.FriendRequestService;
import com.springleaf.easychat.service.FriendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 好友控制器
 */
@Slf4j
@RestController
@RequestMapping("/friend")
public class FriendController {

    @Resource
    private FriendService friendService;

    @Resource
    private FriendRequestService friendRequestService;

    /**
     * 添加好友（已废弃，请使用好友申请流程）
     * @deprecated 使用 POST /friend/request/send 代替
     */
    @Deprecated
    @PostMapping("/add")
    public Result<Void> addFriend(@Valid @RequestBody AddFriendRequest request) {
        friendService.addFriend(request);
        return Result.success();
    }

    /**
     * 删除好友
     *
     * @param request 删除好友请求
     */
    @DeleteMapping("/delete")
    public Result<Void> deleteFriend(@Valid @RequestBody DeleteFriendRequest request) {
        friendService.deleteFriend(request);
        return Result.success();
    }

    /**
     * 获取好友列表
     *
     * @return 好友列表
     */
    @GetMapping("/list")
    public Result<List<FriendVO>> getFriendList() {
        List<FriendVO> friendList = friendService.getFriendList();
        return Result.success(friendList);
    }

    /**
     * 发送好友申请
     *
     * @param request 申请请求
     */
    @PostMapping("/request/send")
    public Result<Void> sendFriendRequest(@Valid @RequestBody SendFriendRequestDTO request) {
        friendRequestService.sendFriendRequest(request);
        return Result.success();
    }

    /**
     * 处理好友申请（同意或拒绝）
     *
     * @param request 处理请求
     */
    @PostMapping("/request/handle")
    public Result<Void> handleFriendRequest(@Valid @RequestBody HandleFriendRequestDTO request) {
        friendRequestService.handleFriendRequest(request);
        return Result.success();
    }

    /**
     * 撤回好友申请
     *
     * @param requestId 申请ID
     */
    @DeleteMapping("/request/withdraw/{requestId}")
    public Result<Void> withdrawFriendRequest(@PathVariable Long requestId) {
        friendRequestService.withdrawFriendRequest(requestId);
        return Result.success();
    }

    /**
     * 获取我收到的好友申请列表
     *
     * @return 申请列表
     */
    @GetMapping("/request/received")
    public Result<List<FriendRequestVO>> getReceivedRequests() {
        List<FriendRequestVO> requestList = friendRequestService.getReceivedRequests();
        return Result.success(requestList);
    }

    /**
     * 获取我发出的好友申请列表
     *
     * @return 申请列表
     */
    @GetMapping("/request/sent")
    public Result<List<FriendRequestVO>> getSentRequests() {
        List<FriendRequestVO> requestList = friendRequestService.getSentRequests();
        return Result.success(requestList);
    }

    /**
     * 获取未处理的好友申请数量
     *
     * @return 未处理数量
     */
    @GetMapping("/request/unhandled/count")
    public Result<Long> getUnhandledCount() {
        Long count = friendRequestService.getUnhandledCount();
        return Result.success(count);
    }
}
