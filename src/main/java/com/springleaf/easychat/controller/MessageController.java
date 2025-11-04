package com.springleaf.easychat.controller;

import com.springleaf.easychat.common.Result;
import com.springleaf.easychat.model.dto.MessageHistoryDTO;
import com.springleaf.easychat.model.vo.MessageVO;
import com.springleaf.easychat.service.MessageService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息控制器
 */
@Slf4j
@RestController
@RequestMapping("/message")
public class MessageController {

    @Resource
    private MessageService messageService;

    /**
     * 获取会话的历史消息（分页）
     *
     * @param queryDTO 查询参数
     * @return 分页消息列表
     */
    @GetMapping("/history")
    public Result<List<MessageVO>> getMessageHistory(@Valid MessageHistoryDTO queryDTO) {
        List<MessageVO> messagePage = messageService.getMessageHistory(queryDTO);
        return Result.success(messagePage);
    }

    /**
     * 删除消息
     */
    @DeleteMapping("/delete")
    public Result<Void> deleteMessage(@RequestParam @NotNull(message = "消息ID不能为空") Long messageId) {
        messageService.deleteMessage(messageId);
        return Result.success();
    }

    /**
     * 撤回消息
     */
    @PostMapping("/recall")
    public Result<Void> recallMessage(@RequestParam @NotNull(message = "消息ID不能为空") Long messageId) {
        messageService.recallMessage(messageId);
        return Result.success();
    }
}
