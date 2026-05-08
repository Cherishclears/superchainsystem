package org.supermarket.modules.sale.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.supermarket.common.entity.BaseEntity;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sale_order")
public class SaleOrder extends BaseEntity {

    private String orderNo;           // 订单号
    private Long storeId;             // 门店
    private Long cashierId;           // 收银员
    private Long memberId;            // 会员（非会员为null）
    private BigDecimal totalAmount;   // 商品总金额
    private BigDecimal discountAmount;// 优惠金额
    private BigDecimal payableAmount; // 应付金额
    private BigDecimal paidAmount;    // 实付金额
    private Integer paymentType;      // 1现金 2微信 3支付宝 4会员卡
    private Integer pointsEarned;     // 本单获得积分
    private Integer status;           // 1已完成 2已退款
    private String remark;
}