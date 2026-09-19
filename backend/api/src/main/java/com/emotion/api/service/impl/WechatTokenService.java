package com.emotion.api.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.emotion.api.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class WechatTokenService {
    
    @Autowired
    private RedisUtil redisUtil;
    
    @Value("${wechat.app-id}")
    private String appId;
    
    @Value("${wechat.app-secret}")
    private String appSecret;
    
    private static final String TOKEN_KEY = "wechat:access_token";
    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    
    /**
     * 获取Access Token（带缓存）
     */
    public String getAccessToken() {
        // 先从Redis获取
        Object tokenObj = redisUtil.get(TOKEN_KEY);
        if (tokenObj != null) {
            return tokenObj.toString();
        }
        
        // Redis中没有，从微信服务器获取
        String url = String.format("%s?grant_type=client_credential&appid=%s&secret=%s",
                TOKEN_URL, appId, appSecret);
        
        String response = HttpUtil.get(url);
        JSONObject result = JSONUtil.parseObj(response);
        
        if (result.containsKey("access_token")) {
            String token = result.getStr("access_token");
            int expiresIn = result.getInt("expires_in");
            
            // 存入Redis，提前5分钟过期
            redisUtil.set(TOKEN_KEY, token, expiresIn - 300, TimeUnit.SECONDS);
            
            log.info("获取Access Token成功");
            return token;
        } else {
            log.error("获取Access Token失败: {}", result.getStr("errmsg"));
            throw new RuntimeException("获取Access Token失败: " + result.getStr("errmsg"));
        }
    }
}
