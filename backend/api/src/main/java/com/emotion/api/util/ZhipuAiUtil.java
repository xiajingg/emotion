package com.emotion.api.util;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.ChatCompletionCreateParams;
import ai.z.openapi.service.model.ChatCompletionResponse;
import ai.z.openapi.service.model.ChatMessage;
import ai.z.openapi.service.model.ChatMessageRole;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 智普AI工具类
 */
@Slf4j
public class ZhipuAiUtil {

    // 密钥不入库，由 Spring 启动时注入（见 com.emotion.api.config.ZhipuAiConfig）
    private static volatile String apiKey;
    private static final String MODEL = "glm-4.7-flash";

    private static volatile ZhipuAiClient client;

    /**
     * 注入 API Key。由 ZhipuAiConfig 在启动时调用，也可用于运行期轮换。
     */
    public static void setApiKey(String key) {
        apiKey = key;
        client = null; // 重置单例，确保用新密钥重建
    }

    /**
     * 获取客户端实例（单例模式）
     */
    private static ZhipuAiClient getClient() {
        if (client == null) {
            synchronized (ZhipuAiUtil.class) {
                if (client == null) {
                    if (apiKey == null || apiKey.isBlank()) {
                        throw new IllegalStateException(
                                "智谱 API Key 未配置：请设置 zhipu.api-key 或环境变量 ZHIPU_API_KEY");
                    }
                    client = ZhipuAiClient.builder().ofZHIPU()
                            .apiKey(apiKey)
                            .build();
                }
            }
        }
        return client;
    }

    /**
     * 发送聊天请求
     *
     * @param userMessage 用户消息
     * @return AI回复内容
     */
    public static String chat(String userMessage) {
        return chat(userMessage, null);
    }

    /**
     * 发送聊天请求（支持系统提示词）
     *
     * @param userMessage 用户消息
     * @param systemPrompt 系统提示词（可选）
     * @return AI回复内容
     */
    public static String chat(String userMessage, String systemPrompt) {
        try {
            ZhipuAiClient client = getClient();
            List<ChatMessage> messages = new ArrayList<>();
            
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messages.add(ChatMessage.builder()
                        .role(ChatMessageRole.SYSTEM.value())
                        .content(systemPrompt)
                        .build());
            }
            
            messages.add(ChatMessage.builder()
                    .role(ChatMessageRole.USER.value())
                    .content(userMessage)
                    .build());

            ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                    .model(MODEL)
                    .messages(messages)
                    .maxTokens(8192)
                    .temperature(0.7f)
                    .build();

            ChatCompletionResponse response = client.chat().createChatCompletion(request);

            if (response.isSuccess() && response.getData() != null 
                    && response.getData().getChoices() != null 
                    && !response.getData().getChoices().isEmpty()) {
                Object reply = response.getData().getChoices().get(0).getMessage();
                String result = reply != null ? reply.toString() : "";
                log.debug("智普AI回复: {}", result);
                return result;
            } else {
                log.error("智普AI请求失败: {}", response.getMsg());
                return "[AI服务响应异常]";
            }
        } catch (Exception e) {
            log.error("调用智普AI接口异常", e);
            return "[AI服务调用失败: " + e.getMessage() + "]";
        }
    }

    /**
     * 分析异常信息
     *
     * @param errorMessage 错误消息
     * @param stackTrace 堆栈信息
     * @return AI分析结果
     */
    public static String analyzeException(String errorMessage, String stackTrace) {
        String prompt = String.format(
                "你要根据下面的错误日志, 用50字以内给我输出两句话, " +
                "1.原因: 报错的代码具体位置和原因. " +
                "2. 建议解决方案: 解决方法\n\n" +
                "错误信息: %s\n\n" +
                "堆栈信息: %s",
                errorMessage, stackTrace
        );
        
        return chat(prompt);
    }
}
