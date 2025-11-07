package com.springleaf.easychat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.springleaf.easychat.model.dto.AddGroupMemberDTO;
import com.springleaf.easychat.model.dto.CreateGroupDTO;
import com.springleaf.easychat.model.entity.Group;

import java.util.List;

/**
 * 群组服务接口
 */
public interface GroupService extends IService<Group> {

    /**
     * 创建群组
     *
     * @param createGroupDTO 创建群组请求
     * @return 群组信息
     */
    Group createGroup(CreateGroupDTO createGroupDTO);

    /**
     * 添加群成员
     *
     */
    void addMembers(AddGroupMemberDTO addGroupMemberDTO);
}
