package com.emotion.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LineChartData {
    private double score;

    private String time;
}
