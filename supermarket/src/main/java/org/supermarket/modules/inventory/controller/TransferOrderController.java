package org.supermarket.modules.inventory.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.supermarket.common.result.Result;
import org.supermarket.modules.inventory.dto.TransferOrderDTO;
import org.supermarket.modules.inventory.entity.TransferOrder;
import org.supermarket.modules.inventory.entity.TransferOrderItem;
import org.supermarket.modules.inventory.service.TransferOrderService;

import java.util.List;

@RestController
@RequestMapping("/transfer")
@RequiredArgsConstructor
public class TransferOrderController {

    private final TransferOrderService transferService;

    @GetMapping("/page")
    public Result<?> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long fromStoreId,
            @RequestParam(required = false) Long toStoreId,
            @RequestParam(required = false) Integer status) {
        return Result.ok(transferService.pageQuery(pageNum, pageSize, fromStoreId, toStoreId, status));
    }

    @PostMapping
    public Result<String> create(@RequestBody TransferOrderDTO dto) {
        transferService.create(dto);
        return Result.ok("创建成功");
    }

    @PutMapping("/{id}/submit")
    public Result<String> submit(@PathVariable Long id) {
        transferService.submit(id);
        return Result.ok("提交成功");
    }

    @PutMapping("/{id}/approve")
    public Result<String> approve(@PathVariable Long id) {
        transferService.approve(id);
        return Result.ok("审核通过");
    }

    @PutMapping("/{id}/ship")
    public Result<String> ship(@PathVariable Long id) {
        transferService.ship(id);
        return Result.ok("发货成功");
    }

    @PutMapping("/{id}/receive")
    public Result<String> receive(@PathVariable Long id) {
        transferService.receive(id);
        return Result.ok("收货成功");
    }

    @PutMapping("/{id}/reject")
    public Result<String> reject(@PathVariable Long id) {
        transferService.reject(id);
        return Result.ok("已拒收");
    }

    @GetMapping("/{id}/items")
    public Result<List<TransferOrderItem>> items(@PathVariable Long id) {
        return Result.ok(transferService.getItems(id));
    }
}