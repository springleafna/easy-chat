package com.springleaf.easychat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springleaf.easychat.model.entity.Group;
import org.apache.ibatis.annotations.Mapper;

/**
 * 群组Mapper接口
 */
@Mapper
public interface GroupMapper extends BaseMapper<Group> {

    // 待实现自定义SQL方法
}
