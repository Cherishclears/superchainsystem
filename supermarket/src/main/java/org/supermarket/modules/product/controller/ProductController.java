package org.supermarket.modules.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.supermarket.common.result.Result;
import org.supermarket.common.utils.RedisUtils;
import org.supermarket.modules.product.entity.Product;
import org.supermarket.modules.product.entity.StoreProductConfig;
import org.supermarket.modules.product.service.ProductService;
import org.supermarket.modules.product.service.StoreProductConfigService;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    private final StoreProductConfigService storeProductConfigService;

    private final RedisUtils redisUtils;

    // 分页查询
    @GetMapping("/page")
    public Result<Page<Product>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {
        return Result.ok(productService.pageProduct(pageNum, pageSize, productName, categoryId, status));
    }

    // 根据ID查询
    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable Long id) {
        return Result.ok(productService.getById(id));
    }

    // 根据条形码查询
    @GetMapping("/barcode/{barcode}")
    public Result<Product> getByBarcode(
            @PathVariable String barcode,
            @RequestParam(required = false) Long storeId) {

        String cacheKey = "product:barcode:" + barcode + ":" + (storeId != null ? storeId : "default");

        // 先查 Redis
        Object cached = redisUtils.get(cacheKey);
        if (cached != null) {
            return Result.ok((Product) cached);
        }

        // 查数据库
        Product product = productService.getOne(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getBarcode, barcode)
                        .eq(Product::getStatus, 1));

        if (product == null) return Result.fail("商品不存在或已下架");

        if (storeId != null) {
            StoreProductConfig config = storeProductConfigService.getOne(
                    new LambdaQueryWrapper<StoreProductConfig>()
                            .eq(StoreProductConfig::getStoreId, storeId)
                            .eq(StoreProductConfig::getProductId, product.getId()));
            if (config != null) {
                if (config.getStatus() == 0) return Result.fail("该商品在本店已停售");
                if (config.getRetailPrice() != null) product.setRetailPrice(config.getRetailPrice());
                if (config.getMemberPrice() != null) product.setMemberPrice(config.getMemberPrice());
            }
        }

        // 写入 Redis，缓存 30 分钟
        redisUtils.set(cacheKey, product, 30, TimeUnit.MINUTES);

        return Result.ok(product);
    }

    // 新增
    @PostMapping
    public Result<String> add(@Valid @RequestBody Product product) {
        productService.addProduct(product);
        return Result.ok("添加成功");
    }

    // 修改
    @PutMapping
    public Result<String> update(@RequestBody Product product) {
        productService.updateProduct(product);
        return Result.ok("修改成功");
    }

    // 上下架
    @PutMapping("/{id}/status/{status}")
    public Result<String> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        productService.updateStatus(id, status);
        return Result.ok(status == 1 ? "上架成功" : "下架成功");
    }

    // 删除（逻辑删除）
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        productService.removeById(id);
        return Result.ok("删除成功");
    }

    // 查询门店在售商品
    @GetMapping("/store/{storeId}/page")
    public Result<Page<Product>> pageByStore(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Long categoryId) {
        return Result.ok(productService.pageByStore(pageNum, pageSize, storeId, productName, categoryId));
    }


}
