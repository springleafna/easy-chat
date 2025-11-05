package com.springleaf.easychat.enums;

import lombok.Getter;

/**
 * 群成员状态枚举
 */
@Getter
public enum GroupMemberStatusEnum {

    /**
     * 已退出
     */
    QUIT(0, "已退出"),

    /**
     * 正常
     */
    NORMAL(1, "正常");

    private final Integer code;
    private final String description;

    GroupMemberStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
