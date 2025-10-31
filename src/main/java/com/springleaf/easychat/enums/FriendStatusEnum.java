package com.springleaf.easychat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 好友关系状态枚举
 */
@Getter
@AllArgsConstructor
public enum FriendStatusEnum {

    /**
     * 已删除
     */
    DELETED(0, "已删除"),

    /**
     * 正常
     */
    NORMAL(1, "正常"),

    /**
     * 黑名单
     */
    BLACKLIST(2, "黑名单");

    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static FriendStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (FriendStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }
}
