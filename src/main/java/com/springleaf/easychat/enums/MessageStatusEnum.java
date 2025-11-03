package com.springleaf.easychat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息状态枚举
 */
@Getter
@AllArgsConstructor
public enum MessageStatusEnum {

    /**
     * 已撤回
     */
    WITHDRAWN(0, "已撤回"),

    /**
     * 正常
     */
    NORMAL(1, "正常"),

    /**
     * 已删除
     */
    DELETED(2, "已删除");

    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static MessageStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (MessageStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }
}
