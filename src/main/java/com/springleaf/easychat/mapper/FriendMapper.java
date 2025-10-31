package com.springleaf.easychat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springleaf.easychat.model.entity.Friend;
import org.apache.ibatis.annotations.Mapper;

/**
 * 好友Mapper接口
 */
@Mapper
public interface FriendMapper extends BaseMapper<Friend> {

    // 待实现自定义SQL方法
}
