package org.supermarket.modules.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.supermarket.modules.inventory.entity.TransferOrderItem;

@Mapper
public interface TransferOrderItemMapper extends BaseMapper<TransferOrderItem> {
}