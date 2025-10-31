package com.springleaf.easychat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 会话状态枚举
 */
@Getter
@AllArgsConstructor
public enum ConversationStatusEnum {

    /**
     * 已删除
     */
    DELETED(0, "已删除"),

    /**
     * 正常
     */
    NORMAL(1, "正常");

    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static ConversationStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ConversationStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }
}
