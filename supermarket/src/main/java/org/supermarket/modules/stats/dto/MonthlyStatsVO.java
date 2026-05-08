package org.supermarket.modules.stats.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class MonthlyStatsVO {
    private String month;
    private BigDecimal totalAmount;
    private Integer totalOrders;
    private BigDecimal avgOrderAmount;
    private Integer newMembers;
    private BigDecimal totalAmountLastMonth;
    private String growthRate;
    private List<StatsVO.ProductRank> topProducts;
    private List<StatsVO.CategoryStats> categoryStats;
}