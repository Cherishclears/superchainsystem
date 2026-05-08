package org.supermarket.modules.sale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("sale_order_item")
public class SaleOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;     // 冗余商品名，防止改名影响历史记录
    private String barcode;         // 冗余条形码
    private BigDecimal unitPrice;   // 实际成交单价
    private BigDecimal originalPrice; // 原零售价
    private BigDecimal quantity;
    private BigDecimal subtotal;
}