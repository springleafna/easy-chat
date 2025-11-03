package com.springleaf.easychat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息类型枚举
 */
@Getter
@AllArgsConstructor
public enum MessageTypeEnum {

    /**
     * 文本消息
     */
    TEXT(1, "文本"),

    /**
     * 图片消息
     */
    IMAGE(2, "图片"),

    /**
     * 语音消息
     */
    VOICE(3, "语音"),

    /**
     * 视频消息
     */
    VIDEO(4, "视频"),

    /**
     * 文件消息
     */
    FILE(5, "文件"),

    /**
     * 位置消息
     */
    LOCATION(6, "位置"),

    /**
     * 系统消息
     */
    SYSTEM(7, "系统消息");

    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static MessageTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (MessageTypeEnum typeEnum : values()) {
            if (typeEnum.getCode().equals(code)) {
                return typeEnum;
            }
        }
        return null;
    }
}
