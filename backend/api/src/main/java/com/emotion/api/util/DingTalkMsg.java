package com.emotion.api.util;

import cn.hutool.core.codec.Base64;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.dingtalk.api.response.OapiRobotSendResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;

@Component
@Slf4j
public class DingTalkMsg {

    // 钉钉机器人凭据 —— 不入库，见 application-secret.yml 或环境变量
    @Value("${dingtalk.access-token}")
    private String accessToken;

    @Value("${dingtalk.secret}")
    private String dingtalkSecret;

    @Async
    public void sendMsgToDingTalk(String msg) {
        try {
            // 获取当前时间
            Long timestamp = System.currentTimeMillis();
            // 定义密钥
            String secret = dingtalkSecret;
            // 拼接密钥和时间戳
            String stringToSign = timestamp + "\n" + secret;
            // 加密算法，下面4行在做加密
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes("UTF-8"));
            String sign = URLEncoder.encode(Base64.encode(signData), "UTF-8");
            // 拼接url请求
            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/robot/send?access_token=" + accessToken + "&sign=" + sign + "&timestamp=" + timestamp);
            // 请求体
            OapiRobotSendRequest req = new OapiRobotSendRequest();
            /**
             * 发送文本消息
             */
            // 发送的文本，设置发送内容
            OapiRobotSendRequest.Text text = new OapiRobotSendRequest.Text();
            text.setContent(msg);
            // 设置消息类型
            req.setMsgtype("text");
            // 把消息放进来
            req.setText(text);
            OapiRobotSendResponse rsp = client.execute(req);
            System.out.println(rsp.getBody());
        } catch (Exception ex) {
            log.error("发送钉钉消息异常", ex);
        }
    }
}
