package org.supermarket.modules.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.supermarket.modules.inventory.entity.InventoryRecord;

@Mapper
public interface InventoryRecordMapper extends BaseMapper<InventoryRecord> {
}