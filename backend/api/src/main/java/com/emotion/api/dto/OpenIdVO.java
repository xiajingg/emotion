package com.emotion.api.dto;

import cn.hutool.core.annotation.Alias;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class OpenIdVO {

    @Alias("session_key")
    @JsonAlias("session_key")
    private String sessionKey;
    @Alias("openid")
    @JsonAlias("openid")
    private String openId;
    @Alias("errcode")
    @JsonAlias("errcode")
    private String errCode;
    @Alias("errmsg")
    @JsonAlias("errmsg")
    private String errMsg;
}
