package com.emotion.api.repository.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("reward_record")
public class RewardRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String param;
    private Date createTime;
    private Integer count; // 新增次数字段
}