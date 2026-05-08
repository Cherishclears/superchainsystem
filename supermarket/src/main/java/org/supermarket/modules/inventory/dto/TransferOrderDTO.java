package org.supermarket.modules.inventory.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TransferOrderDTO {
    private Long fromStoreId;
    private Long toStoreId;
    private String remark;
    private List<Item> items;

    @Data
    public static class Item {
        private Long productId;
        private String productName;
        private BigDecimal quantity;
    }
}