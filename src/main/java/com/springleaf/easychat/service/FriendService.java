package com.springleaf.easychat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.springleaf.easychat.model.dto.friend.AddFriendRequest;
import com.springleaf.easychat.model.dto.friend.DeleteFriendRequest;
import com.springleaf.easychat.model.entity.Friend;
import com.springleaf.easychat.model.vo.FriendVO;

import java.util.List;

/**
 * 好友服务接口
 */
public interface FriendService extends IService<Friend> {

    /**
     * 添加好友
     *
     * @param request 添加好友请求
     * @return 是否成功
     */
    Boolean addFriend(AddFriendRequest request);

    /**
     * 删除好友
     *
     * @param request 删除好友请求
     * @return 是否成功
     */
    Boolean deleteFriend(DeleteFriendRequest request);

    /**
     * 获取好友列表
     *
     * @return 好友列表
     */
    List<FriendVO> getFriendList();
}
