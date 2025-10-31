package com.springleaf.easychat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springleaf.easychat.model.entity.GroupMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * 群组成员Mapper接口
 */
@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {

    // 待实现自定义SQL方法
}
