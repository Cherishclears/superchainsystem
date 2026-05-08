package org.supermarket.modules.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.supermarket.modules.purchase.entity.PurchaseOrderItem;

import java.util.List;

@Mapper
public interface PurchaseOrderItemMapper extends BaseMapper<PurchaseOrderItem> {

    @Select("SELECT * FROM purchase_order_item WHERE order_id = #{orderId}")
    List<PurchaseOrderItem> selectByOrderId(Long orderId);
}