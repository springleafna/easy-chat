package com.springleaf.easychat.enums;

import lombok.Getter;

/**
 * 群成员角色枚举
 */
@Getter
public enum GroupMemberRoleEnum {

    /**
     * 普通成员
     */
    MEMBER(1, "普通成员"),

    /**
     * 管理员
     */
    ADMIN(2, "管理员"),

    /**
     * 群主
     */
    OWNER(3, "群主");

    private final Integer code;
    private final String description;

    GroupMemberRoleEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
