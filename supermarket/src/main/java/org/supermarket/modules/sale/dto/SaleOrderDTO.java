package org.supermarket.modules.sale.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SaleOrderDTO {

    @NotNull(message = "门店不能为空")
    private Long storeId;

    @NotNull(message = "收银员不能为空")
    private Long cashierId;

    private Long memberId;      // 会员ID（和手机号二选一）
    private String memberPhone; // 会员手机号（收银时扫码或输入手机号）

    @NotNull(message = "支付方式不能为空")
    private Integer paymentType;

    private BigDecimal paidAmount;
    private String remark;

    @NotEmpty(message = "购物车不能为空")
    private List<SaleItemDTO> items;

    @Data
    public static class SaleItemDTO {

        @NotNull(message = "商品不能为空")
        private Long productId;

        @NotNull(message = "数量不能为空")
        private BigDecimal quantity;

        private BigDecimal unitPrice; // 促销价，不传则用原价
    }
}