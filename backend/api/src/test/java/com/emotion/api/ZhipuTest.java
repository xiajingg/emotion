package com.emotion.api;

import cn.hutool.json.JSONUtil;
import com.emotion.api.dto.AnalyzeEmotionsDTO;
import com.emotion.api.dto.ZhipuImgResDTO;
import com.emotion.api.util.ZhipuAI;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EmotionApplication.class)
public class ZhipuTest {

    // 密钥不入库：由 application-secret.yml 或环境变量 ZHIPU_API_KEY 提供
    @Value("${zhipu.api-key:${ZHIPU_API_KEY:}}")
    private String apiKey;

    private ClientV4 buildClient() {
        return new ClientV4.Builder(apiKey)
                .networkConfig(300, 100, 100, 100, TimeUnit.SECONDS)
                .connectionPool(new okhttp3.ConnectionPool(8, 1, TimeUnit.SECONDS))
                .build();
    }

    /**
     * 同步调用
     */
    @Test
    public  void testInvoke() {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), "将用户的输入转化为具体的视觉元素：首先，观察到用户是女性，这可能影响整体色调和服装设计。她吃了丰盛的大餐，心情愉悦，所以选择温暖明亮的颜色。结合规则1，喜悦的情绪对应明黄、珊瑚粉和水绿渐变。根据规则3，使用特写构图，将主体放在画面的60%位置，突出她的形象。在动态元素方面，采用花瓣飘落的轨迹作为自然动态元素，为场景增添生动感。最后，整体画面应传递出满足和幸福感，色彩明快，构图紧凑，细节丰富。");
        messages.add(chatMessage);
        String requestId = String.format("1111111111", System.currentTimeMillis());

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model("cogview-3-flash")
                .stream(Boolean.FALSE)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .requestId(requestId)
                .build();
        ModelApiResponse invokeModelApiResp = buildClient().invokeModelApi(chatCompletionRequest);
        ModelData data = invokeModelApiResp.getData();
        Object message = data.getChoices().get(0).getMessage();
        ZhipuImgResDTO bean = JSONUtil.toBean(message.toString(), ZhipuImgResDTO.class);
        System.out.println("model output:" + bean);
    }

    @Autowired
    private RestTemplate restTemplate;
    @Test
    public  void testInvokeStream() {
        String text = "性别:男, 心情: 今天被老板骂了, 不想干了";
        String s = restTemplate
                .getForObject("http://100.107.179.128:8090/api/ai/chatImg?text=" + text, String.class);
        String img = ZhipuAI.textToImaUrl(s+", 风格: 抽象");
        System.out.println(img);
    }
}
