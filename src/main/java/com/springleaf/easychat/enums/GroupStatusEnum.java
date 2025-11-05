package com.springleaf.easychat.enums;

import lombok.Getter;

/**
 * 群组状态枚举
 */
@Getter
public enum GroupStatusEnum {

    /**
     * 已解散
     */
    DISBANDED(0, "已解散"),

    /**
     * 正常
     */
    NORMAL(1, "正常");

    private final Integer code;
    private final String description;

    GroupStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
