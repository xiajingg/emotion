package com.emotion.api.config;

import com.emotion.api.util.ZhipuAI;
import com.emotion.api.util.ZhipuAiUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 把配置中的智谱 API Key 注入到静态工具类
 * {@link ZhipuAiUtil}（文本对话）与 {@link ZhipuAI}（文生图）。
 *
 * <p>密钥来源优先级：{@code zhipu.api-key}（application-*.yml）→ 环境变量 {@code ZHIPU_API_KEY}。
 * 真实密钥只写在已 gitignore 的 application-secret.yml 中，不入库。
 */
@Configuration
public class ZhipuAiConfig {

    @Value("${zhipu.api-key:${ZHIPU_API_KEY:}}")
    private String apiKey;

    @PostConstruct
    public void init() {
        ZhipuAiUtil.setApiKey(apiKey);
        ZhipuAI.setApiKey(apiKey);
    }
}
