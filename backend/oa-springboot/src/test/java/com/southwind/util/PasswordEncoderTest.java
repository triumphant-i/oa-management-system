package com.southwind.util;

import org.junit.jupiter.api.Test;

public class PasswordEncoderTest {

    @Test
    public void testEncode() {
        PasswordEncoder encoder = new PasswordEncoder();

        // 测试生成正确的密码格式
        String password = "123456";

        System.out.println("=========== 生成正确的密码 ===========");

        // 生成3次，每次的盐值不同
        for (int i = 0; i < 3; i++) {
            String encoded = encoder.encode(password);
            System.out.println("密码: " + password);
            System.out.println("加密后: " + encoded);
            System.out.println();
        }

        // 测试验证
        System.out.println("=========== 验证密码 ===========");
        String testEncoded = "abc12345$e99a18c428cb38d5f260853678922e03";
        boolean matches = encoder.matches("123456", testEncoded);
        System.out.println("测试密码: 123456");
        System.out.println("加密密码: " + testEncoded);
        System.out.println("验证结果: " + matches);
    }
}