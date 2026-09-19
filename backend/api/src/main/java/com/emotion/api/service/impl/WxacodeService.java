package com.emotion.api.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 微信小程序码生成服务
 * 调用微信 wxacode.getUnlimited 接口获取小程序码
 */
@Slf4j
@Service
public class WxacodeService {

    @Autowired
    private WechatTokenService wechatTokenService;

    private static final String WXACODE_URL = "https://api.weixin.qq.com/wxa/getwxacodeunlimit";

    /**
     * 获取小程序码（Base64 编码返回）
     *
     * @param scene 场景值（用于统计分享来源，最多32个字符）
     * @param page  小程序页面路径（如 "pages/index/index"）
     * @return Base64 编码的小程序码图片，格式为 data:image/png;base64,xxx
     */
    public String getWxacodeBase64(String scene, String page) {
        String accessToken = wechatTokenService.getAccessToken();

        JSONObject body = new JSONObject();
        body.set("scene", scene != null ? scene : "share");
        body.set("page", page != null ? page : "pages/index/index");
        body.set("check_path", false);
        body.set("env_version", "release");
        body.set("width", 280);
        body.set("auto_color", true);
        body.set("is_hyaline", false);

        String url = WXACODE_URL + "?access_token=" + accessToken;

        try {
            HttpResponse response = HttpRequest.post(url)
                    .body(body.toString())
                    .execute();

            if (response.getStatus() == 200) {
                byte[] bytes = response.bodyBytes();
                String contentType = response.header("Content-Type");

                // 如果返回的是错误 JSON（errcode），说明生成失败
                if (contentType != null && contentType.contains("application/json")) {
                    String bodyStr = response.body();
                    JSONObject result = JSONUtil.parseObj(bodyStr);
                    int errcode = result.getInt("errcode", 0);
                    if (errcode != 0) {
                        log.error("获取小程序码失败: errcode={}, errmsg={}",
                                errcode, result.getStr("errmsg"));
                        throw new RuntimeException("获取小程序码失败: " + result.getStr("errmsg"));
                    }
                }

                String base64 = "data:image/png;base64," + Base64.encode(bytes);
                log.info("小程序码生成成功, size={} bytes", bytes.length);
                return base64;
            } else {
                log.error("获取小程序码HTTP错误: status={}, body={}", response.getStatus(), response.body());
                throw new RuntimeException("获取小程序码失败: HTTP " + response.getStatus());
            }
        } catch (Exception e) {
            log.error("获取小程序码异常", e);
            throw new RuntimeException("获取小程序码失败: " + e.getMessage());
        }
    }
}
