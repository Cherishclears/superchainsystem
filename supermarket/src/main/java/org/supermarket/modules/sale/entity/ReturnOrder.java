package org.supermarket.modules.sale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("return_order")
public class ReturnOrder {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String returnNo;          // 退货单号
    private String originalOrderNo;   // 原销售单号
    private Long storeId;
    private Long cashierId;
    private BigDecimal returnAmount;  // 退款金额
    private String reason;            // 退货原因
    private Integer status;
    private LocalDateTime createTime;
    private Long createBy;
}