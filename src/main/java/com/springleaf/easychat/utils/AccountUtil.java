package com.springleaf.easychat.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 账号生成工具类
 */
public class AccountUtil {

    /**
     * 账号前缀
     */
    private static final String ACCOUNT_PREFIX = "EC";

    /**
     * 计数器（用于同一时刻生成多个账号时保证唯一性）
     */
    private static final AtomicInteger counter = new AtomicInteger(0);

    /**
     * 生成唯一账号
     * 格式：EC + 时间戳(yyyyMMddHHmmss) + 3位随机数
     *
     * @return 唯一账号
     */
    public static String generateAccount() {
        // 获取当前时间戳
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        // 获取计数器值（0-999循环）
        int count = counter.getAndIncrement() % 1000;

        // 格式化为3位数字（不足补0）
        String countStr = String.format("%03d", count);

        return ACCOUNT_PREFIX + timestamp + countStr;
    }
}
