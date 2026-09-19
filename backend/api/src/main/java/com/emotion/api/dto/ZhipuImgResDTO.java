package com.emotion.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class ZhipuImgResDTO {
    private String role;
    private List<Content> content;
    private String name;
    private List<Object> tool_calls;
    private String tool_call_id;

    @Data
    public static class Content {
        private String url;
    }
}

