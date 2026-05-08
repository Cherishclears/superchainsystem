package org.supermarket.modules.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.supermarket.modules.product.entity.ProductCategory;

import java.util.List;

public interface ProductCategoryService extends IService<ProductCategory> {
    List<ProductCategory> listAll();
}