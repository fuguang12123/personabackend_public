package com.example.persona_backend.common;

import lombok.Data;

/**
 * 统一 API 响应结果封装
 * @param <T> 数据载荷类型
 */
@Data
public class Result<T> {

    private Integer code; // 状态码：200成功，其他失败
    private String message; // 提示信息
    private T data; // 数据载荷

    public Result() {}

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 成功静态方法
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> success(T data, String msg) {
        return new Result<>(200, msg, data);
    }

    // 失败静态方法
    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }

    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }
}