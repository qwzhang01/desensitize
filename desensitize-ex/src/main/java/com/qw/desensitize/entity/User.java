package com.qw.desensitize.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qw.desensitize.common.encrypt1.EncryptField;
import com.qw.desensitize.common.encrypt1.EncryptObj;
import com.qw.desensitize.dto.Encrypt;
import lombok.Data;

/**
 * 用户 entity
 */
@Data
@TableName("user")
@EncryptObj
public class User {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;
    @TableField("name")
    private String name;
    /**
     * 使用拦截器方式加密
     */
    @EncryptField
    @TableField("phoneNo")
    private String phoneNo;
    @TableField("gender")
    private String gender;
    /**
     * 使用类型转换器加密解密
     */
    @TableField("idNo")
    private Encrypt idNo;
}
