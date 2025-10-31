package com.springleaf.easychat.controller;

import com.springleaf.easychat.common.Result;
import com.springleaf.easychat.model.dto.friend.AddFriendRequest;
import com.springleaf.easychat.model.dto.friend.DeleteFriendRequest;
import com.springleaf.easychat.model.vo.FriendVO;
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

    /**
     * 添加好友
     *
     * @param request 添加好友请求
     * @return 是否成功
     */
    @PostMapping("/add")
    public Result<Boolean> addFriend(@Valid @RequestBody AddFriendRequest request) {
        Boolean result = friendService.addFriend(request);
        return Result.success(result);
    }

    /**
     * 删除好友
     *
     * @param request 删除好友请求
     * @return 是否成功
     */
    @DeleteMapping("/delete")
    public Result<Boolean> deleteFriend(@Valid @RequestBody DeleteFriendRequest request) {
        Boolean result = friendService.deleteFriend(request);
        return Result.success(result);
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
}
