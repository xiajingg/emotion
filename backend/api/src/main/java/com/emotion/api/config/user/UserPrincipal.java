package com.emotion.api.config.user;

import lombok.Data;

@Data
public class UserPrincipal {

    private String userOpenId;

    private Long userId;
}
