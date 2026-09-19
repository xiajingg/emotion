package com.emotion.api.dto;

import lombok.Data;

@Data
public class SubmitDrawingDTO {

    private Long uploadFileId;

    private String selectedMood;

    private String drawingMeta;

    private String drawingShareText;
}
