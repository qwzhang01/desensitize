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
 * 组织信息表
 * </p>
 *
 * @author avinzhang
 * @since 2025-10-03
 */
@Getter
@Setter
@ToString
@TableName("unit_info")
@Accessors(chain = true)
public class UnitInfo extends Model<UnitInfo> {

    private static final long serialVersionUID = 1L;

    /**
     * 组织ID
     */
    @TableId(value = "unitId", type = IdType.ASSIGN_ID)
    private Integer unitId;

    /**
     * 组织名称
     */
    @TableField("unitName")
    private String unitName;

    /**
     * 组织全名
     */
    @TableField("unitFullName")
    private String unitFullName;

    /**
     * 组织全路径
     */
    @TableField("unitFullPath")
    private String unitFullPath;

    /**
     * 父组织ID
     */
    @TableField("parentUnitId")
    private Integer parentUnitId;

    /**
     * 虚拟组织（1：虚拟组织，0，实体组织）
     */
    @TableField("virtualFlag")
    private Boolean virtualFlag;

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
     * 删除标识符（正常1,删除0）
     */
    @TableLogic
    @TableField("enableFlag")
    private Boolean enableFlag;

    @Override
    public Serializable pkVal() {
        return this.unitId;
    }
}
