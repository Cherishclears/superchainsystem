package org.supermarket.modules.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.supermarket.common.entity.BaseEntity;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product")
public class Product extends BaseEntity {

    private String productCode;       // 商品编码
    private String barcode;           // 条形码
    private String productName;       // 商品名称
    private Long categoryId;          // 分类ID
    private Long supplierId;          // 供应商ID
    private String unit;              // 单位
    private String spec;              // 规格
    private BigDecimal purchasePrice; // 进价
    private BigDecimal retailPrice;   // 零售价
    private BigDecimal memberPrice;   // 会员价
    private Integer shelfLifeDays;    // 保质期（天）
    private String imageUrl;          // 图片
    private String description;       // 描述
    private Integer isWeighable;      // 是否称重商品 0否 1是
    private Integer status;           // 1上架 0下架
}