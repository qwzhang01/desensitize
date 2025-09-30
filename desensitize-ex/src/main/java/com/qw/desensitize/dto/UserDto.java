package com.qw.desensitize.dto;

import com.qw.desensitize.common.sensitive.UnSensitive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户dto
 * 继承父类，需要做脱敏处理
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserDto extends UnSensitiveDto {
    private String name;
    /**
     * 对手机号码做脱敏的主机，脱敏算法是手机号码
     */
    @UnSensitive(type = UN_SENSITIVE_PHONE)
    private String phoneNo;
    private String gender;
    private Encrypt idNo;
}
