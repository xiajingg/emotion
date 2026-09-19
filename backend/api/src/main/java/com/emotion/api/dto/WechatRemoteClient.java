package com.emotion.api.dto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class WechatRemoteClient {

    @Autowired
    private RestTemplate restTemplate;

    public String callWechatRemoteService(String url) {
        String json = restTemplate.getForObject(url, String.class);
        log.info("{}", json);
        return json;
    }
}
