package com.springleaf.easychat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springleaf.easychat.model.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话Mapper接口
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    // 待实现自定义SQL方法
}
