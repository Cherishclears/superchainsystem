package org.supermarket.modules.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.supermarket.modules.inventory.dto.TransferOrderDTO;
import org.supermarket.modules.inventory.entity.TransferOrder;
import org.supermarket.modules.inventory.entity.TransferOrderItem;

import java.util.List;

public interface TransferOrderService extends IService<TransferOrder> {

    // 创建调拨单
    void create(TransferOrderDTO dto);

    // 提交审核
    void submit(Long id);

    // 审核通过
    void approve(Long id);

    // 确认发货（出库门店库存扣减）
    void ship(Long id);

    // 确认收货（入库门店库存增加）
    void receive(Long id);

    // 拒收（回滚出库门店库存）
    void reject(Long id);

    // 分页查询
    Page<TransferOrder> pageQuery(int pageNum, int pageSize,
                                  Long fromStoreId, Long toStoreId, Integer status);

    // 查询明细
    List<TransferOrderItem> getItems(Long transferId);
}