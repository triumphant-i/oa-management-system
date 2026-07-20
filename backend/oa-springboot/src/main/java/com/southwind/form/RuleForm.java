package com.southwind.form;

import lombok.Data;

@Data
public class RuleForm {
    private String name;
    private String username;
    private String password;

    // getter 和 setter 方法
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        // 兼容前端传递的username字段，如果没有则使用name字段
        return username != null ? username : name;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}

