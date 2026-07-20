package com.southwind.vo;

import lombok.Data;

@Data
public class ResultVO <T>{
    private Integer code;
    private T data;
    private String message; // 新增 message 字段

    // 手动添加 setter 方法（解决 Lombok 编译问题）
    public void setCode(Integer code) {
        this.code = code;
    }

    public void setData(T data) {
        this.data = data;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // 手动添加 getter 方法
    public Integer getCode() {
        return code;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}
