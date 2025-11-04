package com.springleaf.easychat.controller;

import com.springleaf.easychat.common.Result;
import com.springleaf.easychat.model.dto.ActiveChatDTO;
import com.springleaf.easychat.model.vo.ConversationVO;
import com.springleaf.easychat.service.ConversationService;
import com.springleaf.easychat.service.UnreadService;
import com.springleaf.easychat.utils.UserContextUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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

    @Resource
    private UnreadService unreadService;

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

    /**
     * 设置活跃会话
     * 用户进入聊天页时调用，设置当前活跃的会话ID
     *
     * @param dto 活跃会话DTO
     * @return 操作结果
     */
    @PostMapping("/active")
    public Result<Void> setActiveChat(@Valid @RequestBody ActiveChatDTO dto) {
        Long userId = UserContextUtil.getCurrentUserId();
        unreadService.setActiveChat(userId, dto.getConversationId());
        return Result.success();
    }

    /**
     * 切换会话置顶状态
     *
     * @param conversationId 会话ID
     * @return 操作结果
     */
    @PutMapping("/pin/{conversationId}")
    public Result<Void> togglePin(@PathVariable @NotBlank(message = "会话ID不能为空") String conversationId) {
        conversationService.togglePin(conversationId);
        return Result.success();
    }

    /**
     * 切换会话免打扰状态
     *
     * @param conversationId 会话ID
     * @return 操作结果
     */
    @PutMapping("/mute/{conversationId}")
    public Result<Void> toggleMute(@PathVariable @NotBlank(message = "会话ID不能为空") String conversationId) {
        conversationService.toggleMute(conversationId);
        return Result.success();
    }
}
