package com.springleaf.easychat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户状态枚举
 */
@Getter
@AllArgsConstructor
public enum UserStatusEnum {

    /**
     * 禁用
     */
    DISABLED(0, "禁用"),

    /**
     * 正常
     */
    NORMAL(1, "正常"),

    /**
     * 注销
     */
    DELETED(2, "注销");

    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static UserStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }
}
