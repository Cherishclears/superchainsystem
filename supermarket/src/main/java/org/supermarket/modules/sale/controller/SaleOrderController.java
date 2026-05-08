package org.supermarket.modules.sale.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.supermarket.common.result.Result;
import org.supermarket.modules.sale.dto.SaleOrderDTO;
import org.supermarket.modules.sale.entity.SaleOrder;
import org.supermarket.modules.sale.service.SaleOrderService;

@RestController
@RequestMapping("/sale")
@RequiredArgsConstructor
public class SaleOrderController {

    private final SaleOrderService saleOrderService;

    // 收银下单
    // 把前端传来的JSON数据转成SaleOrderDTO对象，然后检查这个对象里的数据是否合法，最后把这个验证过的对象赋值给变量dto
    @PostMapping("/order")
    public Result<SaleOrder> createOrder(@Valid @RequestBody SaleOrderDTO dto) {
        return Result.ok(saleOrderService.createOrder(dto));
    }

    // 分页查询订单
    @GetMapping("/order/page")
    public Result<Page<SaleOrder>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long storeId) {
        return Result.ok(saleOrderService.pageOrder(pageNum, pageSize, storeId));
    }

    // 退货
    @PostMapping("/return")
    public Result<String> returnOrder(
            @RequestParam String originalOrderNo,
            @RequestParam(required = false) String reason) {
        saleOrderService.returnOrder(originalOrderNo, reason);
        return Result.ok("退货成功");
    }
}
