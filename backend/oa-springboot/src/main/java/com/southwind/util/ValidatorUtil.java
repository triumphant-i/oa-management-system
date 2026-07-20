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
}