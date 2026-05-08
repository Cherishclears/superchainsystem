package org.supermarket.modules.sale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.supermarket.modules.sale.entity.SaleOrder;

@Mapper
public interface SaleOrderMapper extends BaseMapper<SaleOrder> {
}