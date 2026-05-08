package org.supermarket.modules.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("member_level")
public class MemberLevel {

    private Long id;
    private String levelName;       // 等级名称
    private BigDecimal minAmount;   // 升级所需最低消费
    private BigDecimal discountRate;// 折扣率
    private BigDecimal pointsRate;  // 积分倍率
    private LocalDateTime createTime;
}