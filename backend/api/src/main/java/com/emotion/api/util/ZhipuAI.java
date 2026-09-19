package com.emotion.api.util;

import cn.hutool.json.JSONUtil;
import com.emotion.api.dto.ZhipuImgResDTO;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.ChatCompletionRequest;
import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import com.zhipu.oapi.service.v4.model.ModelApiResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ZhipuAI {

    // 密钥不入库，由 Spring 启动时注入（见 com.emotion.api.config.ZhipuAiConfig）
    private static volatile String apiKey;
    private static volatile ClientV4 client;

    /**
     * 注入 API Key。由 ZhipuAiConfig 在启动时调用，也可用于运行期轮换。
     */
    public static void setApiKey(String key) {
        apiKey = key;
        client = null; // 重置单例，确保用新密钥重建
    }

    private static ClientV4 getClient() {
        if (client == null) {
            synchronized (ZhipuAI.class) {
                if (client == null) {
                    if (apiKey == null || apiKey.isBlank()) {
                        throw new IllegalStateException(
                                "智谱 API Key 未配置：请设置 zhipu.api-key 或环境变量 ZHIPU_API_KEY");
                    }
                    client = new ClientV4.Builder(apiKey)
                            .networkConfig(300, 100, 100, 100, TimeUnit.SECONDS)
                            .connectionPool(new okhttp3.ConnectionPool(8, 1, TimeUnit.SECONDS))
                            .build();
                }
            }
        }
        return client;
    }

    /**
     * 同步调用
     */
    public static String textToImaUrl(String text) {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), text+", 风格: 抽象");
        messages.add(chatMessage);
        String requestId = String.format(System.currentTimeMillis()+"", System.currentTimeMillis());

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model("cogview-3-flash")
                .stream(Boolean.FALSE)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .requestId(requestId)
                .build();
        ModelApiResponse invokeModelApiResp = getClient().invokeModelApi(chatCompletionRequest);
        ChatMessage message = invokeModelApiResp.getData().getChoices().get(0).getMessage();
        ZhipuImgResDTO bean = JSONUtil.toBean(message.toString(), ZhipuImgResDTO.class);
//        invokeModelApiResp.getData().getChoices().get(0).getMessage()._children.get("content").get(0);
        System.out.println("model output:" + invokeModelApiResp);
        return bean.getContent().get(0).getUrl();
    }
}
