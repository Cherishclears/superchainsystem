package org.supermarket.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public abstract class BaseEntity implements Serializable {

    // 主键，自增
    @TableId(type = IdType.AUTO)
    private Long id;

    // 创建时间，插入时自动填充
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // 更新时间，插入和更新时自动填充
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // 创建人ID，插入时自动填充
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    // 更新人ID，插入和更新时自动填充
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    // 逻辑删除：0正常 1已删除，接口返回时不显示这个字段
    @JsonIgnore
    @TableLogic
    private Integer isDeleted;
}
