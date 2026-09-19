package com.emotion.api.repository.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_share")
public class UserShare {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String newUserId;
    private String shareUserId;
    private LocalDateTime createTime;
}