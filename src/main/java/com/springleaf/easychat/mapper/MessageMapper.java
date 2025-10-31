package com.springleaf.easychat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springleaf.easychat.model.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息Mapper接口
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    // 待实现自定义SQL方法
}
