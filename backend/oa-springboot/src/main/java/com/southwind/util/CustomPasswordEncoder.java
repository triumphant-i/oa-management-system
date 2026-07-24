package com.southwind.util;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * 自定义密码加密工具类
 * 使用MD5+盐值进行密码加密和验证
 * 
 * 注意：这是简化实现，生产环境建议：
 * 1. 添加Spring Security依赖
 * 2. 使用BCryptPasswordEncoder替代此类
 * 3. BCrypt更安全，自动加盐，防彩虹表攻击
 * 
 * 当前实现特点：
 * 1. MD5+随机盐值
 * 2. 盐值存储在密码中（格式：salt$hash）
 * 3. 防止彩虹表攻击
 */
@Component("customPasswordEncoder")
public class CustomPasswordEncoder {

    private static final String SALT_SEPARATOR = "$";

    /**
     * 加密密码
     * @param rawPassword 明文密码
     * @return 加密后的密码（格式：salt$hash）
     */
    public String encode(String rawPassword) {
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
     * 验证密码
     * @param rawPassword 明文密码
     * @param encodedPassword 加密后的密码（格式：salt$hash）
     * @return 是否匹配
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        
        try {
            // 分离盐值和哈希值
            String[] parts = encodedPassword.split("\\$");
            if (parts.length != 2) {
                System.out.println("密码格式错误，parts.length=" + parts.length);
                return false;
            }
            
            String salt = parts[0];
            String hash = parts[1];
            
            // 验证：尝试两种格式
            // 新格式：MD5(salt + rawPassword)
            String combined = salt + rawPassword;
            String newHash = md5(combined);
            
            if (hash.equals(newHash)) {
                return true;
            }
            
            // 旧格式兼容：MD5(rawPassword)
            String oldHash = md5(rawPassword);
            
            if (hash.equals(oldHash)) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            System.out.println("密码验证异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * MD5加密
     * @param input 输入字符串
     * @return MD5哈希值
     */
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5加密失败", e);
        }
    }
}
