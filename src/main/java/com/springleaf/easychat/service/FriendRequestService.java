package com.springleaf.easychat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.springleaf.easychat.model.dto.friend.HandleFriendRequestDTO;
import com.springleaf.easychat.model.dto.friend.SendFriendRequestDTO;
import com.springleaf.easychat.model.entity.FriendRequest;
import com.springleaf.easychat.model.vo.FriendRequestVO;

import java.util.List;

/**
 * 好友申请服务接口
 */
public interface FriendRequestService extends IService<FriendRequest> {

    /**
     * 发送好友申请
     *
     * @param request 申请请求
     */
    void sendFriendRequest(SendFriendRequestDTO request);

    /**
     * 处理好友申请（同意或拒绝）
     *
     * @param request 处理请求
     */
    void handleFriendRequest(HandleFriendRequestDTO request);

    /**
     * 撤回好友申请
     *
     * @param requestId 申请ID
     */
    void withdrawFriendRequest(Long requestId);

    /**
     * 获取我收到的好友申请列表
     *
     * @return 申请列表
     */
    List<FriendRequestVO> getReceivedRequests();

    /**
     * 获取我发出的好友申请列表
     *
     * @return 申请列表
     */
    List<FriendRequestVO> getSentRequests();

    /**
     * 获取未处理的好友申请数量
     *
     * @return 未处理数量
     */
    Long getUnhandledCount();
}
