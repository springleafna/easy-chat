package com.springleaf.easychat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 好友申请状态枚举
 */
@Getter
@AllArgsConstructor
public enum FriendRequestStatusEnum {

    /**
     * 待处理
     */
    PENDING(0, "待处理"),

    /**
     * 已同意
     */
    ACCEPTED(1, "已同意"),

    /**
     * 已拒绝
     */
    REJECTED(2, "已拒绝"),

    /**
     * 已撤回
     */
    WITHDRAWN(3, "已撤回");

    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static FriendRequestStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (FriendRequestStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }
}
