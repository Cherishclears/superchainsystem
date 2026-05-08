package org.supermarket.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.supermarket.modules.system.entity.Store;
import org.supermarket.modules.system.mapper.StoreMapper;
import org.supermarket.modules.system.service.StoreService;

@Service
public class StoreServiceImpl extends ServiceImpl<StoreMapper, Store>
        implements StoreService {
}