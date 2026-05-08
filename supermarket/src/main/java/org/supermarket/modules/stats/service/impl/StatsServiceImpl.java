package org.supermarket.modules.stats.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.supermarket.modules.inventory.mapper.InventoryMapper;
import org.supermarket.modules.member.entity.Member;
import org.supermarket.modules.member.mapper.MemberMapper;
import org.supermarket.modules.product.entity.Product;
import org.supermarket.modules.product.entity.ProductCategory;
import org.supermarket.modules.product.mapper.ProductCategoryMapper;
import org.supermarket.modules.product.mapper.ProductMapper;
import org.supermarket.modules.sale.entity.SaleOrder;
import org.supermarket.modules.sale.entity.SaleOrderItem;
import org.supermarket.modules.sale.mapper.SaleOrderItemMapper;
import org.supermarket.modules.sale.mapper.SaleOrderMapper;
import org.supermarket.modules.stats.dto.MonthlyStatsVO;
import org.supermarket.modules.stats.dto.StatsVO;

import org.springframework.ai.chat.client.ChatClient;
import jakarta.servlet.http.HttpServletResponse;
import org.supermarket.modules.stats.service.StatsService;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StatsServiceImpl implements StatsService {


    private final SaleOrderMapper saleOrderMapper;
    private final SaleOrderItemMapper saleOrderItemMapper;
    private final InventoryMapper inventoryMapper;
    private final MemberMapper memberMapper;
    private final ProductMapper productMapper;
    private final ProductCategoryMapper categoryMapper;
    private final ChatClient chatClient;

    // ---- 构造器（替代 @RequiredArgsConstructor）----
    public StatsServiceImpl(SaleOrderMapper saleOrderMapper,
                            SaleOrderItemMapper saleOrderItemMapper,
                            InventoryMapper inventoryMapper,
                            MemberMapper memberMapper,
                            ProductMapper productMapper,
                            ProductCategoryMapper categoryMapper,
                            ChatClient.Builder chatClientBuilder) {
        this.saleOrderMapper = saleOrderMapper;
        this.saleOrderItemMapper = saleOrderItemMapper;
        this.inventoryMapper = inventoryMapper;
        this.memberMapper = memberMapper;
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
        this.chatClient = chatClientBuilder.build();
    }


    @Override
    public StatsVO getDashboard(Long storeId, String startDate, String endDate) {
        StatsVO vo = new StatsVO();

        // 今日时间范围
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        // ---- 今日概览（固定今日，不受时间筛选影响）----
        List<SaleOrder> todayOrders = queryOrders(storeId, todayStart, todayEnd);
        BigDecimal todaySaleAmount = sumAmount(todayOrders);
        vo.setTodaySaleAmount(todaySaleAmount);
        vo.setTodayOrderCount(todayOrders.size());
        vo.setTodayAvgAmount(todayOrders.isEmpty() ? BigDecimal.ZERO :
                todaySaleAmount.divide(new BigDecimal(todayOrders.size()), 2, RoundingMode.HALF_UP));

        Long todayMemberCount = memberMapper.selectCount(
                new LambdaQueryWrapper<Member>()
                        .ge(Member::getCreateTime, todayStart)
                        .lt(Member::getCreateTime, todayEnd));
        vo.setTodayMemberCount(todayMemberCount.intValue());

        // 库存预警
        int warningCount = storeId != null ? inventoryMapper.selectWarningList(storeId).size() : 0;
        vo.setWarningProductCount(warningCount);

        // ---- 解析时间范围 ----
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime rangeStart;
        LocalDateTime rangeEnd = LocalDate.now().plusDays(1).atStartOfDay();

        if (startDate != null && endDate != null) {
            rangeStart = LocalDate.parse(startDate, dateFormatter).atStartOfDay();
            rangeEnd = LocalDate.parse(endDate, dateFormatter).plusDays(1).atStartOfDay();
        } else {
            // 默认近7天
            rangeStart = LocalDate.now().minusDays(6).atStartOfDay();
        }

        // ---- 趋势折线图 ----
        List<SaleOrder> rangeOrders = queryOrders(storeId, rangeStart, rangeEnd);
        List<StatsVO.DailyStats> dailyStatsList = buildDailyStats(rangeStart, rangeEnd, rangeOrders);
        vo.setLast7Days(dailyStatsList);

        // ---- TOP5商品 ----
        List<StatsVO.ProductRank> topProducts = buildTopProducts(storeId, rangeOrders);
        vo.setTopProducts(topProducts);

        // ---- 分类销售占比饼图 ----
        List<StatsVO.CategoryStats> categoryStats = buildCategoryStats(rangeOrders);
        vo.setCategoryStats(categoryStats);

        return vo;
    }

    // 查询订单
    private List<SaleOrder> queryOrders(Long storeId, LocalDateTime start, LocalDateTime end) {
        return saleOrderMapper.selectList(new LambdaQueryWrapper<SaleOrder>()
                .eq(storeId != null, SaleOrder::getStoreId, storeId)
                .eq(SaleOrder::getStatus, 1)
                .ge(SaleOrder::getCreateTime, start)
                .lt(SaleOrder::getCreateTime, end));
    }

    // 计算总金额
    private BigDecimal sumAmount(List<SaleOrder> orders) {
        return orders.stream()
                .map(SaleOrder::getPayableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // 构建每日统计
    private List<StatsVO.DailyStats> buildDailyStats(LocalDateTime start, LocalDateTime end, List<SaleOrder> orders) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        List<StatsVO.DailyStats> list = new ArrayList<>();

        // 按日期分组订单
        Map<String, List<SaleOrder>> ordersByDay = orders.stream()
                .collect(Collectors.groupingBy(o ->
                        o.getCreateTime().format(formatter)));

        LocalDate current = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();
        while (!current.isAfter(endDate) && !current.isAfter(LocalDate.now())) {
            String dateKey = current.format(formatter);
            List<SaleOrder> dayOrders = ordersByDay.getOrDefault(dateKey, Collections.emptyList());

            StatsVO.DailyStats daily = new StatsVO.DailyStats();
            daily.setDate(dateKey);
            daily.setAmount(sumAmount(dayOrders));
            daily.setOrderCount(dayOrders.size());
            list.add(daily);
            current = current.plusDays(1);
        }
        return list;
    }

    // 构建TOP5
    private List<StatsVO.ProductRank> buildTopProducts(Long storeId, List<SaleOrder> orders) {
        if (orders.isEmpty()) return new ArrayList<>();
        List<Long> orderIds = orders.stream().map(SaleOrder::getId).collect(Collectors.toList());
        List<SaleOrderItem> items = saleOrderItemMapper.selectList(
                new LambdaQueryWrapper<SaleOrderItem>().in(SaleOrderItem::getOrderId, orderIds));

        return items.stream()
                .collect(Collectors.groupingBy(SaleOrderItem::getProductId))
                .entrySet().stream()
                .map(entry -> {
                    StatsVO.ProductRank rank = new StatsVO.ProductRank();
                    rank.setProductId(entry.getKey());
                    rank.setProductName(entry.getValue().get(0).getProductName());
                    rank.setTotalQty(entry.getValue().stream()
                            .map(SaleOrderItem::getQuantity)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                    rank.setTotalAmount(entry.getValue().stream()
                            .map(SaleOrderItem::getSubtotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                    return rank;
                })
                .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                .limit(5)
                .collect(Collectors.toList());
    }

    // 构建分类占比
    private List<StatsVO.CategoryStats> buildCategoryStats(List<SaleOrder> orders) {
        if (orders.isEmpty()) return new ArrayList<>();

        List<Long> orderIds = orders.stream().map(SaleOrder::getId).collect(Collectors.toList());
        List<SaleOrderItem> items = saleOrderItemMapper.selectList(
                new LambdaQueryWrapper<SaleOrderItem>().in(SaleOrderItem::getOrderId, orderIds));

        if (items.isEmpty()) return new ArrayList<>();

        // 查商品分类
        List<Long> productIds = items.stream()
                .map(SaleOrderItem::getProductId).distinct().collect(Collectors.toList());
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>().in(Product::getId, productIds));
        Map<Long, Long> productCategoryMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Product::getCategoryId));

        // 查一级分类
        List<ProductCategory> allCategories = categoryMapper.selectList(null);
        Map<Long, String> categoryNameMap = allCategories.stream()
                .collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getCategoryName));
        // 子分类 -> 一级分类
        Map<Long, Long> subToTopMap = allCategories.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() != 0)
                .collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getParentId));

        // 按一级分类汇总金额
        Map<String, BigDecimal> categoryAmountMap = new LinkedHashMap<>();
        items.forEach(item -> {
            Long categoryId = productCategoryMap.get(item.getProductId());
            if (categoryId == null) return;
            // 如果是二级分类，找到一级分类
            Long topCategoryId = subToTopMap.getOrDefault(categoryId, categoryId);
            String catName = categoryNameMap.getOrDefault(topCategoryId, "其他");
            categoryAmountMap.merge(catName, item.getSubtotal(), BigDecimal::add);
        });

        return categoryAmountMap.entrySet().stream()
                .map(e -> {
                    StatsVO.CategoryStats cs = new StatsVO.CategoryStats();
                    cs.setCategoryName(e.getKey());
                    cs.setAmount(e.getValue());
                    return cs;
                })
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .collect(Collectors.toList());
    }

    @Override
    public MonthlyStatsVO getLastMonthStats(Long storeId) {
        LocalDate firstDayOfLastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        LocalDate lastDayOfLastMonth = firstDayOfLastMonth.withDayOfMonth(firstDayOfLastMonth.lengthOfMonth());
        LocalDateTime start = firstDayOfLastMonth.atStartOfDay();
        LocalDateTime end = lastDayOfLastMonth.plusDays(1).atStartOfDay();

        LocalDate firstDayOf2MonthsAgo = LocalDate.now().minusMonths(2).withDayOfMonth(1);
        LocalDate lastDayOf2MonthsAgo = firstDayOf2MonthsAgo.withDayOfMonth(firstDayOf2MonthsAgo.lengthOfMonth());
        LocalDateTime start2 = firstDayOf2MonthsAgo.atStartOfDay();
        LocalDateTime end2 = lastDayOf2MonthsAgo.plusDays(1).atStartOfDay();

        List<SaleOrder> lastMonthOrders = queryOrders(storeId, start, end);
        List<SaleOrder> twoMonthsAgoOrders = queryOrders(storeId, start2, end2);

        BigDecimal totalAmount = sumAmount(lastMonthOrders);
        BigDecimal totalAmountLastMonth = sumAmount(twoMonthsAgoOrders);

        String growthRate = "暂无环比数据";
        if (totalAmountLastMonth.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal rate = totalAmount.subtract(totalAmountLastMonth)
                    .divide(totalAmountLastMonth, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            growthRate = (rate.compareTo(BigDecimal.ZERO) > 0 ? "+" : "") +
                    rate.setScale(1, RoundingMode.HALF_UP) + "%";
        }

        Long newMembers = memberMapper.selectCount(
                new LambdaQueryWrapper<Member>()
                        .ge(Member::getCreateTime, start)
                        .lt(Member::getCreateTime, end));

        MonthlyStatsVO vo = new MonthlyStatsVO();
        vo.setMonth(firstDayOfLastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")));
        vo.setTotalAmount(totalAmount);
        vo.setTotalOrders(lastMonthOrders.size());
        vo.setAvgOrderAmount(lastMonthOrders.isEmpty() ? BigDecimal.ZERO :
                totalAmount.divide(new BigDecimal(lastMonthOrders.size()), 2, RoundingMode.HALF_UP));
        vo.setNewMembers(newMembers.intValue());
        vo.setTotalAmountLastMonth(totalAmountLastMonth);
        vo.setGrowthRate(growthRate);
        vo.setTopProducts(buildTopProducts(storeId, lastMonthOrders));
        vo.setCategoryStats(buildCategoryStats(lastMonthOrders));
        return vo;
    }

    @Override
    public void generateReport(Long storeId, HttpServletResponse response) throws Exception {
        MonthlyStatsVO stats = getLastMonthStats(storeId);

        StringBuilder top5 = new StringBuilder();
        if (stats.getTopProducts() != null) {
            stats.getTopProducts().forEach(p ->
                    top5.append("  - ").append(p.getProductName())
                            .append("：¥").append(p.getTotalAmount())
                            .append("，销量").append(p.getTotalQty()).append("\n"));
        }

        StringBuilder categories = new StringBuilder();
        if (stats.getCategoryStats() != null) {
            stats.getCategoryStats().forEach(c ->
                    categories.append("  - ").append(c.getCategoryName())
                            .append("：¥").append(c.getAmount()).append("\n"));
        }

        String prompt = String.format("""
            你是一位专业的零售行业数据分析师。请根据以下超市%s月的经营数据，生成一份专业、简洁的运营月报。
            
            【基础数据】
            - 月份：%s
            - 总销售额：¥%s（环比%s）
            - 订单总数：%d单
            - 客单价：¥%s
            - 新增会员：%d人
            
            【热销商品TOP5】
            %s
            【分类销售情况】
            %s
            
            请按以下结构生成报告：
            1. 本月经营概况（3-4句总结）
            2. 销售亮点分析
            3. 存在的问题或风险
            4. 下月运营建议（3条具体建议）
            
            要求：语言专业简洁，每个部分不超过150字。
            """,
                stats.getMonth(), stats.getMonth(),
                stats.getTotalAmount(), stats.getGrowthRate(),
                stats.getTotalOrders(), stats.getAvgOrderAmount(),
                stats.getNewMembers(), top5, categories
        );

        response.setContentType("text/event-stream;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Access-Control-Allow-Origin", "*");

        var writer = response.getWriter();
        chatClient.prompt()
                .user(prompt)
                .stream()
                .chatResponse()
                .doOnNext(chunk -> {
                    try {
                        String text = chunk.getResult().getOutput().getContent();
                        if (text != null && !text.isEmpty()) {
                            writer.write("data: " + text.replace("\n", "\\n") + "\n\n");
                            writer.flush();
                        }
                    } catch (Exception e) {
                        log.error("流式输出错误", e);
                    }
                })
                .doOnComplete(() -> {
                    try {
                        writer.write("data: [DONE]\n\n");
                        writer.flush();
                    } catch (Exception e) {
                        log.error("流式完成错误", e);
                    }
                })
                .blockLast();
    }
}