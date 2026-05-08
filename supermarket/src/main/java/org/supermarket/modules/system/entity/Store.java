package org.supermarket.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.supermarket.common.entity.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("store")
public class Store extends BaseEntity {

    private String storeCode;   // 门店编码
    private String storeName;   // 门店名称
    private String address;     // 地址
    private String region;      // 区域
    private Long managerId;     // 店长ID
    private String phone;       // 联系电话
    private Integer status;     // 1正常 0关闭
}