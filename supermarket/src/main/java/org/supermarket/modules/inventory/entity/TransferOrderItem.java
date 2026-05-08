package org.supermarket.modules.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("transfer_order_item")
public class TransferOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long transferId;
    private Long productId;
    private String productName;
    private BigDecimal quantity;
    private LocalDateTime createTime;
}