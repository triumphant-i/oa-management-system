package com.southwind.util;

import com.southwind.constant.SystemConstants;

/**
 * 参数校验工具类
 * 统一处理参数验证，避免NPE和非法参数
 */
public class ValidatorUtil {

    /**
     * 验证字符串非空
     * @param str 字符串
     * @param paramName 参数名（用于错误提示）
     * @throws IllegalArgumentException 如果为空
     */
    public static void notEmpty(String str, String paramName) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException(paramName + "不能为空");
        }
    }

    /**
     * 验证对象非空
     * @param obj 对象
     * @param paramName 参数名（用于错误提示）
     * @throws IllegalArgumentException 如果为空
     */
    public static void notNull(Object obj, String paramName) {
        if (obj == null) {
            throw new IllegalArgumentException(paramName + "不能为空");
        }
    }

    /**
     * 验证ID是否有效（大于0）
     * @param id ID值
     * @param paramName 参数名（用于错误提示）
     * @throws IllegalArgumentException 如果无效
     */
    public static void validId(Integer id, String paramName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(paramName + "无效");
        }
    }

    /**
     * 验证搜索字段是否合法（防止SQL注入）
     * @param field 字段名
     * @return 合法的字段名，非法返回null
     */
    public static String validateSearchField(String field) {
        if (!SystemConstants.isValidSearchField(field)) {
            return null;
        }
        return field;
    }

    /**
     * 安全解析整数
     * @param str 字符串
     * @param defaultValue 默认值（解析失败时返回）
     * @return 解析后的整数
     */
    public static Integer parseInt(String str, Integer defaultValue) {
        if (str == null || str.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 安全解析整数（必须成功）
     * @param str 字符串
     * @param paramName 参数名（用于错误提示）
     * @return 解析后的整数
     * @throws IllegalArgumentException 如果解析失败
     */
    public static Integer parseIntRequired(String str, String paramName) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException(paramName + "不能为空");
        }
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(paramName + "格式错误");
        }
    }

    /**
     * 验证字符串最大长度
     * @param str 字符串
     * @param maxLength 最大长度
     * @param paramName 参数名（用于错误提示）
     * @throws IllegalArgumentException 如果超过最大长度
     */
    public static void maxLength(String str, int maxLength, String paramName) {
        if (str != null && str.length() > maxLength) {
            throw new IllegalArgumentException(paramName + "长度不能超过" + maxLength + "个字符");
        }
    }

    /**
     * 验证字符串最小长度
     * @param str 字符串
     * @param minLength 最小长度
     * @param paramName 参数名（用于错误提示）
     * @throws IllegalArgumentException 如果小于最小长度
     */
    public static void minLength(String str, int minLength, String paramName) {
        if (str != null && str.length() < minLength) {
            throw new IllegalArgumentException(paramName + "长度不能少于" + minLength + "个字符");
        }
    }

    /**
     * 验证手机号格式
     * @param phone 手机号
     * @throws IllegalArgumentException 如果格式不正确
     */
    public static void validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        // 验证手机号格式（中国大陆手机号）
        if (!phone.trim().matches("^1[3-9]\\d{9}$")) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
    }

    /**
     * 验证邮箱格式
     * @param email 邮箱
     * @throws IllegalArgumentException 如果格式不正确
     */
    public static void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        // 验证邮箱格式
        if (!email.trim().matches("^[a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
    }
}