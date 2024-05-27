package com.qw.desensitize.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 脱敏实体继承类
 *
 * @author avinzhang
 */
@Data
@Accessors(chain = true)
public class UnSensitiveDto implements Serializable {
    /**
     * 是否脱敏
     */
    private boolean sensitiveFlag = false;

    /**
     * 脱敏类型
     * 邮箱
     * 电话
     * 身份证
     */
    public static final String UN_SENSITIVE_EMAIL = "email";
    public static final String UN_SENSITIVE_PHONE = "phone";
    public static final String UN_SENSITIVE_ID_NUM = "idNum";
}