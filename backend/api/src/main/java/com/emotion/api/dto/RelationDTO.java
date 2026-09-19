package com.emotion.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class RelationDTO {
    private List<UserRelationshipDTO> userRelationships;

    private int bindStatus;
}
