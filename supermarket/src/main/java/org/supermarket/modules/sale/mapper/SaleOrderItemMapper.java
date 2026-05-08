package org.supermarket.modules.sale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.supermarket.modules.sale.entity.SaleOrderItem;

import java.util.List;

@Mapper
public interface SaleOrderItemMapper extends BaseMapper<SaleOrderItem> {

    @Select("SELECT * FROM sale_order_item WHERE order_id = #{orderId}")
    List<SaleOrderItem> selectByOrderId(Long orderId);
}