package com.emotion.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DrinkRecordDTO {
    // 喝水日期
    private String date;

    // 喝水记录细节
    private List<DrinkRecordDetail> details;

    // 总饮水量
    private Integer totalDrinkIntake;
}
