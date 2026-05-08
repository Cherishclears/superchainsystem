package org.supermarket.modules.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.supermarket.modules.product.entity.StoreProductConfig;

import java.util.List;

public interface StoreProductConfigService extends IService<StoreProductConfig> {

    // 查询某门店的所有商品配置
    List<StoreProductConfig> listByStore(Long storeId);

    // 保存或更新配置（有则更新，无则新增）
    void saveOrUpdateConfig(StoreProductConfig config);
}