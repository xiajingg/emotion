package com.emotion.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class DrinkSubmitDTO {
    // 饮水量
    private Integer drinkIntake;

    // 上传图片ids
    private Long uploadFileId;
}
