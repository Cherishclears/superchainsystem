package org.supermarket.modules.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("member")
public class Member {

    private Long id;
    private String memberNo;        // 会员编号
    private String phone;           // 手机号
    private String realName;        // 真实姓名
    private Integer gender;         // 0未知 1男 2女
    private LocalDate birthday;     // 生日
    private Long levelId;           // 会员等级
    private Integer points;         // 当前积分
    private Integer totalPoints;    // 累计积分
    private BigDecimal totalAmount; // 累计消费金额
    private Long registerStore;     // 注册门店
    private Integer status;         // 1正常 0冻结
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}