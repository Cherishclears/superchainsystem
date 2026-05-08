package org.supermarket.modules.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.supermarket.common.entity.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product_category")
public class ProductCategory extends BaseEntity {

    private Long parentId;        // 父分类ID，0为顶级
    private String categoryName;  // 分类名称
    private Integer level;        // 层级 1一级 2二级
    private Integer sortOrder;    // 排序
    private Integer status;       // 1启用 0禁用
}