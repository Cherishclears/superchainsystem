package org.supermarket.modules.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.supermarket.common.exception.BusinessException;
import org.supermarket.common.result.ResultCode;
import org.supermarket.modules.product.entity.Product;
import org.supermarket.modules.product.entity.StoreProductConfig;
import org.supermarket.modules.product.mapper.ProductMapper;
import org.supermarket.modules.product.mapper.StoreProductConfigMapper;
import org.supermarket.modules.product.service.ProductService;


import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;



import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product>
        implements ProductService {

    private final StoreProductConfigMapper storeProductConfigMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Page<Product> pageProduct(int pageNum, int pageSize,
                                     String productName, Long categoryId, Integer status) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        // 商品名模糊查询
        wrapper.like(StringUtils.hasText(productName), Product::getProductName, productName);
        // 分类筛选
        wrapper.eq(categoryId != null, Product::getCategoryId, categoryId);
        // 状态筛选
        wrapper.eq(status != null, Product::getStatus, status);
        // 按创建时间倒序
        wrapper.orderByDesc(Product::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public void addProduct(Product product) {
        // 校验条形码唯一
        if (StringUtils.hasText(product.getBarcode())) {
            long count = count(new LambdaQueryWrapper<Product>()
                    .eq(Product::getBarcode, product.getBarcode()));
            if (count > 0) throw new BusinessException(ResultCode.PRODUCT_BARCODE_EXISTS);
        }
        // 生成商品编码
        product.setProductCode("P" + System.currentTimeMillis());
        save(product);
    }

    @Override
    public void updateProduct(Product product) {
        Product old = getById(product.getId());
        if (old == null) throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        updateById(product);
        // 清除该商品所有条形码相关缓存
        if (StringUtils.hasText(old.getBarcode())) {
            Set<String> keys = redisTemplate.keys("product:barcode:" + old.getBarcode() + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }

    @Override
    public Product getByBarcode(String barcode) {
        return getOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getBarcode, barcode)
                .eq(Product::getStatus, 1));
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Product product = new Product();
        product.setId(id);
        product.setStatus(status);
        updateById(product);
    }

    @Override
    public Page<Product> pageByStore(int pageNum, int pageSize, Long storeId, String productName, Long categoryId) {
        // 查该门店在售的商品配置
        List<StoreProductConfig> configs = storeProductConfigMapper.selectList(
                new LambdaQueryWrapper<StoreProductConfig>()
                        .eq(StoreProductConfig::getStoreId, storeId)
                        .eq(StoreProductConfig::getStatus, 1));

        if (configs.isEmpty()) return new Page<>();

        // 构建配置Map
        Map<Long, StoreProductConfig> configMap = configs.stream()
                .collect(Collectors.toMap(StoreProductConfig::getProductId, c -> c));

        List<Long> productIds = new ArrayList<>(configMap.keySet());

        // 查商品
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Product::getId, productIds);
        wrapper.eq(Product::getStatus, 1);
        wrapper.like(StringUtils.hasText(productName), Product::getProductName, productName);
        wrapper.eq(categoryId != null, Product::getCategoryId, categoryId);
        wrapper.orderByAsc(Product::getCategoryId);

        Page<Product> pageResult = page(new Page<>(pageNum, pageSize), wrapper);

        // 用门店配置价格覆盖默认价格
        pageResult.getRecords().forEach(product -> {
            StoreProductConfig config = configMap.get(product.getId());
            if (config != null) {
                if (config.getRetailPrice() != null) {
                    product.setRetailPrice(config.getRetailPrice());
                }
                if (config.getMemberPrice() != null) {
                    product.setMemberPrice(config.getMemberPrice());
                }
            }
        });

        return pageResult;
    }
}