package com.springleaf.easychat.controller;

import com.springleaf.easychat.common.Result;
import com.springleaf.easychat.model.vo.ConversationVO;
import com.springleaf.easychat.service.ConversationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 会话控制器
 */
@Slf4j
@RestController
@RequestMapping("/conversation")
public class ConversationController {

    @Resource
    private ConversationService conversationService;

    /**
     * 获取会话列表
     *
     * @return 会话列表
     */
    @GetMapping("/list")
    public Result<List<ConversationVO>> getConversationList() {
        List<ConversationVO> conversationList = conversationService.getConversationList();
        return Result.success(conversationList);
    }
}
