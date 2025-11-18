package com.qw.desensitize.dto;

import com.qw.desensitize.common.MaskName;
import com.qw.desensitize.common.NameCoverAlgo;
import io.github.qwzhang01.desensitize.domain.Encrypt;
import io.github.qwzhang01.desensitize.encrypt.annotation.EncryptField;
import io.github.qwzhang01.desensitize.mask.annotation.Mask;
import io.github.qwzhang01.desensitize.mask.annotation.MaskId;
import io.github.qwzhang01.desensitize.mask.annotation.MaskPhone;
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
    @MaskName
    private String gender;
    @MaskId
    private Encrypt idNo;
}
