package com.emotion.api.dto;

import lombok.Data;

@Data
public class BindUserRequest {
    private String uniqueCode;

    private String relation;
}
