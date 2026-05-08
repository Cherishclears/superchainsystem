package org.supermarket.modules.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.supermarket.modules.inventory.entity.TransferOrder;

@Mapper
public interface TransferOrderMapper extends BaseMapper<TransferOrder> {
}