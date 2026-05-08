package org.supermarket.modules.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.supermarket.modules.product.entity.Product;

public interface ProductService extends IService<Product> {

    // 分页查询
    Page<Product> pageProduct(int pageNum, int pageSize,
                              String productName, Long categoryId, Integer status);

    // 新增商品（校验条形码唯一）
    void addProduct(Product product);

    // 修改商品
    void updateProduct(Product product);

    // 根据条形码查询（收银扫码用）
    Product getByBarcode(String barcode);

    // 上下架
    void updateStatus(Long id, Integer status);

    // 查询某门店在售的商品（分页）
    Page<Product> pageByStore(int pageNum, int pageSize, Long storeId, String productName, Long categoryId);
}