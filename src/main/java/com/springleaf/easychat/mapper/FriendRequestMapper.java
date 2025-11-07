package com.springleaf.easychat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springleaf.easychat.model.entity.FriendRequest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 好友申请Mapper接口
 */
@Mapper
public interface FriendRequestMapper extends BaseMapper<FriendRequest> {

}
