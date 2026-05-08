package org.supermarket.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.supermarket.modules.system.entity.Store;

@Mapper
public interface StoreMapper extends BaseMapper<Store> {
}