package org.supermarket.modules.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.supermarket.modules.product.entity.ProductCategory;
import org.supermarket.modules.product.mapper.ProductCategoryMapper;
import org.supermarket.modules.product.service.ProductCategoryService;

import java.util.List;

@Service
public class ProductCategoryServiceImpl extends ServiceImpl<ProductCategoryMapper, ProductCategory>
        implements ProductCategoryService {

    @Override
    public List<ProductCategory> listAll() {
        return list(new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getStatus, 1)
                .orderByAsc(ProductCategory::getLevel)
                .orderByAsc(ProductCategory::getSortOrder));
    }
}