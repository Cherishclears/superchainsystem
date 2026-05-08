package org.supermarket.modules.purchase.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AiSuggestVO {
    private Long productId;
    private String productName;
    private BigDecimal suggestQty;      // AI 建议采购数量
    private BigDecimal currentQty;      // 当前库存
    private BigDecimal warningQty;      // 预警下限
    private BigDecimal soldQty7Days;    // 近7天销量
    private String reason;              // AI 给出的补货理由
}