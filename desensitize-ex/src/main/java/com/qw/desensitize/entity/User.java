package com.qw.desensitize.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.qwzhang01.desensitize.annotation.EncryptField;
import io.github.qwzhang01.desensitize.annotation.MaskId;
import io.github.qwzhang01.desensitize.annotation.MaskName;
import io.github.qwzhang01.desensitize.annotation.MaskPhone;
import io.github.qwzhang01.desensitize.domain.Encrypt;
import lombok.Data;

/**
 * 用户 entity
 */
@Data
@TableName("user")
public class User {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    @TableField("name")
    @MaskName
    private String name;
    /**
     * 使用拦截器方式加密
     */
    @MaskPhone
    @EncryptField
    @TableField("phoneNo")
    private String phoneNo;
    @TableField("gender")
    private String gender;
    /**
     * 使用类型转换器加密解密
     */
    @TableField("idNo")
    @MaskId
    private Encrypt idNo;
}
