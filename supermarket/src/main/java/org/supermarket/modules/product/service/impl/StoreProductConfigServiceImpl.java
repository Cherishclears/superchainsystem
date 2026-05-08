package org.supermarket.modules.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.supermarket.modules.product.entity.StoreProductConfig;
import org.supermarket.modules.product.mapper.StoreProductConfigMapper;
import org.supermarket.modules.product.service.StoreProductConfigService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StoreProductConfigServiceImpl
        extends ServiceImpl<StoreProductConfigMapper, StoreProductConfig>
        implements StoreProductConfigService {

    @Override
    public List<StoreProductConfig> listByStore(Long storeId) {
        return list(new LambdaQueryWrapper<StoreProductConfig>()
                .eq(StoreProductConfig::getStoreId, storeId));
    }

    @Override
    public void saveOrUpdateConfig(StoreProductConfig config) {
        StoreProductConfig existing = getOne(new LambdaQueryWrapper<StoreProductConfig>()
                .eq(StoreProductConfig::getStoreId, config.getStoreId())
                .eq(StoreProductConfig::getProductId, config.getProductId()));
        if (existing != null) {
            config.setId(existing.getId());
            config.setUpdateTime(LocalDateTime.now());
            updateById(config);
        } else {
            config.setCreateTime(LocalDateTime.now());
            config.setUpdateTime(LocalDateTime.now());
            save(config);
        }
    }
}