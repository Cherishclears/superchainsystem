package org.supermarket.modules.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.supermarket.common.result.Result;
import org.supermarket.modules.purchase.dto.AiSuggestVO;
import org.supermarket.modules.purchase.dto.PurchaseOrderDTO;
import org.supermarket.modules.purchase.entity.PurchaseOrder;
import org.supermarket.modules.purchase.service.PurchaseOrderService;

import java.util.List;

@RestController
@RequestMapping("/purchase")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    // 分页查询
    @GetMapping("/page")
    public Result<Page<PurchaseOrder>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Integer status) {
        return Result.ok(purchaseOrderService.pageOrder(pageNum, pageSize, storeId, status));
    }

    // 创建采购单
    @PostMapping
    public Result<String> create(@Valid @RequestBody PurchaseOrderDTO dto) {
        purchaseOrderService.createOrder(dto);
        return Result.ok("采购单创建成功");
    }

    // 提交审核
    @PutMapping("/{id}/submit")
    public Result<String> submit(@PathVariable Long id) {
        purchaseOrderService.submitOrder(id);
        return Result.ok("提交成功");
    }

    // 审核通过
    @PutMapping("/{id}/approve")
    public Result<String> approve(@PathVariable Long id) {
        purchaseOrderService.approveOrder(id);
        return Result.ok("审核通过");
    }

    // 确认入库
    @PutMapping("/{id}/receive")
    public Result<String> receive(@PathVariable Long id) {
        purchaseOrderService.receiveOrder(id);
        return Result.ok("入库成功");
    }

    // 取消采购单
    @PutMapping("/{id}/cancel")
    public Result<String> cancel(@PathVariable Long id) {
        purchaseOrderService.cancelOrder(id);
        return Result.ok("取消成功");
    }
    // AI 智能补货建议
    @GetMapping("/ai-suggest")
    public Result<List<AiSuggestVO>> aiSuggest(@RequestParam Long storeId) {
        return Result.ok(purchaseOrderService.aiSuggest(storeId));
    }
}
