package com.springleaf.easychat.controller;

import com.springleaf.easychat.common.Result;
import com.springleaf.easychat.model.dto.AddGroupMemberDTO;
import com.springleaf.easychat.model.dto.CreateGroupDTO;
import com.springleaf.easychat.model.entity.Group;
import com.springleaf.easychat.service.GroupService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 群组控制器
 */
@Slf4j
@RestController
@RequestMapping("/group")
@Validated
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    /**
     * 创建群组
     *
     * @param createGroupDTO 创建群组请求
     * @return 群组信息
     */
    @PostMapping("/create")
    public Result<Group> createGroup(@Valid @RequestBody CreateGroupDTO createGroupDTO) {
        log.info("创建群组，群名称: {}", createGroupDTO.getGroupName());
        Group group = groupService.createGroup(createGroupDTO);
        return Result.success(group);
    }

    /**
     * 添加群成员
     *
     * @param addGroupMemberDTO 添加群成员请求
     * @return 操作结果
     */
    @PostMapping("/addMembers")
    public Result<Void> addMembers(@Valid @RequestBody AddGroupMemberDTO addGroupMemberDTO) {
        groupService.addMembers(addGroupMemberDTO);
        return Result.success();
    }
}
