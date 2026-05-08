package org.supermarket.modules.stats.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StatsVO {

    // ---- 今日概览 ----
    private BigDecimal todaySaleAmount;   // 今日销售额
    private Integer todayOrderCount;      // 今日订单数
    private BigDecimal todayAvgAmount;    // 今日客单价
    private Integer todayMemberCount;     // 今日新增会员数

    // ---- 库存预警 ----
    private Integer warningProductCount;  // 预警商品数量

    // ---- 折线图：近7天销售额 ----
    private List<DailyStats> last7Days;

    // ---- 排行榜：销量TOP5商品 ----
    private List<ProductRank> topProducts;

    // ---- 饼图：商品分类销售占比 ----
    private List<CategoryStats> categoryStats;

    @Data
    public static class DailyStats {
        private String date;              // 日期 如 02-22
        private BigDecimal amount;        // 当日销售额
        private Integer orderCount;       // 当日订单数
    }

    @Data
    public static class ProductRank {
        private Long productId;
        private String productName;
        private BigDecimal totalQty;      // 销售总量
        private BigDecimal totalAmount;   // 销售总额
    }

    @Data
    public static class CategoryStats {
        private String categoryName;  // 一级分类名
        private BigDecimal amount;    // 销售额
    }
}