package com.southwind.vo;

import lombok.Data;

import java.util.List;

@Data
public class PageVO {
    private Object data;
    private Long total;
    private List<?> records;  // 分页数据列表
    private Long current;     // 当前页码
    private Long size;        // 每页大小

    // 手动添加 setter 方法（解决 Lombok 编译问题）
    public void setTotal(Long total) {
        this.total = total;
    }

    public void setRecords(List<?> records) {
        this.records = records;
    }

    public void setCurrent(Long current) {
        this.current = current;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public void setData(Object data) {
        this.data = data;
    }

    // 手动添加 getter 方法
    public Long getTotal() {
        return total;
    }

    public List<?> getRecords() {
        return records;
    }

    public Long getCurrent() {
        return current;
    }

    public Long getSize() {
        return size;
    }

    public Object getData() {
        return data;
    }
}