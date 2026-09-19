package com.emotion.api.service;

import com.emotion.api.dto.OpenIdVO;

public interface WechatClientService {
    OpenIdVO getOpenIdByCode(String code);
}
