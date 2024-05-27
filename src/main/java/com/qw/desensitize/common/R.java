package com.qw.desensitize.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口返回包装类
 *
 * @param <T>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class R<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> R success() {
        return new R(0, "操作成功", null);
    }

    public static <T> R success(T data) {
        return new R(0, "获取成功", data);
    }

    public static <T> R error() {
        return new R(-1, "获取失败", null);
    }
}
