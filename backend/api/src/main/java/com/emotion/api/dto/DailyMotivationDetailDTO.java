package com.emotion.api.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DailyMotivationDetailDTO {

    private Long id;

    private LocalDate date;

    private String motivationContent;
}
