package org.supermarket.modules.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("store_product_config")
public class StoreProductConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long storeId;
    private Long productId;
    private BigDecimal retailPrice;  // null 表示用商品默认价
    private BigDecimal memberPrice;
    private Integer status;          // 1在售 0不售
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}