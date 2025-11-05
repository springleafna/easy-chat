package com.springleaf.easychat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.easychat.enums.ConversationStatusEnum;
import com.springleaf.easychat.enums.ConversationTypeEnum;
import com.springleaf.easychat.enums.GroupMemberRoleEnum;
import com.springleaf.easychat.enums.GroupMemberStatusEnum;
import com.springleaf.easychat.enums.GroupStatusEnum;
import com.springleaf.easychat.exception.BusinessException;
import com.springleaf.easychat.mapper.ConversationMapper;
import com.springleaf.easychat.mapper.GroupMapper;
import com.springleaf.easychat.mapper.GroupMemberMapper;
import com.springleaf.easychat.model.dto.CreateGroupDTO;
import com.springleaf.easychat.model.entity.Conversation;
import com.springleaf.easychat.model.entity.Group;
import com.springleaf.easychat.model.entity.GroupMember;
import com.springleaf.easychat.model.entity.User;
import com.springleaf.easychat.service.GroupService;
import com.springleaf.easychat.service.UserService;
import com.springleaf.easychat.utils.ConversationIdUtil;
import com.springleaf.easychat.utils.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 群组服务实现类
 */
@Slf4j
@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group> implements GroupService {

    private final GroupMemberMapper groupMemberMapper;
    private final ConversationMapper conversationMapper;
    private final UserService userService;

    public GroupServiceImpl(GroupMemberMapper groupMemberMapper,
                           ConversationMapper conversationMapper,
                           UserService userService) {
        this.groupMemberMapper = groupMemberMapper;
        this.conversationMapper = conversationMapper;
        this.userService = userService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Group createGroup(CreateGroupDTO createGroupDTO) {
        // 获取当前登录用户（群主）
        Long ownerId = UserContextUtil.getCurrentUserId();

        // 验证群成员是否存在
        List<Long> memberIds = createGroupDTO.getMemberIds();
        validateUsers(memberIds);

        // 创建群组
        Group group = new Group();
        group.setGroupName(createGroupDTO.getGroupName());
        group.setOwnerId(ownerId);
        group.setAvatarUrl(createGroupDTO.getAvatarUrl());
        group.setAnnouncement(createGroupDTO.getAnnouncement());
        group.setMaxMembers(createGroupDTO.getMaxMembers() != null ? createGroupDTO.getMaxMembers() : 500);
        group.setStatus(GroupStatusEnum.NORMAL.getCode());

        // 保存群组
        this.save(group);
        log.info("创建群组成功，群组ID: {}, 群主ID: {}", group.getId(), ownerId);

        // 添加群主为群成员（角色为群主）
        GroupMember ownerMember = new GroupMember();
        ownerMember.setGroupId(group.getId());
        ownerMember.setUserId(ownerId);
        ownerMember.setRole(GroupMemberRoleEnum.OWNER.getCode());
        ownerMember.setStatus(GroupMemberStatusEnum.NORMAL.getCode());
        ownerMember.setJoinedAt(LocalDateTime.now());
        groupMemberMapper.insert(ownerMember);

        // 添加其他成员（角色为普通成员）
        addMembersInternal(group.getId(), memberIds, GroupMemberRoleEnum.MEMBER.getCode());

        // 为所有群成员创建会话记录（包括群主）
        createConversationsForAllMembers(group.getId(), ownerId, memberIds);

        log.info("群组创建完成，群组ID: {}, 总成员数: {}", group.getId(), memberIds.size() + 1);
        return group;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMembers(Long groupId, List<Long> userIds) {
        // 获取当前用户
        Long currentUserId = UserContextUtil.getCurrentUserId();

        // 验证群组是否存在且正常
        Group group = this.getById(groupId);
        if (group == null) {
            throw new BusinessException("群组不存在");
        }
        if (!GroupStatusEnum.NORMAL.getCode().equals(group.getStatus())) {
            throw new BusinessException("群组已解散，无法添加成员");
        }

        // 验证当前用户是否有权限添加成员（群主或管理员）
        LambdaQueryWrapper<GroupMember> currentUserWrapper = new LambdaQueryWrapper<>();
        currentUserWrapper.eq(GroupMember::getGroupId, groupId)
                         .eq(GroupMember::getUserId, currentUserId)
                         .eq(GroupMember::getStatus, GroupMemberStatusEnum.NORMAL.getCode());
        GroupMember currentUserMember = groupMemberMapper.selectOne(currentUserWrapper);

        if (currentUserMember == null) {
            throw new BusinessException("您不是该群成员，无权添加成员");
        }

        Integer currentUserRole = currentUserMember.getRole();
        if (!GroupMemberRoleEnum.OWNER.getCode().equals(currentUserRole)
            && !GroupMemberRoleEnum.ADMIN.getCode().equals(currentUserRole)) {
            throw new BusinessException("只有群主或管理员才能添加成员");
        }

        // 验证用户是否存在
        validateUsers(userIds);

        // 查询当前群成员数量
        LambdaQueryWrapper<GroupMember> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(GroupMember::getGroupId, groupId)
                   .eq(GroupMember::getStatus, GroupMemberStatusEnum.NORMAL.getCode());
        long currentMemberCount = groupMemberMapper.selectCount(countWrapper);

        if (currentMemberCount + userIds.size() > group.getMaxMembers()) {
            throw new BusinessException("群成员数量已达上限，最多允许" + group.getMaxMembers() + "人");
        }

        // 过滤掉已经是群成员的用户
        List<Long> newMemberIds = filterExistingMembers(groupId, userIds);
        if (newMemberIds.isEmpty()) {
            throw new BusinessException("所有用户都已经是群成员");
        }

        // 添加新成员
        addMembersInternal(groupId, newMemberIds, GroupMemberRoleEnum.MEMBER.getCode());

        // 为新成员创建会话记录
        createConversationsForNewMembers(groupId, newMemberIds);

        log.info("添加群成员成功，群组ID: {}, 新增成员数: {}", groupId, newMemberIds.size());
    }

    /**
     * 验证用户是否存在
     */
    private void validateUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException("用户ID列表不能为空");
        }

        // 去重
        Set<Long> uniqueUserIds = new HashSet<>(userIds);

        // 批量查询用户
        List<User> users = userService.listByIds(uniqueUserIds);
        if (users.size() != uniqueUserIds.size()) {
            throw new BusinessException("部分用户不存在");
        }
    }

    /**
     * 过滤掉已经是群成员的用户
     */
    private List<Long> filterExistingMembers(Long groupId, List<Long> userIds) {
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId)
               .in(GroupMember::getUserId, userIds)
               .eq(GroupMember::getStatus, GroupMemberStatusEnum.NORMAL.getCode());

        List<GroupMember> existingMembers = groupMemberMapper.selectList(wrapper);
        Set<Long> existingUserIds = new HashSet<>();
        for (GroupMember member : existingMembers) {
            existingUserIds.add(member.getUserId());
        }

        List<Long> newMemberIds = new ArrayList<>();
        for (Long userId : userIds) {
            if (!existingUserIds.contains(userId)) {
                newMemberIds.add(userId);
            }
        }

        return newMemberIds;
    }

    /**
     * 添加成员（内部方法）
     */
    private void addMembersInternal(Long groupId, List<Long> userIds, Integer role) {
        LocalDateTime now = LocalDateTime.now();

        for (Long userId : userIds) {
            GroupMember member = new GroupMember();
            member.setGroupId(groupId);
            member.setUserId(userId);
            member.setRole(role);
            member.setStatus(GroupMemberStatusEnum.NORMAL.getCode());
            member.setJoinedAt(now);
            groupMemberMapper.insert(member);
        }
    }

    /**
     * 为所有群成员创建会话记录（创建群组时使用）
     */
    private void createConversationsForAllMembers(Long groupId, Long ownerId, List<Long> memberIds) {
        String conversationId = ConversationIdUtil.generateGroupChatId(groupId);

        // 为群主创建会话
        createConversation(ownerId, conversationId, groupId);

        // 为其他成员创建会话
        for (Long memberId : memberIds) {
            createConversation(memberId, conversationId, groupId);
        }
    }

    /**
     * 为新成员创建会话记录（添加成员时使用）
     */
    private void createConversationsForNewMembers(Long groupId, List<Long> memberIds) {
        String conversationId = ConversationIdUtil.generateGroupChatId(groupId);

        for (Long memberId : memberIds) {
            createConversation(memberId, conversationId, groupId);
        }
    }

    /**
     * 创建单个会话记录
     */
    private void createConversation(Long userId, String conversationId, Long groupId) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setConversationId(conversationId);
        conversation.setTargetId(groupId);
        conversation.setType(ConversationTypeEnum.GROUP.getCode());
        conversation.setPinned(false);
        conversation.setMuted(false);
        conversation.setStatus(ConversationStatusEnum.NORMAL.getCode());
        conversationMapper.insert(conversation);
    }
}
