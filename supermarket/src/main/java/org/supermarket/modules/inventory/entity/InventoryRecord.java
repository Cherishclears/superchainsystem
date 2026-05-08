package org.supermarket.modules.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inventory_record")
public class InventoryRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long storeId;
    private Long productId;
    private Integer changeType;      // 1入库 2出库 3调拨入 4调拨出 5盘点 6报损
    private BigDecimal quantityChange;  // 变化量（正入负出）
    private BigDecimal quantityBefore;  // 变更前库存
    private BigDecimal quantityAfter;   // 变更后库存
    private String refOrderNo;          // 关联单号
    private String remark;
    private LocalDateTime createTime;
    private Long createBy;
}