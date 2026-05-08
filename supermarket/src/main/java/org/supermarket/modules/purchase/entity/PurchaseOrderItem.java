package org.supermarket.modules.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("purchase_order_item")
public class PurchaseOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;           // 采购单ID
    private Long productId;         // 商品ID
    private BigDecimal purchasePrice; // 本次采购价
    private BigDecimal quantity;    // 采购数量
    private BigDecimal receivedQty; // 已收货数量
    private BigDecimal subtotal;    // 小计
    private LocalDate expireDate;   // 本批次到期日
    private String remark;
}