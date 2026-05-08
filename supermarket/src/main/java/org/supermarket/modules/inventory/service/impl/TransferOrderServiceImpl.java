package org.supermarket.modules.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.supermarket.common.exception.BusinessException;
import org.supermarket.common.utils.SecurityUtils;
import org.supermarket.modules.inventory.dto.TransferOrderDTO;
import org.supermarket.modules.inventory.entity.TransferOrder;
import org.supermarket.modules.inventory.entity.TransferOrderItem;
import org.supermarket.modules.inventory.mapper.TransferOrderItemMapper;
import org.supermarket.modules.inventory.mapper.TransferOrderMapper;
import org.supermarket.modules.inventory.service.InventoryService;
import org.supermarket.modules.inventory.service.TransferOrderService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransferOrderServiceImpl extends ServiceImpl<TransferOrderMapper, TransferOrder>
        implements TransferOrderService {

    private final TransferOrderItemMapper itemMapper;
    private final InventoryService inventoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(TransferOrderDTO dto) {
        // 生成调拨单号
        String transferNo = "TF" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + (int)(Math.random() * 1000);

        TransferOrder order = new TransferOrder();
        order.setTransferNo(transferNo);
        order.setFromStoreId(dto.getFromStoreId());
        order.setToStoreId(dto.getToStoreId());
        order.setStatus(0);
        order.setRemark(dto.getRemark());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        save(order);

        // 保存明细
        List<TransferOrderItem> items = dto.getItems().stream().map(i -> {
            TransferOrderItem item = new TransferOrderItem();
            item.setTransferId(order.getId());
            item.setProductId(i.getProductId());
            item.setProductName(i.getProductName());
            item.setQuantity(i.getQuantity());
            item.setCreateTime(LocalDateTime.now());
            return item;
        }).collect(Collectors.toList());

        items.forEach(itemMapper::insert);
    }

    @Override
    public void submit(Long id) {
        updateStatus(id, 0, 1);
    }

    @Override
    public void approve(Long id) {
        // 只有总部能审核
        if (!SecurityUtils.isHQ()) {
            throw new BusinessException("无权限，仅总部管理员可审核");
        }
        updateStatus(id, 1, 2);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ship(Long id) {
        TransferOrder order = getAndCheck(id, 2);

        // 只有出库门店的账号能发货
        if (SecurityUtils.isHQ()) {
            throw new BusinessException("无权限，请使用出库门店账号操作");
        }
        Long currentStoreId = SecurityUtils.getCurrentStoreId();
        if (!order.getFromStoreId().equals(currentStoreId)) {
            throw new BusinessException("无权限，只有出库门店账号才能发货");
        }

        List<TransferOrderItem> items = getItems(id);
        items.forEach(item ->
                inventoryService.outbound(order.getFromStoreId(), item.getProductId(),
                        item.getQuantity(), order.getTransferNo())
        );
        updateStatus(id, 2, 3);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receive(Long id) {
        TransferOrder order = getAndCheck(id, 3);

        // 只有入库门店的账号能收货
        if (SecurityUtils.isHQ()) {
            throw new BusinessException("无权限，请使用入库门店账号操作");
        }
        Long currentStoreId = SecurityUtils.getCurrentStoreId();
        if (!order.getToStoreId().equals(currentStoreId)) {
            throw new BusinessException("无权限，只有入库门店账号才能收货");
        }

        List<TransferOrderItem> items = getItems(id);
        items.forEach(item ->
                inventoryService.inbound(order.getToStoreId(), item.getProductId(),
                        item.getQuantity(), order.getTransferNo())
        );
        updateStatus(id, 3, 4);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id) {
        TransferOrder order = getAndCheck(id, 3);

        // 只有入库门店的账号能拒收
        if (SecurityUtils.isHQ()) {
            throw new BusinessException("无权限，请使用入库门店账号操作");
        }
        Long currentStoreId = SecurityUtils.getCurrentStoreId();
        if (!order.getToStoreId().equals(currentStoreId)) {
            throw new BusinessException("无权限，只有入库门店账号才能拒收");
        }

        List<TransferOrderItem> items = getItems(id);
        items.forEach(item ->
                inventoryService.inbound(order.getFromStoreId(), item.getProductId(),
                        item.getQuantity(), order.getTransferNo())
        );
        updateStatus(id, 3, 5);
    }

    @Override
    public Page<TransferOrder> pageQuery(int pageNum, int pageSize,
                                         Long fromStoreId, Long toStoreId, Integer status) {
        LambdaQueryWrapper<TransferOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(fromStoreId != null, TransferOrder::getFromStoreId, fromStoreId);
        wrapper.eq(toStoreId != null, TransferOrder::getToStoreId, toStoreId);
        wrapper.eq(status != null, TransferOrder::getStatus, status);
        wrapper.orderByDesc(TransferOrder::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<TransferOrderItem> getItems(Long transferId) {
        return itemMapper.selectList(new LambdaQueryWrapper<TransferOrderItem>()
                .eq(TransferOrderItem::getTransferId, transferId));
    }

    private void updateStatus(Long id, int expectedStatus, int newStatus) {
        TransferOrder order = getAndCheck(id, expectedStatus);
        order.setStatus(newStatus);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
    }

    private TransferOrder getAndCheck(Long id, int expectedStatus) {
        TransferOrder order = getById(id);
        if (order == null) throw new BusinessException("调拨单不存在");
        if (order.getStatus() != expectedStatus) throw new BusinessException("当前状态不允许此操作");
        return order;
    }
}