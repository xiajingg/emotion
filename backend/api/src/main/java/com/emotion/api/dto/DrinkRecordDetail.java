package com.emotion.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DrinkRecordDetail {
    // 喝水的时间：时分秒
    private String time;
    // 饮水量
    private Integer drinkIntake;
    // 图片
    private Long imageId;
}
