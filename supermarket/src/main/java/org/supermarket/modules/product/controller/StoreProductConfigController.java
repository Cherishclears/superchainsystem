package org.supermarket.modules.product.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.supermarket.common.result.Result;
import org.supermarket.common.utils.RedisUtils;
import org.supermarket.modules.product.entity.Product;
import org.supermarket.modules.product.entity.StoreProductConfig;
import org.supermarket.modules.product.service.ProductService;
import org.supermarket.modules.product.service.StoreProductConfigService;

import java.util.List;

@RestController
@RequestMapping("/store-product")
@RequiredArgsConstructor
public class StoreProductConfigController {

    private final StoreProductConfigService configService;

    private final RedisUtils redisUtils;
    private final ProductService productService;


    // 保存或更新配置
    @PostMapping("/save")
    public Result<String> save(@RequestBody StoreProductConfig config) {
        configService.saveOrUpdateConfig(config);
        // 清除该门店该商品的条形码缓存
        Product product = productService.getById(config.getProductId());
        if (product != null && product.getBarcode() != null) {
            redisUtils.delete("product:barcode:" + product.getBarcode() + ":" + config.getStoreId());
        }
        return Result.ok("保存成功");
    }

    // 查询某门店的商品配置列表
    @GetMapping("/list/{storeId}")
    public Result<List<StoreProductConfig>> list(@PathVariable Long storeId) {
        return Result.ok(configService.listByStore(storeId));
    }


    // 批量保存
    @PostMapping("/batch")
    public Result<String> batch(@RequestBody List<StoreProductConfig> configs) {
        configs.forEach(configService::saveOrUpdateConfig);
        return Result.ok("保存成功");
    }
}