package org.supermarket.modules.sale.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.supermarket.modules.sale.dto.SaleOrderDTO;
import org.supermarket.modules.sale.entity.SaleOrder;

public interface SaleOrderService extends IService<SaleOrder> {

    // 创建销售订单（收银）
    SaleOrder createOrder(SaleOrderDTO dto);

    // 退货
    void returnOrder(String originalOrderNo, String reason);

    // 分页查询
    Page<SaleOrder> pageOrder(int pageNum, int pageSize, Long storeId);
}