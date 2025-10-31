package com.springleaf.easychat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 会话类型枚举
 */
@Getter
@AllArgsConstructor
public enum ConversationTypeEnum {

    /**
     * 单聊
     */
    SINGLE(1, "单聊"),

    /**
     * 群聊
     */
    GROUP(2, "群聊");

    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static ConversationTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ConversationTypeEnum typeEnum : values()) {
            if (typeEnum.getCode().equals(code)) {
                return typeEnum;
            }
        }
        return null;
    }
}
