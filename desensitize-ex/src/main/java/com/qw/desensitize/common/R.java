package com.qw.desensitize.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class R<T> implements Serializable {
    private int code;
    private String msg;
    private T data;

    public static <T> R<T> ok(T data) {
        R<T> result = new R<>();
        result.setCode(HttpStatus.OK.value());
        result.setMsg("success");
        result.setData(data);
        return result;
    }

    public static R<Void> info(String msg) {
        R<Void> result = new R<>();
        result.setCode(HttpStatus.OK.value());
        result.setMsg(msg);
        return result;
    }

    public static <T> R<T> warn(String msg) {
        R<T> result = new R<>();
        result.setCode(HttpStatus.CHECKPOINT.value());
        result.setMsg(msg);
        return result;
    }

    public static <T> R<T> warn(String msg, T data) {
        R<T> result = new R<>();
        result.setCode(HttpStatus.CHECKPOINT.value());
        result.setMsg(msg);
        return result;
    }

    public static <T> R<T> error(String msg) {
        R<T> result = new R<>();
        result.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        result.setMsg(msg);
        return result;
    }

    public static <T> R<T> error(Throwable t) {
        R<T> result = new R<>();
        result.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        result.setMsg(t.getLocalizedMessage());
        return result;
    }

    public static <T> R<T> success(T dto) {
        R<T> result = new R<>();
        result.setCode(HttpStatus.OK.value());
        result.setMsg("success");
        result.setData(dto);
        return result;
    }
}
