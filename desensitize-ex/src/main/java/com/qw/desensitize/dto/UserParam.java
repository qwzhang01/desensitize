package com.qw.desensitize.dto;

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
    private String phoneNo;
    private String gender;
    private Encrypt idNo;
}
