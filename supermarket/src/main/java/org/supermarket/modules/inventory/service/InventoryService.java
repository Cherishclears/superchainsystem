package org.supermarket.modules.inventory.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.supermarket.modules.inventory.entity.Inventory;

import java.math.BigDecimal;
import java.util.List;

public interface InventoryService extends IService<Inventory> {

    // 入库（采购收货时调用）
    void inbound(Long storeId, Long productId, BigDecimal qty, String refOrderNo);

    // 出库（销售时调用）
    void outbound(Long storeId, Long productId, BigDecimal qty, String refOrderNo);

    // 查询门店全部库存
    List<Inventory> listByStore(Long storeId);

    // 查询库存预警商品
    List<Inventory> listWarning(Long storeId);

    // 盘点调整库存
    void adjust(Long storeId, Long productId, BigDecimal newQty, String remark);

    // 设置预警下限
    void updateWarningQty(Long storeId, Long productId, BigDecimal warningQty);
}