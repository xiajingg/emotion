package com.emotion.api.exception;

import cn.hutool.core.codec.Base64;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.dingtalk.api.response.OapiRobotSendResponse;
import com.emotion.api.service.OllamaDirectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
public class SendException {

    @Autowired
    private OllamaDirectService ollamaDirectService;

    // 钉钉机器人凭据 —— 不入库，见 application-secret.yml 或环境变量
    @Value("${dingtalk.access-token}")
    private String accessToken;

    @Value("${dingtalk.secret}")
    private String dingtalkSecret;

    private static final ConcurrentHashMap<String, AtomicInteger> errorRateLimit = new ConcurrentHashMap<>();
    private static final long RATE_LIMIT_WINDOW = 60000;
    private static final int MAX_CALLS_PER_WINDOW = 5;

    @Async
    public void sendMsgToDingTalk(Exception e) {
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
            StringBuilder exStackTrace = new StringBuilder();
            exStackTrace.append(e.getMessage());
            exStackTrace.append("\n");
            StackTraceElement[] stackTrace = e.getStackTrace();
            for (int i = 0; i < 10 && i < stackTrace.length; i++) {
                exStackTrace.append(stackTrace[i]);
                exStackTrace.append("\n");
            }
            
            String s = trackExceptionWithRateLimit(e);
            exStackTrace.append("-----------------------------------------------");
            exStackTrace.append("\n");
            exStackTrace.append(s);
            text.setContent(exStackTrace.toString());
            //定义 @ 对象
//            OapiRobotSendRequest.At at = new OapiRobotSendRequest.At();
            // 设置消息类型
            req.setMsgtype("text");
            // 把消息放进来
            req.setText(text);
//            req.setAt(at);
            OapiRobotSendResponse rsp = client.execute(req);
            System.out.println(rsp.getBody());
        } catch (Exception ex) {
            log.error("发送钉钉消息异常", ex);
        }
    }

    private String trackExceptionWithRateLimit(Exception e) {
        String errorKey = e.getClass().getSimpleName();
        long currentTime = System.currentTimeMillis();
        
        errorRateLimit.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().longValue() > RATE_LIMIT_WINDOW
        );
        
        AtomicInteger counter = errorRateLimit.computeIfAbsent(errorKey, k -> new AtomicInteger(0));
        
        if (counter.incrementAndGet() > MAX_CALLS_PER_WINDOW) {
            log.warn("异常 {} 的AI分析已达到频率限制，跳过本次分析", errorKey);
            return "[AI分析已跳过：该类型异常分析次数过多]";
        }
        
        return trackException(e);
    }

    public String trackException(Exception e) {
        try {
            StringBuilder exStackTrace = new StringBuilder();
            StackTraceElement[] stackTrace = e.getStackTrace();
            for (int i = 0; i < 10 && i < stackTrace.length; i++) {
                exStackTrace.append(stackTrace[i]);
            }
            
            // 🔑 关键修改：使用 OllamaDirectService 替代 ZhipuAiUtil
            String aiResult = ollamaDirectService.analyzeException(e.getMessage(), exStackTrace.toString());
            
            log.error("捕获到全局异常:", e);
            log.error("Ollama分析解决方案 : {}", aiResult);
            return aiResult;
        } catch (Exception aiException) {
            log.error("AI分析异常失败，返回默认提示", aiException);
            return "[AI分析服务暂时不可用，请查看原始错误信息]";
        }
    }
}
