package com.emotion.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserRelationshipDTO {
    private String relation;

    private String uniqueCode;

    private List<Long> userIds;
}