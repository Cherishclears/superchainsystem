package org.supermarket.modules.sale.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.supermarket.common.exception.BusinessException;
import org.supermarket.modules.inventory.service.InventoryService;
import org.supermarket.modules.member.entity.Member;
import org.supermarket.modules.member.entity.MemberLevel;
import org.supermarket.modules.member.mapper.MemberLevelMapper;
import org.supermarket.modules.member.service.MemberService;
import org.supermarket.modules.product.entity.Product;
import org.supermarket.modules.product.service.ProductService;
import org.supermarket.modules.product.service.StoreProductConfigService;
import org.supermarket.modules.sale.dto.SaleOrderDTO;
import org.supermarket.modules.sale.entity.ReturnOrder;
import org.supermarket.modules.sale.entity.SaleOrder;
import org.supermarket.modules.sale.entity.SaleOrderItem;
import org.supermarket.modules.sale.mapper.ReturnOrderMapper;
import org.supermarket.modules.sale.mapper.SaleOrderItemMapper;
import org.supermarket.modules.sale.mapper.SaleOrderMapper;
import org.supermarket.modules.sale.service.SaleOrderService;
import org.supermarket.modules.product.entity.StoreProductConfig;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaleOrderServiceImpl extends ServiceImpl<SaleOrderMapper, SaleOrder>
        implements SaleOrderService {

    private final SaleOrderItemMapper itemMapper;
    private final ReturnOrderMapper returnOrderMapper;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final MemberService memberService;
    private final MemberLevelMapper memberLevelMapper;
    private final StoreProductConfigService storeProductConfigService;



    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOrder createOrder(SaleOrderDTO dto) {

        // 1. 查询会员信息（如果有）
        Member member = null;
        if (dto.getMemberId() != null) {
            member = memberService.getById(dto.getMemberId());
        } else if (StringUtils.hasText(dto.getMemberPhone())) {
            member = memberService.getByPhone(dto.getMemberPhone());
        }

        // 2. 获取会员折扣率（非会员默认1.0不打折）
        BigDecimal discountRate = BigDecimal.ONE;
        if (member != null) {
            MemberLevel level = memberLevelMapper.selectById(member.getLevelId());
            if (level != null) {
                discountRate = level.getDiscountRate();
                log.info("会员[{}]享受{}折优惠", member.getMemberNo(),
                        discountRate.multiply(new BigDecimal("10")));
            }
        }

        // 3. 构建订单明细
        final Member finalMember = member;

        final BigDecimal finalDiscountRate = discountRate;
        List<SaleOrderItem> items = dto.getItems().stream().map(itemDTO -> {
            Product product = productService.getById(itemDTO.getProductId());
            if (product == null) throw new BusinessException("商品不存在: " + itemDTO.getProductId());
            if (product.getStatus() != 1) throw new BusinessException("商品已下架: " + product.getProductName());

            // 查询门店商品配置
            StoreProductConfig config = storeProductConfigService.getOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StoreProductConfig>()
                            .eq(StoreProductConfig::getStoreId, dto.getStoreId())
                            .eq(StoreProductConfig::getProductId, itemDTO.getProductId()));

            // 检查该商品在本门店是否在售
            if (config != null && config.getStatus() == 0) {
                throw new BusinessException("商品 [" + product.getProductName() + "] 在本门店已停售");
            }

            SaleOrderItem item = new SaleOrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getProductName());
            item.setBarcode(product.getBarcode());
            item.setOriginalPrice(product.getRetailPrice());

            // 价格优先级：前端传入价 > 门店配置价 > 商品默认价
            BigDecimal unitPrice;
            if (itemDTO.getUnitPrice() != null) {
                // 前端明确传入了价格（促销价）
                unitPrice = itemDTO.getUnitPrice();
            } else if (config != null && config.getRetailPrice() != null) {
                // 门店有特殊配置价
                unitPrice = config.getRetailPrice();
            } else {
                // 用商品默认零售价
                unitPrice = product.getRetailPrice();
            }

            // 会员价优先级：门店会员配置价 > 商品会员价 > 折扣率计算
            if (finalMember != null) {
                BigDecimal memberUnitPrice = null;
                if (config != null && config.getMemberPrice() != null) {
                    memberUnitPrice = config.getMemberPrice();
                } else if (product.getMemberPrice() != null) {
                    memberUnitPrice = product.getMemberPrice();
                } else if (finalDiscountRate.compareTo(BigDecimal.ONE) < 0) {
                    memberUnitPrice = unitPrice.multiply(finalDiscountRate)
                            .setScale(2, RoundingMode.HALF_UP);
                }
                if (memberUnitPrice != null) {
                    unitPrice = memberUnitPrice;
                }
            }

            item.setUnitPrice(unitPrice);
            item.setQuantity(itemDTO.getQuantity());
            item.setSubtotal(unitPrice.multiply(itemDTO.getQuantity())
                    .setScale(2, RoundingMode.HALF_UP));
            return item;
        }).collect(Collectors.toList());

        // 4. 计算金额
        BigDecimal totalAmount = items.stream()
                .map(SaleOrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 优惠金额 = 原价总计 - 实际总计
        BigDecimal originalTotal = items.stream()
                .map(i -> i.getOriginalPrice().multiply(i.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discountAmount = originalTotal.subtract(totalAmount)
                .setScale(2, RoundingMode.HALF_UP);
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            discountAmount = BigDecimal.ZERO;
        }

        BigDecimal payableAmount = totalAmount;
        BigDecimal paidAmount = dto.getPaidAmount() != null
                ? dto.getPaidAmount() : payableAmount;

        // 5. 计算积分（每消费1元得1积分，取整）
        int pointsEarned = 0;
        if (finalMember != null) {
            MemberLevel level = memberLevelMapper.selectById(finalMember.getLevelId());
            if (level != null) {
                pointsEarned = payableAmount
                        .multiply(level.getPointsRate())
                        .intValue();
            }
        }

        // 6. 保存订单
        SaleOrder order = new SaleOrder();
        order.setOrderNo("SO" + System.currentTimeMillis());
        order.setStoreId(dto.getStoreId());
        order.setCashierId(dto.getCashierId());
        order.setMemberId(finalMember != null ? finalMember.getId() : null);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setPayableAmount(payableAmount);
        order.setPaidAmount(paidAmount);
        order.setPaymentType(dto.getPaymentType());
        order.setPointsEarned(pointsEarned);
        order.setStatus(1);
        order.setRemark(dto.getRemark());
        save(order);

        // 7. 保存明细并扣减库存
        items.forEach(item -> {
            item.setOrderId(order.getId());
            itemMapper.insert(item);
            inventoryService.outbound(
                    dto.getStoreId(),
                    item.getProductId(),
                    item.getQuantity(),
                    order.getOrderNo());
        });

        // 8. 更新会员积分和消费金额
        if (member != null && pointsEarned > 0) {
            member.setTotalAmount(member.getTotalAmount().add(payableAmount));
            memberService.updateById(member);
            memberService.addPoints(member.getId(), pointsEarned, order.getOrderNo());
            log.info("会员[{}]本单获得积分：{}", member.getMemberNo(), pointsEarned);
        }

        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnOrder(String originalOrderNo, String reason) {
        SaleOrder original = getOne(new LambdaQueryWrapper<SaleOrder>()
                .eq(SaleOrder::getOrderNo, originalOrderNo));
        if (original == null) throw new BusinessException("原订单不存在");
        if (original.getStatus() == 2) throw new BusinessException("该订单已退款");

        // 归还库存
        List<SaleOrderItem> items = itemMapper.selectByOrderId(original.getId());
        items.forEach(item -> inventoryService.inbound(
                original.getStoreId(),
                item.getProductId(),
                item.getQuantity(),
                "RETURN-" + originalOrderNo));

        // 如果是会员订单，扣回积分
        if (original.getMemberId() != null && original.getPointsEarned() > 0) {
            memberService.addPoints(
                    original.getMemberId(),
                    -original.getPointsEarned(),
                    originalOrderNo);
            log.info("退货扣回积分：{}", original.getPointsEarned());
        }

        original.setStatus(2);
        updateById(original);

        ReturnOrder returnOrder = new ReturnOrder();
        returnOrder.setReturnNo("RT" + System.currentTimeMillis());
        returnOrder.setOriginalOrderNo(originalOrderNo);
        returnOrder.setStoreId(original.getStoreId());
        returnOrder.setCashierId(original.getCashierId());
        returnOrder.setReturnAmount(original.getPaidAmount());
        returnOrder.setReason(reason);
        returnOrder.setStatus(1);
        returnOrder.setCreateTime(LocalDateTime.now());
        returnOrderMapper.insert(returnOrder);
    }

    @Override
    public Page<SaleOrder> pageOrder(int pageNum, int pageSize, Long storeId) {
        LambdaQueryWrapper<SaleOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(storeId != null, SaleOrder::getStoreId, storeId);
        wrapper.orderByDesc(SaleOrder::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }
}
