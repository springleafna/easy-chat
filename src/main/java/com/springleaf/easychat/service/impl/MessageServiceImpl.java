package com.springleaf.easychat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.easychat.mapper.MessageMapper;
import com.springleaf.easychat.model.entity.Message;
import com.springleaf.easychat.service.MessageService;
import org.springframework.stereotype.Service;

/**
 * 消息服务实现类
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    // 待实现具体业务方法
}
