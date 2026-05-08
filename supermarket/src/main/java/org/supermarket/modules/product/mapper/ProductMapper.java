package org.supermarket.modules.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.supermarket.modules.product.entity.Product;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

}