package com.emotion.api.service;

import cn.hutool.json.JSONException;
import cn.hutool.json.JSONUtil;
import com.emotion.api.dto.EmotionAnalysisResponse;
import com.emotion.api.dto.EmotionResultResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Ollama直接API调用服务（不使用Spring AI）
 * 
 * 产品级重构：从打分工具转型为单次生成的情绪解压卡。
 */
@Slf4j
@Service
public class OllamaDirectService {

    private static final String OLLAMA_API_URL = "http://localhost:11434/api/chat";
    @Value("${ollama.vision-model:qwen3.5:9b}")
    private String visionModel;
    private static final java.util.List<String> DEFAULT_RELIEF_ACTIONS = Arrays.asList(
            "慢慢呼吸30秒",
            "把最担心的事写成一句话",
            "先离开当前刺激源喝口水"
    );
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)  // 增加到180秒，新Prompt字数更多
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    /**
     * 情绪解压卡 - 单次输入生成完整结果
     * 输出：emotion_tag, ai_comment, relief_actions, replies(high_eq/crazy/gentle), answer_book
     */
    public EmotionResultResponse analyzeEmotion(String text, String scene) {
        log.info("开始情绪分析 V3，文本长度: {}, 场景: {}", text.length(), scene);
        long startTime = System.currentTimeMillis();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "qwen3.5:9b");
        requestBody.put("stream", false);
        requestBody.put("think", false);
        
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", buildPromptV3(text, scene));
        requestBody.put("messages", new Object[]{message});

        try {
            RequestBody body = RequestBody.create(
                    JSONUtil.toJsonStr(requestBody),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(OLLAMA_API_URL)
                    .post(body)
                    .build();

            log.info("发送请求到Ollama API (V3)...");
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("HTTP请求失败: " + response.code());
                }

                String responseBody = response.body().string();
                log.info("Ollama原始响应(V3): {}", responseBody);

                Map<String, Object> responseMap = JSONUtil.toBean(responseBody, Map.class);
                Map<String, Object> messageObj = (Map<String, Object>) responseMap.get("message");
                
                if (messageObj == null || !messageObj.containsKey("content")) {
                    throw new RuntimeException("Ollama返回的响应中没有message.content字段");
                }

                String content = (String) messageObj.get("content");
                log.info("AI回复内容(V3): [{}]", content);

                if (content == null || content.trim().isEmpty()) {
                    throw new RuntimeException("AI返回空响应");
                }

                String jsonStr = extractAndRepairJson(content);
                log.info("提取后的JSON(V3): {}", jsonStr);

                EmotionResultResponse result = normalizeEmotionResult(JSONUtil.toBean(jsonStr, EmotionResultResponse.class));

                long elapsed = System.currentTimeMillis() - startTime;
                log.info("情绪分析V3完成，耗时: {}ms, 标签: {}, 答案之书: {}", 
                        elapsed, result.getEmotion_tag(), result.getAnswer_book());

                return result;
            }

        } catch (Exception e) {
            log.error("情绪分析V3失败，耗时: {}ms", System.currentTimeMillis() - startTime, e);
            return fallbackEmotionResult(text, scene);
        }
    }

    public EmotionResultResponse analyzeEmotion(String text, String scene, String taskType, String scenarioKey) {
        return analyzeEmotion(text, scene);
    }

    private EmotionResultResponse normalizeEmotionResult(EmotionResultResponse result) {
        if (result == null) {
            return fallbackEmotionResult("", "");
        }
        if (result.getEmotion_tag() == null || result.getEmotion_tag().trim().isEmpty()) {
            result.setEmotion_tag("情绪打结中");
        }
        if (result.getAi_comment() == null || result.getAi_comment().trim().isEmpty()) {
            result.setAi_comment("你现在不是矫情，是压力真的堆到需要被看见了。先别急着硬扛。");
        }
        if (result.getRelief_actions() == null || result.getRelief_actions().isEmpty()) {
            result.setRelief_actions(DEFAULT_RELIEF_ACTIONS);
        } else if (result.getRelief_actions().size() > 3) {
            result.setRelief_actions(result.getRelief_actions().subList(0, 3));
        }
        if (result.getReplies() == null) {
            result.setReplies(new EmotionResultResponse.Replies());
        }
        if (result.getReplies().getHigh_eq() == null || result.getReplies().getHigh_eq().trim().isEmpty()) {
            result.getReplies().setHigh_eq("我理解你的意思，这件事我需要一点时间消化，我们晚点再好好说。");
        }
        if (result.getReplies().getCrazy() == null || result.getReplies().getCrazy().trim().isEmpty()) {
            result.getReplies().setCrazy("这件事已经影响到我了，我希望我们能把边界和期待说清楚。");
        }
        if (result.getReplies().getGentle() == null || result.getReplies().getGentle().trim().isEmpty()) {
            result.getReplies().setGentle("我现在有点难受，但我愿意好好沟通，也希望你能认真听我说完。");
        }
        if (result.getAnswer_book() == null || result.getAnswer_book().trim().isEmpty()) {
            result.setAnswer_book("先稳住自己");
        }
        return result;
    }

    private EmotionResultResponse fallbackEmotionResult(String text, String scene) {
        EmotionResultResponse result = new EmotionResultResponse();
        result.setEmotion_tag("情绪打结中");
        if (containsCrisisText(text)) {
            result.setAi_comment("你现在承受的东西可能已经超过一个人能消化的范围，请立刻联系身边可信任的人或当地紧急帮助渠道。");
            result.setRelief_actions(Arrays.asList(
                    "先把危险物品移远",
                    "马上联系一个可信任的人",
                    "必要时联系当地紧急帮助渠道"
            ));
        } else {
            result.setAi_comment("你现在不是矫情，是压力真的堆到需要被看见了。先别急着硬扛。");
            result.setRelief_actions(DEFAULT_RELIEF_ACTIONS);
        }
        EmotionResultResponse.Replies replies = new EmotionResultResponse.Replies();
        replies.setHigh_eq("我理解你的意思，这件事我需要一点时间消化，我们晚点再好好说。");
        replies.setCrazy("这件事已经影响到我了，我希望我们能把边界和期待说清楚。");
        replies.setGentle("我现在有点难受，但我愿意好好沟通，也希望你能认真听我说完。");
        result.setReplies(replies);
        result.setAnswer_book("先稳住自己");
        return result;
    }

    private boolean containsCrisisText(String text) {
        if (text == null) return false;
        return text.contains("自杀")
                || text.contains("不想活")
                || text.contains("活不下去")
                || text.contains("结束生命")
                || text.contains("伤害自己");
    }

    /**
     * 旧版情绪分析（兼容保留）
     * @deprecated 请使用 analyzeEmotion(text, scene)
     */
    @Deprecated
    public EmotionAnalysisResponse analyzeEmotion(String text) {
        log.info("开始直接调用Ollama API进行情绪分析，文本长度: {}", text.length());
        long startTime = System.currentTimeMillis();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "qwen3.5:9b");
        requestBody.put("stream", false);
        requestBody.put("think", false);
        
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", buildPromptV2(text));
        requestBody.put("messages", new Object[]{message});

        try {
            RequestBody body = RequestBody.create(
                    JSONUtil.toJsonStr(requestBody),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(OLLAMA_API_URL)
                    .post(body)
                    .build();

            log.info("发送请求到Ollama API...");
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("HTTP请求失败: " + response.code());
                }

                String responseBody = response.body().string();
                log.info("Ollama原始响应: {}", responseBody);

                Map<String, Object> responseMap = JSONUtil.toBean(responseBody, Map.class);
                Map<String, Object> messageObj = (Map<String, Object>) responseMap.get("message");
                
                if (messageObj == null || !messageObj.containsKey("content")) {
                    throw new RuntimeException("Ollama返回的响应中没有message.content字段");
                }

                String content = (String) messageObj.get("content");
                log.info("AI回复内容: [{}]", content);

                if (content == null || content.trim().isEmpty()) {
                    throw new RuntimeException("AI返回空响应");
                }

                String jsonStr = extractAndRepairJson(content);
                log.info("提取后的JSON: {}", jsonStr);

                EmotionAnalysisResponse result = JSONUtil.toBean(jsonStr, EmotionAnalysisResponse.class);

                long elapsed = System.currentTimeMillis() - startTime;
                log.info("情绪分析完成，耗时: {}ms, 分数: {}, 情绪: {}, 建议: {}", 
                        elapsed, result.getScore(), result.getEmotion(), result.getSuggestion());

                return result;
            }

        } catch (Exception e) {
            log.error("情绪分析失败，耗时: {}ms", System.currentTimeMillis() - startTime, e);
            throw new RuntimeException("情绪分析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 情绪分析 V2 - 使用优化后的 Prompt（已合并到主方法，保留用于兼容）
     * @deprecated 请直接使用 analyzeEmotion(text, scene) 方法
     */
    @Deprecated
    public EmotionAnalysisResponse analyzeEmotionV2(String text) {
        return analyzeEmotion(text);
    }

    /**
     * 异常分析 - 用于错误诊断和解决方案生成
     * @param errorMessage 错误消息
     * @param stackTrace 堆栈跟踪信息
     * @return AI 生成的分析和解决方案
     */
    public String analyzeException(String errorMessage, String stackTrace) {
        log.info("开始调用Ollama API进行异常分析");
        long startTime = System.currentTimeMillis();

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "qwen3.5:9b");
        requestBody.put("stream", false);
        requestBody.put("think", false);  // 关闭思考模式
        
        String prompt = String.format(
            "你是一个Java后端开发专家。请分析以下异常并提供解决方案。\n\n" +
            "【错误信息】\n%s\n\n" +
            "【堆栈跟踪】\n%s\n\n" +
            "请以简洁的中文提供：\n" +
            "1. 问题原因分析（1-2句话）\n" +
            "2. 解决方案（具体步骤）\n" +
            "3. 预防建议（可选）\n\n" +
            "要求：\n" +
            "- 语言简洁明了，避免冗长\n" +
            "- 方案切实可行\n" +
            "- 总字数控制在200字以内",
            errorMessage,
            stackTrace
        );
        
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        requestBody.put("messages", new Object[]{message});

        try {
            // 发送HTTP请求
            RequestBody body = RequestBody.create(
                    JSONUtil.toJsonStr(requestBody),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(OLLAMA_API_URL)
                    .post(body)
                    .build();

            log.info("发送异常分析请求到Ollama API...");
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("HTTP请求失败: " + response.code());
                }

                String responseBody = response.body().string();
                log.debug("Ollama原始响应: {}", responseBody);

                // 解析响应
                Map<String, Object> responseMap = JSONUtil.toBean(responseBody, Map.class);
                Map<String, Object> messageObj = (Map<String, Object>) responseMap.get("message");
                
                if (messageObj == null || !messageObj.containsKey("content")) {
                    throw new RuntimeException("Ollama返回的响应中没有message.content字段");
                }

                String content = (String) messageObj.get("content");
                log.info("异常分析完成，耗时: {}ms", System.currentTimeMillis() - startTime);

                return content;
            }

        } catch (Exception e) {
            log.error("异常分析失败，耗时: {}ms", System.currentTimeMillis() - startTime, e);
            return "[AI分析服务暂时不可用，请查看原始错误信息]";
        }
    }

    /**
     * 构建 Prompt V3 - 情绪解压卡生成器。
     * 不做多轮聊天，不做心理诊断；一次性输出可执行的解压动作和回复建议。
     */
    private String buildPromptV3(String text, String scene) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("# Role\n");
        prompt.append("你是\"情绪解压卡\"生成器。用户只会输入一次烦恼，你必须一次性给出情绪识别、共情解释、3条即时解压动作和可直接复制的回复话术。\n\n");
        
        prompt.append("# Rules & Constraints\n");
        prompt.append("1. 不要追问，不要引导多轮聊天；一次输出完整结果。\n");
        prompt.append("2. 不做心理咨询、医学诊断或治疗承诺，不使用\"抑郁症\"等诊断性表述。\n");
        prompt.append("3. 语言温和、具体、站在用户这边，禁止爹味说教，避免\"你应该\"、\"首先其次\"等AI味句式。\n");
        prompt.append("4. 如果用户出现自伤、自杀、活不下去等危机表达，温和建议立刻联系身边可信任的人或当地紧急帮助渠道，不提供危险细节。\n");
        prompt.append("5. 必须严格遵循指定 JSON 格式输出，不要输出任何多余解释、Markdown 标记或前言后语。\n\n");

        prompt.append("# 输出格式\n");
        prompt.append("只输出一个纯 JSON 对象，不要用 ```json 包裹：\n");
        prompt.append("{\"emotion_tag\":\"情绪标签\",\"ai_comment\":\"共情解释\",\"relief_actions\":[\"解压动作1\",\"解压动作2\",\"解压动作3\"],\"replies\":{\"high_eq\":\"高情商回复\",\"crazy\":\"表达边界回复\",\"gentle\":\"温柔回复\"},\"answer_book\":\"简短指引\"}\n\n");
        
        prompt.append("# 字段级生成细则\n\n");
        
        prompt.append("## emotion_tag（情绪标签）\n");
        prompt.append("- 4-8个中文字符，生动但不过度娱乐化\n");
        prompt.append("- 禁止使用：悲伤、愤怒、开心、焦虑等干瘪单词\n");
        prompt.append("- 参考风格：压力满格中、委屈堵心中、边界被撞疼、睡前内耗中\n\n");
        
        prompt.append("## ai_comment（共情解释）\n");
        prompt.append("- 30-60个中文字符，先接住用户情绪，再解释可能的压力来源\n");
        prompt.append("- 不评价对错，不命令用户，不做诊断\n");
        prompt.append("- 参考风格：\"你现在不是矫情，是压力和不确定感一起挤上来了，先把自己稳住。\"\n\n");

        prompt.append("## relief_actions（即时解压动作）\n");
        prompt.append("- 必须返回3条，每条8-18个中文字符\n");
        prompt.append("- 必须是马上能做的小动作，具体、低门槛、非医疗化\n");
        prompt.append("- 参考：慢慢呼吸30秒、把担心写成一句话、先离开现场喝口水\n\n");
        
        prompt.append("## replies（回复建议）\n");
        prompt.append("根据用户输入，提供三种风格话术，每句20-45字，必须是可直接复制发送的口语。\n");
        prompt.append("- high_eq（高情商回复）：不卑不亢，得体化解尴尬，温柔坚守底线\n");
        prompt.append("  示例：\"收到啦，不过这个需求目前排期比较满，我们下周一对齐一下优先级可以吗？\"\n");
        prompt.append("- crazy（表达边界回复）：直接但不攻击，清楚表达不舒服、需求或底线。字段名必须仍叫 crazy，用于兼容旧前端。\n");
        prompt.append("  示例：\"这件事已经影响到我了，我希望我们能把边界和期待说清楚。\"\n");
        prompt.append("- gentle（温柔回复）：表达感受和理解，适合关系修复或缓和语气\n");
        prompt.append("  示例：\"我知道你最近压力很大，没关系的，我等你忙完我们再好好聊聊好吗？\"\n\n");
        
        // 场景约束注入
        if (scene != null && !scene.isEmpty()) {
            switch (scene) {
                case "压力焦虑":
                    prompt.append("【场景约束：压力焦虑】侧重帮助用户先稳定身体感受，解压动作要非常具体，回复话术可以偏自我表达。\n\n");
                    break;
                case "职场委屈":
                    prompt.append("【场景约束：职场委屈】用户正在面对工作、领导或同事。回复话术必须专业、体面、有边界，避免情绪化攻击。\n\n");
                    break;
                case "恋爱焦虑":
                    prompt.append("【场景约束：恋爱焦虑】用户面对伴侣或暧昧对象。回复要能表达感受和需求，避免操控、冷暴力或攻击。\n\n");
                    break;
                case "不知道怎么回":
                    prompt.append("【场景约束：不知道怎么回】重点产出可直接复制的话术，语气自然，不要解释太多。\n\n");
                    break;
                case "睡前emo":
                    prompt.append("【场景约束：睡前emo】侧重睡前放下和停止内耗，解压动作要安静、低刺激、适合睡前。\n\n");
                    break;
                default:
                    prompt.append("【场景约束：通用情绪解压】先接住情绪，再给可执行动作和可复制话术。\n\n");
                    break;
            }
        }
        
        prompt.append("## answer_book（简短指引）\n");
        prompt.append("- 4-8个中文字符，温柔、坚定、适合保存\n");
        prompt.append("- 参考风格：先稳住自己、慢慢来就好、别急着内耗、边界也很重要\n\n");
        
        prompt.append("# 用户输入\n");
        prompt.append(text);

        return prompt.toString();
    }

    public EmotionResultResponse analyzeDrawingEmotion(String drawingMeta, byte[] imageBytes) {
        log.info("开始涂鸦图片情绪分析，图片大小: {} bytes, 模型: {}", imageBytes == null ? 0 : imageBytes.length, visionModel);
        long startTime = System.currentTimeMillis();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", visionModel);
        requestBody.put("stream", false);
        requestBody.put("think", false);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", buildDrawingPrompt(drawingMeta));
        message.put("images", new String[]{java.util.Base64.getEncoder().encodeToString(imageBytes)});
        requestBody.put("messages", new Object[]{message});

        try {
            RequestBody body = RequestBody.create(
                    JSONUtil.toJsonStr(requestBody),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(OLLAMA_API_URL)
                    .post(body)
                    .build();

            log.info("发送涂鸦图片请求到Ollama API...");

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("HTTP请求失败: " + response.code());
                }

                String responseBody = response.body().string();
                log.info("Ollama涂鸦图片原始响应: {}", responseBody);

                Map<String, Object> responseMap = JSONUtil.toBean(responseBody, Map.class);
                Map<String, Object> messageObj = (Map<String, Object>) responseMap.get("message");

                if (messageObj == null || !messageObj.containsKey("content")) {
                    throw new RuntimeException("Ollama返回的响应中没有message.content字段");
                }

                String content = (String) messageObj.get("content");
                if (content == null || content.trim().isEmpty()) {
                    throw new RuntimeException("AI返回空响应");
                }

                String jsonStr = extractAndRepairJson(content);
                EmotionResultResponse result = normalizeEmotionResult(JSONUtil.toBean(jsonStr, EmotionResultResponse.class));

                long elapsed = System.currentTimeMillis() - startTime;
                log.info("涂鸦图片情绪分析完成，耗时: {}ms, 标签: {}", elapsed, result.getEmotion_tag());
                return result;
            }
        } catch (Exception e) {
            log.error("涂鸦图片情绪分析失败，耗时: {}ms", System.currentTimeMillis() - startTime, e);
            return fallbackEmotionResult(drawingMeta, "情绪涂鸦");
        }
    }

    private String buildDrawingPrompt(String drawingMeta) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("# Role\n");
        prompt.append("你是\"情绪涂鸦解压卡\"的核心生成器。用户画的通常是非常简单、幼稚、抽象的几笔线段，可能是爱心、皇冠、星星、脸、房子、树、太阳、云、箭头等符号。你的任务不是评价画得像不像，而是先识别用户想画的符号，再把这个符号和当下感受翻译成一张让用户觉得\"它懂我\"、\"我想保存\"、\"我想分享\"的情绪卡。\n\n");

        prompt.append("# Product Goal\n");
        prompt.append("- 激活：让第一次使用的用户在10秒内获得一个准确、温柔、可执行的结果。\n");
        prompt.append("- 留存：让用户愿意下次情绪说不清时再来画一张。\n");
        prompt.append("- 分享：结果要适合做成卡片转发，但不能猎奇、吓人或像算命骗局。\n\n");

        prompt.append("# Rules & Constraints\n");
        prompt.append("1. 不要追问，不要引导多轮聊天；一次输出完整结果。\n");
        prompt.append("2. 不做心理咨询、医学诊断或治疗承诺，不使用\"抑郁症\"等诊断性表述。\n");
        prompt.append("3. 语言温和、具体、站在用户这边，禁止爹味说教，避免\"你应该\"、\"首先其次\"等AI味句式。\n");
        prompt.append("4. 如果用户出现自伤、自杀、活不下去等危机表达，温和建议立刻联系身边可信任的人或当地紧急帮助渠道，不提供危险细节。\n");
        prompt.append("5. 必须严格遵循指定 JSON 格式输出，不要输出任何多余解释、Markdown 标记或前言后语。\n\n");

        prompt.append("# 输出格式\n");
        prompt.append("只输出一个纯 JSON 对象，不要用 ```json 包裹：\n");
        prompt.append("{\"emotion_tag\":\"情绪标签\",\"ai_comment\":\"它懂我\",\"relief_actions\":[\"解压动作1\",\"解压动作2\",\"解压动作3\"],\"replies\":{\"high_eq\":\"高情商回复\",\"crazy\":\"表达边界回复\",\"gentle\":\"温柔回复\"},\"answer_book\":\"简短指引\"}\n\n");

        prompt.append("# 涂鸦解读规则\n");
        prompt.append("- 简单线条：看到的东西可能就是用户当下需要的或被击中的事物。\n");
        prompt.append("- 高密度/凌乱：用户当前压力较大或思绪较杂。\n");
        prompt.append("- 强烈线条（重笔、多次描边、大力涂画）：与某种强烈情绪有关，可能是无力、愤怒或想要被看见。\n");
        prompt.append("- 留白大/线条少：用户可能需要更多空间或时间。\n");
        prompt.append("- 有颜色时：暖色偏向外化、冷色偏向内收、黑色/深色可能对应沉重或复杂的状态。\n");
        prompt.append("- 解释措辞请用推测性语言（\"看起来\"、\"似乎\"、\"可能\"），不要绝对化。\n\n");

        prompt.append("## emotion_tag（情绪标签）\n");
        prompt.append("- 4-8个中文字符，结合涂鸦符号和感受，生动但不过度娱乐化\n");
        prompt.append("- 参考：画个圈圈中、心里在下雨、皇冠还没掉、被戳破的气球\n\n");

        prompt.append("## ai_comment（共情解释）\n");
        prompt.append("- 20-40个中文字符，温柔翻译涂鸦给人的感受\n");
        prompt.append("- 示例：\"你画的这个简单皇冠，不是在乎权力，是想要被认真对待。\"\n\n");

        prompt.append("## relief_actions（即时解压动作）\n");
        prompt.append("- 综合涂鸦感受给出3条具体动作\n\n");

        prompt.append("## replies（回复建议）\n");
        prompt.append("- 给出三种风格话术，符合涂鸦翻译出的情感状态\n\n");

        prompt.append("## answer_book（简短指引）\n");
        prompt.append("- 4-8个中文字符，温柔、坚定、适合保存\n\n");

        prompt.append("# 用户涂鸦信息\n");
        prompt.append(drawingMeta);

        return prompt.toString();
    }

    // ===== 以下为旧版 Prompt，保留用于兼容旧版接口 =====

    private String buildPrompt(String text) {
        return "请分析以下文本的情绪，返回JSON格式：{\"score\":整数,\"suggestion\":\"字符串\"}\n" +
                "评分标准：1-10绝望,11-20痛苦,21-30愤怒,31-40沮丧,41-50平静,51-60好奇,61-70满足,71-80开心,81-90兴奋,91-100狂喜\n" +
                "要求：\n" +
                "1. score: 根据情绪强度给出1-100的整数分数\n" +
                "2. suggestion: 输出一段温暖、合时宜的回复或建议（20-50字），像朋友一样给予安慰、鼓励或共鸣\n" +
                "   - 负面情绪：给予安慰、理解和支持\n" +
                "   - 正面情绪：给予肯定、鼓励和分享喜悦\n" +
                "   - 中性情绪：给予温暖的关怀\n" +
                "文本：" + text;
    }

    private String buildPromptV2(String text) {
        return "请分析以下文本的情绪，并严格返回JSON格式：\n" +
                "{\"score\":整数,\"emotion\":\"字符串\",\"suggestion\":\"字符串\"}\n\n" +
                "情绪评分标准：\n" +
                "1-10：绝望\n11-20：痛苦\n21-30：压抑\n31-40：情绪透支\n41-50：平静\n" +
                "51-60：轻松\n61-70：满足\n71-80：开心\n81-90：兴奋\n91-100：狂喜\n\n" +
                "要求：\n" +
                "1. score: 返回1-100之间的整数，根据用户真实情绪状态判断，不要机械乐观\n" +
                "2. emotion: 返回一个有画面感、容易共鸣的情绪标签，不要使用\"开心\"\"难过\"等普通词\n" +
                "3. suggestion: 输出80~150字，不要像心理医生，要让用户产生\"它懂我\"的感觉\n\n" +
                "文本：\n" + text;
    }

    /**
     * 从AI响应中提取JSON并修复常见格式问题
     * <p>
     * LLM输出的JSON经常有 unterminated string、截断、多余字符等问题。
     * 此方法先提取纯JSON文本，再通过逐步结构分析找到最后一个完整的JSON对象。
     */
    private String extractAndRepairJson(String response) {
        String jsonStr = extractJsonFromResponse(response);

        // 先尝试直接解析
        try {
            JSONUtil.parseObj(jsonStr);
            return jsonStr;
        } catch (JSONException e) {
            log.warn("JSON解析失败，尝试结构修复: {}", e.getMessage());
        }


        // 修复策略1：规范化 Unicode 引号（中文左引/右引 -> ASCII 引号）
        String normalized = jsonStr.replace('\u201C', '"').replace('\u201D', '"');
        if (!normalized.equals(jsonStr)) {
            try {
                JSONUtil.parseObj(normalized);
                log.info("JSON修复成功（Unicode引号规范化）");
                return normalized;
            } catch (JSONException ignored) {
                // 规范化后仍无效，继续后续修复
            }
            jsonStr = normalized;
        }

        // 修复策略：跟踪brace深度，正确处理字符串中的转义，找到最后一个完整的JSON对象
        boolean inString = false;
        boolean escaped = false;
        int depth = 0;
        int lastValidEnd = -1;

        for (int i = 0; i < jsonStr.length(); i++) {
            char c = jsonStr.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }

            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    lastValidEnd = i + 1;
                }
            }
        }

        if (lastValidEnd > 0) {
            String repaired = jsonStr.substring(0, lastValidEnd);
            try {
                JSONUtil.parseObj(repaired);
                log.info("JSON修复成功，截断至位置: {}", lastValidEnd);
                return repaired;
            } catch (JSONException e) {
                log.warn("截断修复后JSON仍然无效: {}", e.getMessage());
            }
        }

        // 如果所有修复都失败，记录原始JSON并抛出异常（由调用方的catch处理走fallback）
        log.error("无法修复AI返回的JSON，原始内容: {}", jsonStr);
        throw new JSONException("无法修复AI返回的JSON: " + jsonStr);
    }

    /**
     * 从AI响应中提取JSON内容
     */
    private String extractJsonFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "{}";
        }

        String trimmed = response.trim();

        // 方法1：提取Markdown代码块 ```json ... ```
        int jsonStart = trimmed.indexOf("```");
        if (jsonStart != -1) {
            int jsonEnd = trimmed.lastIndexOf("```");
            if (jsonEnd > jsonStart) {
                String jsonContent = trimmed.substring(jsonStart + 3, jsonEnd).trim();
                if (jsonContent.toLowerCase().startsWith("json")) {
                    jsonContent = jsonContent.substring(4).trim();
                }
                return jsonContent;
            }
        }

        // 方法2：直接查找JSON对象 { ... }
        int braceStart = trimmed.indexOf('{');
        int braceEnd = trimmed.lastIndexOf('}');
        if (braceStart != -1 && braceEnd > braceStart) {
            return trimmed.substring(braceStart, braceEnd + 1);
        }

        // 方法3：返回原始内容
        return trimmed;
    }
}
