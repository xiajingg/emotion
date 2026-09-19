package com.emotion.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class HistoryEmotion {
    private Long id;
    private String inputText;
    private String emotion;
    private Integer score;
    private String reminder;
    private List<Long> imgId;
    private List<String> imageUrls;
    private Boolean doodle;
    private String viewHint;
    private String aiComment;
    private List<String> reliefActions;
    private Map<String, String> replies;
    private String answerBook;
    private String createTime;

}
