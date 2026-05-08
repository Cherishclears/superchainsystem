package org.supermarket.modules.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.supermarket.modules.purchase.entity.PurchaseOrder;

@Mapper
public interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {
}