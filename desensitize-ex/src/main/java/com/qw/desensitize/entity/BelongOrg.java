package com.qw.desensitize.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 对象数据所属组织表
 * </p>
 *
 * @author avinzhang
 * @since 2025-10-03
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("belong_org")
public class BelongOrg extends Model<BelongOrg> {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 对象类型
     */
    @TableField("objectType")
    private String objectType;

    /**
     * 对象id
     */
    @TableField("objectId")
    private Long objectId;

    /**
     * 组织id
     */
    @TableField("orgId")
    private Long orgId;

    /**
     * 创建时间
     */
    @TableField(value = "createTime", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 创建人
     */
    @TableField(value = "createBy", fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新时间
     */
    @TableField(value = "updateTime", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 更新人
     */
    @TableField(value = "updateBy", fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 删除标识符,正常1,删除0
     */
    @TableLogic
    @TableField("enableFlag")
    private Boolean enableFlag;

    @Override
    public Serializable pkVal() {
        return this.id;
    }
}
