package org.supermarket.modules.inventory.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.supermarket.common.result.Result;
import org.supermarket.modules.inventory.entity.Inventory;
import org.supermarket.modules.inventory.service.InventoryService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // 查询门店库存
    @GetMapping("/list/{storeId}")
    public Result<List<Inventory>> list(@PathVariable Long storeId) {
        return Result.ok(inventoryService.listByStore(storeId));
    }

    // 查询库存预警
    @GetMapping("/warning/{storeId}")
    public Result<List<Inventory>> warning(@PathVariable Long storeId) {
        return Result.ok(inventoryService.listWarning(storeId));
    }

    // 入库
    @PostMapping("/inbound")
    public Result<String> inbound(@RequestParam Long storeId,
                                  @RequestParam Long productId,
                                  @RequestParam BigDecimal qty,
                                  @RequestParam(required = false) String refOrderNo) {
        inventoryService.inbound(storeId, productId, qty, refOrderNo);
        return Result.ok("入库成功");
    }

    // 出库
    @PostMapping("/outbound")
    public Result<String> outbound(@RequestParam Long storeId,
                                   @RequestParam Long productId,
                                   @RequestParam BigDecimal qty,
                                   @RequestParam(required = false) String refOrderNo) {
        inventoryService.outbound(storeId, productId, qty, refOrderNo);
        return Result.ok("出库成功");
    }

    // 盘点调整
    @PostMapping("/adjust")
    public Result<String> adjust(@RequestParam Long storeId,
                                 @RequestParam Long productId,
                                 @RequestParam BigDecimal newQty,
                                 @RequestParam(required = false) String remark) {
        inventoryService.adjust(storeId, productId, newQty, remark);
        return Result.ok("调整成功");
    }

    // 设置预警下限
    @PutMapping("/warning")
    public Result<String> updateWarning(@RequestParam Long storeId,
                                        @RequestParam Long productId,
                                        @RequestParam BigDecimal warningQty) {
        inventoryService.updateWarningQty(storeId, productId, warningQty);
        return Result.ok("设置成功");
    }


}
