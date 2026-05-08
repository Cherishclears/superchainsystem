package org.supermarket.modules.stats.service;

import org.supermarket.modules.stats.dto.MonthlyStatsVO;
import org.supermarket.modules.stats.dto.StatsVO;

public interface StatsService {

    // 获取首页统计数据（今日概览 + 近7天 + TOP5）
    StatsVO getDashboard(Long storeId, String startDate, String endDate);

    MonthlyStatsVO getLastMonthStats(Long storeId);

    void generateReport(Long storeId, jakarta.servlet.http.HttpServletResponse response) throws Exception;


}