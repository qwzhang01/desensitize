package com.qw.desensitize.dto;

import io.github.qwzhang01.desensitize.annotation.EncryptField;
import io.github.qwzhang01.desensitize.annotation.Mask;
import io.github.qwzhang01.desensitize.annotation.MaskId;
import io.github.qwzhang01.desensitize.annotation.MaskPhone;
import io.github.qwzhang01.desensitize.domain.Encrypt;
import io.github.qwzhang01.desensitize.shield.NameCoverAlgo;
import lombok.Data;

/**
 * 用户dto
 * 继承父类，需要做脱敏处理
 */
@Data
public class UserDto {
    private Long id;
    @Mask(value = NameCoverAlgo.class)
    private String name;
    /**
     * 对手机号码做脱敏的主机，脱敏算法是手机号码
     */
    @MaskPhone
    @EncryptField
    private String phoneNo;
    private String gender;
    @MaskId
    private Encrypt idNo;
}
