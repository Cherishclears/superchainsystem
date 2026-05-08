package org.supermarket.modules.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.supermarket.modules.inventory.entity.Inventory;

import java.util.List;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    // 查询低于预警线的商品
    @Select("SELECT * FROM inventory WHERE store_id = #{storeId} " +
            "AND warning_qty > 0 AND quantity <= warning_qty")
    List<Inventory> selectWarningList(Long storeId);
}