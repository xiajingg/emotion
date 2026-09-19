package com.emotion.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class UserRankDTO {
    private String userName;

    private Float total;

    private Integer rank;
}
