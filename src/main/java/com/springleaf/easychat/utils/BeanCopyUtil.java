package com.springleaf.easychat.utils;

import org.springframework.beans.BeanUtils;

/**
 * Bean拷贝工具类
 */
public class BeanCopyUtil {

    /**
     * 复制对象属性
     *
     * @param source 源对象
     * @param targetClass 目标类
     * @param <T> 目标类型
     * @return 目标对象
     */
    public static <T> T copy(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("对象拷贝失败", e);
        }
    }
}
