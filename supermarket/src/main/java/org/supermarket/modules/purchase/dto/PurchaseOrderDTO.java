package org.supermarket.modules.purchase.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseOrderDTO {

    @NotNull(message = "门店不能为空")
    private Long storeId;

    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    private LocalDate expectedDate;
    private String remark;

    @NotEmpty(message = "采购明细不能为空")
    private List<PurchaseOrderItemDTO> items;

    @Data
    public static class PurchaseOrderItemDTO {
        @NotNull(message = "商品不能为空")
        private Long productId;

        @NotNull(message = "采购价不能为空")
        private java.math.BigDecimal purchasePrice;

        @NotNull(message = "采购数量不能为空")
        private java.math.BigDecimal quantity;

        private java.time.LocalDate expireDate;
        private String remark;
    }
}