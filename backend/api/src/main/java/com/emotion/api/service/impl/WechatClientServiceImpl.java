package com.emotion.api.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.emotion.api.dto.WechatRemoteClient;
import com.emotion.api.dto.OpenIdVO;
import com.emotion.api.service.WechatClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WechatClientServiceImpl implements WechatClientService {

    @Autowired
    private WechatRemoteClient wechatRemoteClient;

    // 微信小程序凭据 —— 不入库，见 application-secret.yml 或环境变量
    @Value("${wechat.app-id}")
    private String wechatAppId;

    @Value("${wechat.app-secret}")
    private String wechatAppSecret;

    @Override
    public OpenIdVO getOpenIdByCode(String code) {
        String url = "https://api.weixin.qq.com/sns/jscode2session?" +
                "appid=" + wechatAppId +
                "&secret=" + wechatAppSecret +
                "&js_code=" + code +
                "&grant_type=authorization_code";
        String json = wechatRemoteClient.callWechatRemoteService(url);
        OpenIdVO vo = JSONUtil.toBean(json, OpenIdVO.class);
        if(StrUtil.isNotBlank(vo.getErrMsg())){
            throw new RuntimeException(vo.getErrMsg());
        }
        return vo;
    }
}
