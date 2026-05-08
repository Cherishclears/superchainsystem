package org.supermarket.modules.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("transfer_order")
public class TransferOrder {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String transferNo;
    private Long fromStoreId;
    private Long toStoreId;
    private Integer status;
    private Long applicantId;
    private Long approverId;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}