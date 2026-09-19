package com.emotion.api.repository.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友绑定关系表（共享码中心模型）
 * 支持未来群组扩展：通过 bind_code 关联多个用户
 */
@Data
@TableName("friend_bind")
public class FriendBind {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 共享码（群组标识）
     */
    private String bindCode;

    /**
     * 状态：0-待绑定/初始化, 1-已绑定
     */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
