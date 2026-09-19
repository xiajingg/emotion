package com.emotion.api.repository.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("friend_link_analysis")
public class FriendLinkAnalysis {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String bindCode;

    private LocalDate analysisDate;

    private String title;

    private String summary;

    private String suggestion;

    private String answerBook;

    private Integer meCount;

    private Integer friendCount;

    private Integer sameDayCount;

    private String aiResponse;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
