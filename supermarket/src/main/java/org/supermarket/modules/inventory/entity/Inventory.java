package org.supermarket.modules.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inventory")
public class Inventory {

    private Long id;
    private Long storeId;
    private Long productId;
    private BigDecimal quantity;     // 当前库存
    private BigDecimal warningQty;   // 预警下限
    private LocalDateTime updateTime;

    @Version  // 乐观锁，防止并发更新库存
    private Integer version;
}