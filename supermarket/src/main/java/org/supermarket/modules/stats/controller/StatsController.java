package org.supermarket.modules.stats.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.supermarket.common.result.Result;
import org.supermarket.modules.stats.dto.MonthlyStatsVO;
import org.supermarket.modules.stats.dto.StatsVO;
import org.supermarket.modules.stats.service.StatsService;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    // 首页数据看板
    @GetMapping("/dashboard")
    public Result<StatsVO> dashboard(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(statsService.getDashboard(storeId, startDate, endDate));
    }

    @GetMapping("/last-month")
    public Result<MonthlyStatsVO> lastMonth(@RequestParam(required = false) Long storeId) {
        return Result.ok(statsService.getLastMonthStats(storeId));
    }

    @GetMapping("/report")
    public void generateReport(
            @RequestParam(required = false) Long storeId,
            HttpServletResponse response) throws Exception {
        statsService.generateReport(storeId, response);
    }
}
