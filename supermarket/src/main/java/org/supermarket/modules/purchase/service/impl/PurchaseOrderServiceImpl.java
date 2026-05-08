package org.supermarket.modules.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.supermarket.common.exception.BusinessException;
import org.supermarket.modules.inventory.entity.Inventory;
import org.supermarket.modules.inventory.mapper.InventoryMapper;
import org.supermarket.modules.inventory.service.InventoryService;
import org.supermarket.modules.product.entity.Product;
import org.supermarket.modules.product.mapper.ProductMapper;
import org.supermarket.modules.purchase.dto.AiSuggestVO;
import org.supermarket.modules.purchase.dto.PurchaseOrderDTO;
import org.supermarket.modules.purchase.entity.PurchaseOrder;
import org.supermarket.modules.purchase.entity.PurchaseOrderItem;
import org.supermarket.modules.purchase.mapper.PurchaseOrderItemMapper;
import org.supermarket.modules.purchase.mapper.PurchaseOrderMapper;
import org.supermarket.modules.purchase.service.PurchaseOrderService;
import org.supermarket.modules.sale.entity.SaleOrder;
import org.supermarket.modules.sale.entity.SaleOrderItem;
import org.supermarket.modules.sale.mapper.SaleOrderItemMapper;
import org.supermarket.modules.sale.mapper.SaleOrderMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder>
        implements PurchaseOrderService {

    // ---- 原有字段（不动）----
    private final PurchaseOrderItemMapper itemMapper;
    private final InventoryService inventoryService;

    // ---- 新增字段 ----
    private final InventoryMapper inventoryMapper;
    private final SaleOrderMapper saleOrderMapper;
    private final SaleOrderItemMapper saleOrderItemMapper;
    private final ProductMapper productMapper;
    private final ChatClient.Builder chatClientBuilder;

    // ---- 原有方法（全部不动）----

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(PurchaseOrderDTO dto) {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNo("PO" + System.currentTimeMillis());
        order.setStoreId(dto.getStoreId());
        order.setSupplierId(dto.getSupplierId());
        order.setExpectedDate(dto.getExpectedDate());
        order.setRemark(dto.getRemark());
        order.setStatus(0);

        BigDecimal totalAmount = dto.getItems().stream()
                .map(item -> item.getPurchasePrice().multiply(item.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);
        save(order);

        List<PurchaseOrderItem> items = dto.getItems().stream().map(itemDTO -> {
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setOrderId(order.getId());
            item.setProductId(itemDTO.getProductId());
            item.setPurchasePrice(itemDTO.getPurchasePrice());
            item.setQuantity(itemDTO.getQuantity());
            item.setReceivedQty(BigDecimal.ZERO);
            item.setSubtotal(itemDTO.getPurchasePrice().multiply(itemDTO.getQuantity()));
            item.setExpireDate(itemDTO.getExpireDate());
            item.setRemark(itemDTO.getRemark());
            return item;
        }).collect(Collectors.toList());

        items.forEach(itemMapper::insert);
    }

    @Override
    public void submitOrder(Long orderId) {
        PurchaseOrder order = getAndCheckStatus(orderId, 0, "只有草稿状态的采购单才能提交");
        order.setStatus(1);
        updateById(order);
    }

    @Override
    public void approveOrder(Long orderId) {
        PurchaseOrder order = getAndCheckStatus(orderId, 1, "只有待审核的采购单才能审核");
        order.setStatus(2);
        order.setApproveTime(LocalDateTime.now());
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receiveOrder(Long orderId) {
        PurchaseOrder order = getAndCheckStatus(orderId, 2, "只有已审核的采购单才能入库");

        List<PurchaseOrderItem> items = itemMapper.selectByOrderId(orderId);
        items.forEach(item -> {
            inventoryService.inbound(
                    order.getStoreId(),
                    item.getProductId(),
                    item.getQuantity(),
                    order.getOrderNo());
            item.setReceivedQty(item.getQuantity());
            itemMapper.updateById(item);
        });

        order.setStatus(3);
        order.setActualDate(LocalDate.now());
        updateById(order);
    }

    @Override
    public void cancelOrder(Long orderId) {
        PurchaseOrder order = getById(orderId);
        if (order == null) throw new BusinessException("采购单不存在");
        if (order.getStatus() == 3) throw new BusinessException("已入库的采购单不能取消");
        order.setStatus(4);
        updateById(order);
    }

    @Override
    public Page<PurchaseOrder> pageOrder(int pageNum, int pageSize,
                                         Long storeId, Integer status) {
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(storeId != null, PurchaseOrder::getStoreId, storeId);
        wrapper.eq(status != null, PurchaseOrder::getStatus, status);
        wrapper.orderByDesc(PurchaseOrder::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    // ---- 新增：AI 智能补货 ----

    @Override
    public List<AiSuggestVO> aiSuggest(Long storeId) {
        // 1. 查询该门店所有库存
        List<Inventory> inventories = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getStoreId, storeId));
        if (inventories.isEmpty()) {
            throw new BusinessException("该门店暂无库存数据，无法生成补货建议");
        }

        // 2. 查询近7天该门店的销售订单
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<SaleOrder> recentOrders = saleOrderMapper.selectList(
                new LambdaQueryWrapper<SaleOrder>()
                        .eq(SaleOrder::getStoreId, storeId)
                        .eq(SaleOrder::getStatus, 1)
                        .ge(SaleOrder::getCreateTime, sevenDaysAgo));

        // 3. 统计每个商品近7天销量
        Map<Long, BigDecimal> soldQtyMap = new HashMap<>();
        if (!recentOrders.isEmpty()) {
            List<Long> orderIds = recentOrders.stream()
                    .map(SaleOrder::getId).collect(Collectors.toList());
            List<SaleOrderItem> saleItems = saleOrderItemMapper.selectList(
                    new LambdaQueryWrapper<SaleOrderItem>()
                            .in(SaleOrderItem::getOrderId, orderIds));
            saleItems.forEach(item ->
                    soldQtyMap.merge(item.getProductId(), item.getQuantity(), BigDecimal::add));
        }

        // 4. 查询商品名称
        List<Long> productIds = inventories.stream()
                .map(Inventory::getProductId).collect(Collectors.toList());
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>().in(Product::getId, productIds));
        Map<Long, String> productNameMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Product::getProductName));

        // 5. 拼装给 AI 的数据描述
        StringBuilder dataDesc = new StringBuilder();
        for (Inventory inv : inventories) {
            String productName = productNameMap.getOrDefault(
                    inv.getProductId(), "商品" + inv.getProductId());
            BigDecimal sold7 = soldQtyMap.getOrDefault(inv.getProductId(), BigDecimal.ZERO);
            dataDesc.append(String.format(
                    "商品ID:%d 商品名:%s 当前库存:%.3f 预警下限:%.3f 近7天销量:%.3f\n",
                    inv.getProductId(), productName,
                    inv.getQuantity(), inv.getWarningQty(), sold7));
        }


        log.info("发送给AI的库存数据：\n{}", dataDesc);

        // 6. 拼装 Prompt
        String prompt = String.format("""
                你是一位专业的零售供应链分析师。请根据以下门店库存和销售数据，分析哪些商品需要补货。
                
                【门店库存与销售数据】
                %s
                
                【补货判断标准】
                        1. 当前库存低于预警下限的商品，无论销量如何，必须补货
                        2. 近7天有销量，且当前库存不足近7天销量的3倍，需要补货
                        3. 建议采购数量计算规则（按优先级）：
                           - 若近7天销量 > 0：建议数量 = 近7天销量 × 3
                           - 若近7天销量 = 0 但库存低于预警下限：建议数量 = 预警下限 * 2
                           - 最终建议数量取上述结果和预警下限3倍中的较大值
                        4. 库存充足（高于预警下限3倍）且近期销量很低的商品不需要补货
                
                【返回格式要求】
                只返回JSON数组，不要有任何其他文字、解释或markdown代码块，格式如下：
                [{"productId":1,"productName":"商品名","suggestQty":100,"reason":"补货理由"}]
                
                如果所有商品库存都充足，返回空数组：[]
                """, dataDesc);

        // 7. 调用 AI
        ChatClient chatClient = chatClientBuilder.build();
        String aiResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.info("AI补货建议原始返回：{}", aiResponse);

        // 8. 解析 AI 返回的 JSON
        try {
            String cleanJson = aiResponse.trim()
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> aiList = objectMapper.readValue(
                    cleanJson, new TypeReference<List<Map<String, Object>>>() {});

            // 9. 组装返回结果，补充库存和销量信息
            Map<Long, Inventory> invMap = inventories.stream()
                    .collect(Collectors.toMap(Inventory::getProductId, i -> i));

            return aiList.stream().map(item -> {
                AiSuggestVO vo = new AiSuggestVO();
                Long productId = Long.valueOf(item.get("productId").toString());
                vo.setProductId(productId);
                vo.setProductName(item.get("productName").toString());
                vo.setSuggestQty(new BigDecimal(item.get("suggestQty").toString()));
                vo.setReason(item.getOrDefault("reason", "").toString());

                Inventory inv = invMap.get(productId);
                if (inv != null) {
                    vo.setCurrentQty(inv.getQuantity());
                    vo.setWarningQty(inv.getWarningQty());
                }
                vo.setSoldQty7Days(soldQtyMap.getOrDefault(productId, BigDecimal.ZERO));
                return vo;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("AI补货建议解析失败，原始返回：{}", aiResponse, e);
            throw new BusinessException("AI分析失败，请稍后重试");
        }
    }

    // ---- 私有方法（不动）----

    private PurchaseOrder getAndCheckStatus(Long orderId, int expectedStatus, String errorMsg) {
        PurchaseOrder order = getById(orderId);
        if (order == null) throw new BusinessException("采购单不存在");
        if (order.getStatus() != expectedStatus) throw new BusinessException(errorMsg);
        return order;
    }
}