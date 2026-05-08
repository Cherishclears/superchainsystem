package org.supermarket.modules.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("points_record")
public class PointsRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long memberId;
    private Integer changeType; // 1消费获得 2兑换使用 3活动赠送
    private Integer points;     // 正获得负使用
    private String refOrderNo;
    private String remark;
    private LocalDateTime createTime;
}