package com.qw.desensitize.dto;

import io.github.qwzhang01.desensitize.annotation.EncryptField;
import io.github.qwzhang01.desensitize.domain.Encrypt;
import lombok.Data;

/**
 * 用户dto
 * 继承父类，需要做脱敏处理
 */
@Data
public class UserParam {
    private Long id;
    private String name;
    /**
     * 对手机号码做脱敏的主机，脱敏算法是手机号码
     */
    @EncryptField
    private String phoneNo;
    private String gender;
    private Encrypt idNo;
}
