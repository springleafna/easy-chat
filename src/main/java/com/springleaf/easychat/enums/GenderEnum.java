package com.springleaf.easychat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 性别枚举
 */
@Getter
@AllArgsConstructor
public enum GenderEnum {

    /**
     * 未知
     */
    UNKNOWN(0, "未知"),

    /**
     * 男
     */
    MALE(1, "男"),

    /**
     * 女
     */
    FEMALE(2, "女");

    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static GenderEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (GenderEnum genderEnum : values()) {
            if (genderEnum.getCode().equals(code)) {
                return genderEnum;
            }
        }
        return null;
    }
}
