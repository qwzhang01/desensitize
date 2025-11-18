package com.qw.desensitize.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.qw.desensitize.common.MaskName;
import io.github.qwzhang01.desensitize.domain.Encrypt;
import io.github.qwzhang01.desensitize.encrypt.annotation.EncryptField;
import io.github.qwzhang01.desensitize.mask.annotation.MaskId;
import io.github.qwzhang01.desensitize.mask.annotation.MaskPhone;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 自定义字段信息表
 * </p>
 *
 * @author avinzhang
 * @since 2025-10-03
 */
@Getter
@Setter
@ToString
@TableName("user")
@Accessors(chain = true)
public class User extends Model<User> {

    private static final long serialVersionUID = 1L;

    /**
     * ID主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 姓名
     */
    @MaskName
    @TableField("`name`")
    private String name;

    /**
     * 手机号码
     */
    @MaskPhone
    @EncryptField
    @TableField("phoneNo")
    private String phoneNo;

    /**
     * 性别
     */
    @TableField("gender")
    private String gender;

    /**
     * 身份证号码
     */
    @MaskId
    @TableField("idNo")
    private Encrypt idNo;

    @Override
    public Serializable pkVal() {
        return this.id;
    }
}
