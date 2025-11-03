package com.springleaf.easychat.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.springleaf.easychat.common.Result;
import com.springleaf.easychat.model.dto.MessageHistoryDTO;
import com.springleaf.easychat.model.vo.MessageVO;
import com.springleaf.easychat.service.MessageService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
    public Result<Page<MessageVO>> getMessageHistory(@Valid MessageHistoryDTO queryDTO) {
        Page<MessageVO> messagePage = messageService.getMessageHistory(queryDTO);
        return Result.success(messagePage);
    }
}
