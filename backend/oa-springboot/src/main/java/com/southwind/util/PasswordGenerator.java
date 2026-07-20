package com.southwind.util;

import java.security.MessageDigest;
import java.util.UUID;

/**
 * 密码生成工具（用于生成测试密码）
 * 运行此类的main方法生成加密密码
 */
public class PasswordGenerator {

    private static final String SALT_SEPARATOR = "$";

    /**
     * 加密密码
     */
    public static String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        // 生成随机盐值
        String salt = UUID.randomUUID().toString().substring(0, 8);

        // 加密：MD5(salt + password)
        String hash = md5(salt + rawPassword);

        // 返回：salt$hash
        return salt + SALT_SEPARATOR + hash;
    }

    /**
     * MD5加密
     */
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

    /**
     * 主方法：生成测试密码
     */
    public static void main(String[] args) {
        System.out.println("=====================================");
        System.out.println("密码生成工具");
        System.out.println("=====================================");
        System.out.println();

        String password = "123456";

        System.out.println("明文密码: " + password);
        System.out.println();

        // 生成3个不同的加密密码（每个用户使用不同的盐值）
        System.out.println("zhangsan (系统管理员):");
        String pwd1 = encode(password);
        System.out.println("加密密码: " + pwd1);
        System.out.println();

        System.out.println("lisi (部门主管):");
        String pwd2 = encode(password);
        System.out.println("加密密码: " + pwd2);
        System.out.println();

        System.out.println("wangwu (流程管理员):");
        String pwd3 = encode(password);
        System.out.println("加密密码: " + pwd3);
        System.out.println();

        System.out.println("=====================================");
        System.out.println("请将以上密码更新到数据库");
        System.out.println("=====================================");
    }
}