package org.supermarket.modules.purchase.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.supermarket.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("purchase_order")
public class PurchaseOrder extends BaseEntity {

    private String orderNo;         // 采购单号
    private Long storeId;           // 收货门店
    private Long supplierId;        // 供应商
    private BigDecimal totalAmount; // 采购总金额
    private Integer status;         // 0草稿 1待审核 2已审核 3已入库 4已取消
    private LocalDate expectedDate; // 预计到货日期
    private LocalDate actualDate;   // 实际到货日期
    private Long approveBy;         // 审核人
    private LocalDateTime approveTime; // 审核时间
    private String remark;
}