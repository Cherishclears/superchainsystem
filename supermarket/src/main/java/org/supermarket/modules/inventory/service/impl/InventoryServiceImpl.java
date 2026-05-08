package org.supermarket.modules.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.supermarket.common.exception.BusinessException;
import org.supermarket.common.result.ResultCode;
import org.supermarket.common.utils.RedisUtils;
import org.supermarket.modules.inventory.entity.Inventory;
import org.supermarket.modules.inventory.entity.InventoryRecord;
import org.supermarket.modules.inventory.mapper.InventoryMapper;
import org.supermarket.modules.inventory.mapper.InventoryRecordMapper;
import org.supermarket.modules.inventory.service.InventoryService;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl extends ServiceImpl<InventoryMapper, Inventory>
        implements InventoryService {

    private final InventoryRecordMapper recordMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private final RedisUtils redisUtils;

    private static final String LOCK_PREFIX = "lock:inventory:";

    // 同步单个商品库存到 Redis
    private void syncToRedis(Long storeId, Long productId, BigDecimal quantity) {
        String key = "inventory:" + storeId + ":" + productId;
        // 乘以1000存为整数，支持3位小数的称重商品
        redisUtils.set(key, (long)(quantity.doubleValue() * 1000), 24, TimeUnit.HOURS);
    }

    // 从 Redis 扣减库存，返回是否成功
    private boolean decrFromRedis(Long storeId, Long productId, BigDecimal qty) {
        String key = "inventory:" + storeId + ":" + productId;

        // Redis 没有则先从数据库同步
        if (!redisUtils.hasKey(key)) {
            Inventory inv = getOne(new LambdaQueryWrapper<Inventory>()
                    .eq(Inventory::getStoreId, storeId)
                    .eq(Inventory::getProductId, productId));
            if (inv == null) return false;
            syncToRedis(storeId, productId, inv.getQuantity());
        }

        long decrAmount = (long)(qty.doubleValue() * 1000);
        Long remaining = redisUtils.decrement(key, decrAmount);

        if (remaining != null && remaining < 0) {
            // 库存不足，回滚 Redis
            redisUtils.increment(key, decrAmount);
            return false;
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inbound(Long storeId, Long productId, BigDecimal qty, String refOrderNo) {
        String lockKey = LOCK_PREFIX + storeId + ":" + productId;
        if (!tryLock(lockKey)) throw new BusinessException(ResultCode.INVENTORY_LOCKED);
        try {
            Inventory inventory = getOrCreate(storeId, productId);
            BigDecimal before = inventory.getQuantity();
            BigDecimal after = before.add(qty);
            inventory.setQuantity(after);
            updateById(inventory);
            saveRecord(storeId, productId, 1, qty, before, after, refOrderNo);
            // 同步更新 Redis
            syncToRedis(storeId, productId, after);
        } finally {
            releaseLock(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void outbound(Long storeId, Long productId, BigDecimal qty, String refOrderNo) {
        // 先走 Redis 原子扣减（快速失败，减少锁竞争）
        boolean redisSuccess = decrFromRedis(storeId, productId, qty);
        if (!redisSuccess) throw new BusinessException(ResultCode.INVENTORY_NOT_ENOUGH);

        String lockKey = LOCK_PREFIX + storeId + ":" + productId;
        if (!tryLock(lockKey)) {
            // 获取锁失败，回滚 Redis
            redisUtils.increment("inventory:" + storeId + ":" + productId,
                    (long)(qty.doubleValue() * 1000));
            throw new BusinessException(ResultCode.INVENTORY_LOCKED);
        }
        try {
            Inventory inventory = getOrCreate(storeId, productId);
            BigDecimal before = inventory.getQuantity();
            if (before.compareTo(qty) < 0) {
                // 数据库兜底检查失败，回滚 Redis
                redisUtils.increment("inventory:" + storeId + ":" + productId,
                        (long)(qty.doubleValue() * 1000));
                throw new BusinessException(ResultCode.INVENTORY_NOT_ENOUGH);
            }
            BigDecimal after = before.subtract(qty);
            inventory.setQuantity(after);
            updateById(inventory);
            saveRecord(storeId, productId, 2, qty.negate(), before, after, refOrderNo);
        } finally {
            releaseLock(lockKey);
        }
    }

    @Override
    public List<Inventory> listByStore(Long storeId) {
        return list(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getStoreId, storeId));
    }

    @Override
    public List<Inventory> listWarning(Long storeId) {
        return baseMapper.selectWarningList(storeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjust(Long storeId, Long productId, BigDecimal newQty, String remark) {
        Inventory inventory = getOrCreate(storeId, productId);
        BigDecimal before = inventory.getQuantity();
        BigDecimal diff = newQty.subtract(before);
        inventory.setQuantity(newQty);
        updateById(inventory);
        saveRecord(storeId, productId, 5, diff, before, newQty, remark);
        syncToRedis(storeId, productId, newQty);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarningQty(Long storeId, Long productId, BigDecimal warningQty) {
        Inventory inventory = getOrCreate(storeId, productId);
        inventory.setWarningQty(warningQty);
        updateById(inventory);
    }

    // ---- 私有方法 ----

    // 查询库存，不存在则自动创建一条0库存记录
    private Inventory getOrCreate(Long storeId, Long productId) {
        Inventory inv = getOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getStoreId, storeId)
                .eq(Inventory::getProductId, productId));
        if (inv == null) {
            inv = new Inventory();
            inv.setStoreId(storeId);
            inv.setProductId(productId);
            inv.setQuantity(BigDecimal.ZERO);
            inv.setWarningQty(BigDecimal.ZERO);
            save(inv);
        }
        return inv;
    }

    // 记录库存流水
    private void saveRecord(Long storeId, Long productId, int changeType,
                            BigDecimal change, BigDecimal before,
                            BigDecimal after, String refOrderNo) {
        InventoryRecord record = new InventoryRecord();
        record.setStoreId(storeId);
        record.setProductId(productId);
        record.setChangeType(changeType);
        record.setQuantityChange(change);
        record.setQuantityBefore(before);
        record.setQuantityAfter(after);
        record.setRefOrderNo(refOrderNo);
        recordMapper.insert(record);
    }

    // Redis 分布式锁
    private boolean tryLock(String key) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(10)));
    }

    private void releaseLock(String key) {
        redisTemplate.delete(key);
    }
}