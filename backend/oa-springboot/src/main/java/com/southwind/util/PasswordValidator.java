package com.southwind.util;

import java.security.MessageDigest;

/**
 * 密码验证工具
 */
public class PasswordValidator {

    public static void main(String[] args) {
        // 数据库中的密码
        String dbPassword = "4e162fb2$0f63eb83b776b9c7224e2b3ececa9513";
        String inputPassword = "123456";

        // 解析
        String[] parts = dbPassword.split("\\$");
        String salt = parts[0];
        String hash = parts[1];

        System.out.println("数据库密码: " + dbPassword);
        System.out.println("盐值: " + salt);
        System.out.println("Hash: " + hash);
        System.out.println();

        // 重新计算MD5
        String input = salt + inputPassword;
        System.out.println("输入: " + input);

        String calculatedHash = md5(input);
        System.out.println("计算的Hash: " + calculatedHash);
        System.out.println();

        // 验证
        if (hash.equals(calculatedHash)) {
            System.out.println("✅ 密码匹配成功！");
        } else {
            System.out.println("❌ 密码不匹配");
            System.out.println("数据库Hash: " + hash);
            System.out.println("计算Hash:   " + calculatedHash);
        }
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5加密失败", e);
        }
    }
}