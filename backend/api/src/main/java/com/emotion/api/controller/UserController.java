package com.emotion.api.controller;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emotion.api.config.BaseResult;
import com.emotion.api.config.EncryptionUtil;
import com.emotion.api.config.JwtTokenUtil;
import com.emotion.api.config.user.CurrentUser;
import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.dto.*;
import com.emotion.api.dto.UserProfileUpdateDTO;
import com.emotion.api.repository.dao.rds.UserTextInteractionMapper;
import com.emotion.api.repository.dao.rds.ImageUploadRecordMapper;
import com.emotion.api.repository.po.ImageUploadRecord;
import com.emotion.api.repository.po.User;
import com.emotion.api.repository.po.WechatUser;
import com.emotion.api.repository.po.UserFunctionRecord;
import com.emotion.api.repository.po.UserTextInteraction;
import com.emotion.api.repository.po.WechatArticle;
import com.emotion.api.repository.po.UserUsageLog;
import com.emotion.api.repository.mapper.UserUsageLogMapper;
import com.emotion.api.service.*;
import com.emotion.api.service.impl.WxacodeService;
import com.emotion.api.util.RustFsUtil;
import com.emotion.api.util.DingTalkMsg;
import com.emotion.api.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;  // ✅ 新增
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author xiajing
 * @since 2024-03-29
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private IUserService userService;

//    @Autowired
//    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private WechatUserService wechatUserService;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private WechatArticleService wechatArticleService;
    @Autowired
    private UserTextInteractionMapper userTextInteractionMapper;
    @Autowired
    private ImageUploadRecordMapper imageUploadRecordMapper;
    @Autowired
    private RustFsUtil rustFsUtil;
    @Autowired
    private IUserFunctionRecordService userFunctionRecordService;

    // 微信小程序凭据 —— 不入库，见 application-secret.yml 或环境变量
    @Value("${wechat.app-id}")
    private String wechatAppId;

    @Value("${wechat.app-secret}")
    private String wechatAppSecret;

    @Autowired
    private UserUsageLogMapper userUsageLogMapper;
    @Autowired
    private com.emotion.api.service.OllamaChatService ollamaChatService;
    @Autowired
    private com.emotion.api.service.OllamaDirectService ollamaDirectService;  // ✅ 新增
    @Autowired
    private WxacodeService wxacodeService;  // ✅ 微信小程序码服务

    @PostMapping("/login")
    public BaseResult<String> loginUser(@RequestBody User loginRequest) {
        User userByUserName = userService.findUserByUserName(loginRequest.getUsername());
        if (ObjectUtil.isNull(userByUserName)) {
            return BaseResult.error("用户不存在");
        }
        // 把传过来的密码加密, 然后跟数据库中的密码进行比对
        try {
            if (!EncryptionUtil.encrypt(loginRequest.getPassword() + userByUserName.getSalt()).equals(userByUserName.getPassword())) {
                return BaseResult.error("密码错误");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

//        Authentication authentication = null;
//        try {
//            authentication = authenticationManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(
//                            userByUserName.getUsername(),
//                            EncryptionUtil.encrypt(userByUserName.getPassword())
//                    )
//            );
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }

        // 如果认证成功，生成JWT token并返回
        String jwt = jwtTokenUtil.generateToken(JSONUtil.toJsonStr(userByUserName.getUsername()));
        // todo:存入redis
        return BaseResult.success(jwt);
    }

    /**
     * 注册接口, 注册成功返回JWT token
     *
     * @param registerRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResult<String> registerUser(@RequestBody User registerRequest) {
        // 判断用户名是否存在
        User userByUserName = userService.findUserByUserName(registerRequest.getUsername());
        if (ObjectUtil.isNotNull(userByUserName)) {
            return BaseResult.error("用户名已存在");
        }
        // 生成随机4位盐
        registerRequest.setSalt(EncryptionUtil.getRandomSalt(4));
        // 加密密码
        try {
            registerRequest.setPassword(EncryptionUtil.encrypt(registerRequest.getPassword() + registerRequest.getSalt()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        userService.save(registerRequest);
        // 如果认证成功，生成JWT token并返回
        String jwt = jwtTokenUtil.generateToken(JSONUtil.toJsonStr(registerRequest.getUsername()));
        // todo:jwt存入redis
        return BaseResult.success(jwt);
    }

    @GetMapping("/test")
    public String test(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return JSONUtil.toJsonPrettyStr(userService.list()) + user.getUsername();
    }

    /**
     * curl --location --request
     * 把这个请求封装成get请求
     *
     * @param code
     * @return
     */
    @GetMapping("/api/getOpenId")
    public void getOpenId(@RequestParam("code") String code) {
        String url = "https://api.weixin.qq.com/sns/jscode2session?" +
                "appid=" + wechatAppId +
                "&secret=" + wechatAppSecret +
                "&js_code=" + code +
                "&grant_type=authorization_code";
        // 用okhttp工具调用这个接口
        Request request = new Request.Builder()
                .url(url)
                .build();
        OkHttpClient client = new OkHttpClient();
        try {
            Response response = client.newCall(request).execute();
            String userOpenId = response.body().string();
            Map userMap = JSONUtil.toBean(userOpenId, Map.class);
            log.info("response: {}", JSONUtil.toJsonStr(response));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * curl --location --request
     * 把这个请求封装成get请求
     *
     * @param code
     * @return
     */
    @GetMapping("/api/v1/getOpenId")
    public BaseResult<String> getOpenIdV1(@RequestParam("code") String code) {
        try {
            return BaseResult.success(wechatUserService.getToken(code));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * curl --location --request
     * 把这个请求封装成get请求
     *
     * @param code
     * @return
     */
    @GetMapping("/api/v2/getOpenId")
    public BaseResult<TokenAndIdDTO> getOpenIdV2(@RequestParam("code") String code) {
        try {
            return BaseResult.success(wechatUserService.getTokenAndId(code));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    private Environment environment;

    private String getActiveProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles[0].equals("prod")) {
            return "prod";
        }
        return "dev";
    }

    @PostMapping("/api/v1/submitText")
    public BaseResult<Map> submitText(@CurrentUser UserPrincipal userPrincipal, @RequestBody SubmitTextDTO text) {
        log.info("{} : {}", JSONUtil.toJsonStr(userPrincipal), JSONUtil.toJsonStr(text));
        // 区别普通提交还是签到提交
        if (text.getType() == 1) {
            String date = DateUtil.now();
            // 补签到校验，补签到时间不能超过当前时间
            if (text.getSupplementarySignIn() == 1) {
                if (ObjectUtil.isNull(text.getSupplementarySignInTime())) {
                    return BaseResult.error("400", "补签时间不正确");
                }
                // 获取补签的时间，用hutool的方法判断如果date大于等于今天，返回签到时间不正确
                if (DateUtil.parse(date).isBeforeOrEquals(DateUtil.parse(text.getSupplementarySignInTime()))) {
                    return BaseResult.error("400", "补签时间不正确");
                }
                date = text.getSupplementarySignInTime();
            }
            // 使用data + userId查询text表是否有数据
            LambdaQueryWrapper<UserTextInteraction> queryWrapper = new LambdaQueryWrapper<>();
            // 设置查询条件，这里使用eq方法表示等于userId
            queryWrapper.eq(UserTextInteraction::getUserId, userPrincipal.getUserId());
            // 把date放到一个数组参数里去
            queryWrapper.apply("DATE_FORMAT(sign_in_time, '%Y-%m-%d') = DATE_FORMAT({0}, '%Y-%m-%d')", date);
            List<UserTextInteraction> userTextInteractions = userTextInteractionMapper.selectList(queryWrapper);
            if (CollUtil.isNotEmpty(userTextInteractions)) {
                return BaseResult.error("400", "已签到过");
            }
        }
        String inputText = text.getText();
        if (inputText.length() > 2000) {
            return BaseResult.error("500", "输入文本过长");
        }
        
        // 产品级重构：生成单次完整的情绪解压卡，传入可选场景标签
        com.emotion.api.dto.EmotionResultResponse emotionResult = null;
        try {
            String scene = text.getScene();
            emotionResult = ollamaDirectService.analyzeEmotion(text.getText(), scene, text.getTaskType(), text.getScenarioKey());
        } catch (Exception e) {
            log.error("Ollama情绪分析V3失败", e);
            return BaseResult.error("500", "AI服务异常");
        }

        if (emotionResult == null || emotionResult.getEmotion_tag() == null) {
            return BaseResult.error("500", "服务器异常");
        }
        Map analyzeEmotions = new HashMap();
        analyzeEmotions.put("emotion_tag", emotionResult.getEmotion_tag());
        analyzeEmotions.put("ai_comment", emotionResult.getAi_comment());
        analyzeEmotions.put("relief_actions", emotionResult.getRelief_actions());
        Map replies = new HashMap();
        if (emotionResult.getReplies() != null) {
            replies.put("high_eq", emotionResult.getReplies().getHigh_eq());
            replies.put("crazy", emotionResult.getReplies().getCrazy());
            replies.put("gentle", emotionResult.getReplies().getGentle());
            replies.put("sarcastic", emotionResult.getReplies().getSarcastic());
            replies.put("safe", emotionResult.getReplies().getSafe());
            replies.put("firm", emotionResult.getReplies().getFirm());
            replies.put("short", emotionResult.getReplies().getShort_reply());
            replies.put("short_reply", emotionResult.getReplies().getShort_reply());
        }
        analyzeEmotions.put("replies", replies);
        analyzeEmotions.put("answer_book", emotionResult.getAnswer_book());
        analyzeEmotions.put("task_type", emotionResult.getTask_type());
        analyzeEmotions.put("pain_point", emotionResult.getPain_point());
        analyzeEmotions.put("stabilize_action", emotionResult.getStabilize_action());
        analyzeEmotions.put("dont_say", emotionResult.getDont_say());
        analyzeEmotions.put("next_time_tip", emotionResult.getNext_time_tip());

        Map result = new HashMap();
        result.put("data", analyzeEmotions);

        // 兼容旧版数据库存储
        com.emotion.api.dto.EmotionAnalysisResponse legacyResponse = new com.emotion.api.dto.EmotionAnalysisResponse();
        legacyResponse.setScore(0);
        legacyResponse.setEmotion(emotionResult.getEmotion_tag());
        legacyResponse.setSuggestion(emotionResult.getAi_comment());

        Long interactionId = userService.saveSubmit(userPrincipal.getUserId(), text, JSONUtil.toJsonStr(result), 0.0, JSONUtil.toJsonStr(legacyResponse));
        analyzeEmotions.put("interactionId", interactionId);
        analyzeEmotions.put("interaction_id", interactionId);

        return BaseResult.success(analyzeEmotions);
    }

    /**
     * 情绪涂鸦分析 - 图片上传到 RustFS 后，用 uploadFileId 调用 Ollama 多模态模型。
     */
    @PostMapping("/api/v1/submitDrawing")
    public BaseResult<Map> submitDrawing(@CurrentUser UserPrincipal userPrincipal, @RequestBody SubmitDrawingDTO drawingDTO) {
        log.info("涂鸦情绪分析请求: userId={}, dto={}", userPrincipal.getUserId(), JSONUtil.toJsonStr(drawingDTO));

        if (drawingDTO == null || drawingDTO.getUploadFileId() == null) {
            return BaseResult.error("400", "缺少涂鸦图片");
        }

        ImageUploadRecord record = imageUploadRecordMapper.selectById(drawingDTO.getUploadFileId());
        if (record == null || !userPrincipal.getUserId().equals(record.getUserId())) {
            return BaseResult.error("403", "图片不存在或无权限");
        }

        com.emotion.api.dto.EmotionResultResponse emotionResult;
        try {
            byte[] imageBytes = rustFsUtil.getFileBytesByUrl(record.getImageUrl());
            emotionResult = ollamaDirectService.analyzeDrawingEmotion(drawingDTO.getDrawingMeta(), imageBytes);
        } catch (Exception e) {
            log.error("涂鸦图片分析失败", e);
            return BaseResult.error("500", "AI服务异常");
        }

        if (emotionResult == null || emotionResult.getEmotion_tag() == null) {
            return BaseResult.error("500", "服务器异常");
        }

        Map analyzeEmotions = new HashMap();
        analyzeEmotions.put("emotion_tag", emotionResult.getEmotion_tag());
        analyzeEmotions.put("ai_comment", emotionResult.getAi_comment());
        analyzeEmotions.put("relief_actions", emotionResult.getRelief_actions());
        Map replies = new HashMap();
        if (emotionResult.getReplies() != null) {
            replies.put("high_eq", emotionResult.getReplies().getHigh_eq());
            replies.put("crazy", emotionResult.getReplies().getCrazy());
            replies.put("gentle", emotionResult.getReplies().getGentle());
            replies.put("sarcastic", emotionResult.getReplies().getSarcastic());
            replies.put("safe", emotionResult.getReplies().getSafe());
            replies.put("firm", emotionResult.getReplies().getFirm());
            replies.put("short", emotionResult.getReplies().getShort_reply());
            replies.put("short_reply", emotionResult.getReplies().getShort_reply());
        }
        analyzeEmotions.put("replies", replies);
        analyzeEmotions.put("answer_book", emotionResult.getAnswer_book());
        analyzeEmotions.put("task_type", emotionResult.getTask_type());
        analyzeEmotions.put("pain_point", emotionResult.getPain_point());
        analyzeEmotions.put("stabilize_action", emotionResult.getStabilize_action());
        analyzeEmotions.put("dont_say", emotionResult.getDont_say());
        analyzeEmotions.put("next_time_tip", emotionResult.getNext_time_tip());

        Map result = new HashMap();
        result.put("data", analyzeEmotions);

        SubmitTextDTO saveDTO = new SubmitTextDTO();
        saveDTO.setText(record.getImageUrl());
        saveDTO.setScene("情绪涂鸦");
        saveDTO.setType(0);
        saveDTO.setUploadFileId(Arrays.asList(drawingDTO.getUploadFileId()));

        com.emotion.api.dto.EmotionAnalysisResponse legacyResponse = new com.emotion.api.dto.EmotionAnalysisResponse();
        legacyResponse.setScore(0);
        legacyResponse.setEmotion(emotionResult.getEmotion_tag());
        legacyResponse.setSuggestion(emotionResult.getAi_comment());

        Long interactionId = userService.saveSubmit(userPrincipal.getUserId(), saveDTO, JSONUtil.toJsonStr(result), 0.0, JSONUtil.toJsonStr(legacyResponse));
        analyzeEmotions.put("interactionId", interactionId);
        analyzeEmotions.put("interaction_id", interactionId);

        return BaseResult.success(analyzeEmotions);
    }

    /**
     * 流式提交文本分析 - 使用SSE实时返回AI分析结果
     * @param userPrincipal 当前用户
     * @param text 提交的文本
     * @return SSE流式响应
     */
    @PostMapping(value = "/api/v1/submitTextStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter submitTextStream(@CurrentUser UserPrincipal userPrincipal, @RequestBody SubmitTextDTO text) {
        log.info("流式分析请求: {} : {}", JSONUtil.toJsonStr(userPrincipal), JSONUtil.toJsonStr(text));
        
        // 保存当前的SecurityContext，以便在异步线程中使用
        org.springframework.security.core.context.SecurityContext securityContext = 
            org.springframework.security.core.context.SecurityContextHolder.getContext();
        
        // 创建SSE emitter，设置超时时间为180秒（3分钟）
        SseEmitter emitter = new SseEmitter(180000L);
        
        // ✅ 添加SSE生命周期回调，防止Security异常
        emitter.onCompletion(() -> {
            log.info("SSE连接已完成");
        });
        
        emitter.onTimeout(() -> {
            log.warn("SSE连接超时");
        });
        
        emitter.onError((throwable) -> {
            log.error("SSE连接错误", throwable);
        });
        
        // 区别普通提交还是签到提交
        if (text.getType() == 1) {
            String date = DateUtil.now();
            // 补签到校验，补签到时间不能超过当前时间
            if (text.getSupplementarySignIn() == 1) {
                if (ObjectUtil.isNull(text.getSupplementarySignInTime())) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data(StreamEmotionResponse.error("补签时间不正确")));
                        emitter.complete();
                    } catch (IOException e) {
                        log.error("发送SSE错误消息失败", e);
                    }
                    return emitter;
                }
                // 获取补签的时间，用hutool的方法判断如果date大于等于今天，返回签到时间不正确
                if (DateUtil.parse(date).isBeforeOrEquals(DateUtil.parse(text.getSupplementarySignInTime()))) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data(StreamEmotionResponse.error("补签时间不正确")));
                        emitter.complete();
                    } catch (IOException e) {
                        log.error("发送SSE错误消息失败", e);
                    }
                    return emitter;
                }
                date = text.getSupplementarySignInTime();
            }
            // 使用data + userId查询text表是否有数据
            LambdaQueryWrapper<UserTextInteraction> queryWrapper = new LambdaQueryWrapper<>();
            // 设置查询条件，这里使用eq方法表示等于userId
            queryWrapper.eq(UserTextInteraction::getUserId, userPrincipal.getUserId());
            // 把date放到一个数组参数里去
            queryWrapper.apply("DATE_FORMAT(sign_in_time, '%Y-%m-%d') = DATE_FORMAT({0}, '%Y-%m-%d')", date);
            List<UserTextInteraction> userTextInteractions = userTextInteractionMapper.selectList(queryWrapper);
            if (CollUtil.isNotEmpty(userTextInteractions)) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(StreamEmotionResponse.error("已签到过")));
                    emitter.complete();
                } catch (IOException e) {
                    log.error("发送SSE错误消息失败", e);
                }
                return emitter;
            }
        }
        
        String inputText = text.getText();
        if (inputText.length() > 2000) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(StreamEmotionResponse.error("输入文本过长")));
                emitter.complete();
            } catch (IOException e) {
                log.error("发送SSE错误消息失败", e);
            }
            return emitter;
        }
        
        // 用于累积完整的JSON响应
        AtomicReference<StringBuilder> fullResponse = new AtomicReference<>(new StringBuilder());
        // ✅ 添加标志位跟踪emitter状态
        AtomicBoolean emitterCompleted = new AtomicBoolean(false);
        
        // 异步处理流式输出
        new Thread(() -> {
            try {
                // 在异步线程中恢复SecurityContext
                org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);
                
                // 发送开始事件
                emitter.send(SseEmitter.event()
                        .name("start")
                        .data(StreamEmotionResponse.chunk("开始分析...")));
                
                // 使用Ollama AI进行流式情绪分析
                log.info("开始调用Ollama流式接口");
                ollamaChatService.analyzeEmotionStream(text.getText())
                        .doOnSubscribe(subscription -> {
                            log.info("Flux已订阅: {}", subscription);
                        })
                        .filter(chunk -> chunk != null && !chunk.trim().isEmpty())  // ✅ 过滤空数据块
                        .doOnNext(chunk -> {
                            // ✅ 如果emitter已完成，不再处理
                            if (emitterCompleted.get()) {
                                log.warn("SSE emitter已完成，跳过数据块处理");
                                return;
                            }
                            
                            try {
                                log.debug("收到数据块: [{}]", chunk);
                                // 累积完整响应
                                fullResponse.get().append(chunk);
                                
                                // 发送每个文本片段到SSE
                                emitter.send(SseEmitter.event()
                                        .name("chunk")
                                        .data(StreamEmotionResponse.chunk(chunk)));
                                log.debug("SSE数据块发送成功");
                            } catch (IllegalStateException e) {
                                // ✅ 捕获emitter已完成的异常
                                log.warn("SSE emitter已完成，停止发送: {}", e.getMessage());
                                emitterCompleted.set(true);
                            } catch (IOException e) {
                                log.error("发送SSE数据块失败", e);
                                emitterCompleted.set(true);
                            }
                        })
                        .doOnError(error -> {
                            log.error("Ollama流式情绪分析失败", error);
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data(StreamEmotionResponse.error("AI服务异常: " + error.getMessage())));
                                emitter.completeWithError(error);
                            } catch (IOException e) {
                                log.error("发送SSE错误消息失败", e);
                            }
                        })
                        .doOnComplete(() -> {
                            log.info("Flux流完成");
                            // ✅ 标记emitter即将完成
                            emitterCompleted.set(true);
                            
                            try {
                                String completeJson = fullResponse.get().toString();
                                log.info("完整JSON: {}", completeJson);
                                
                                // 解析完整的JSON响应
                                EmotionAnalysisResponse emotionResponse = null;
                                try {
                                    // 尝试从AI响应中提取JSON部分（可能包含markdown格式）
                                    String jsonStr = extractJsonFromResponse(completeJson);
                                    log.info("提取后的JSON: {}", jsonStr);
                                    emotionResponse = JSONUtil.toBean(jsonStr, EmotionAnalysisResponse.class);
                                } catch (Exception e) {
                                    log.error("解析AI响应JSON失败: {}", completeJson, e);
                                }
                                
                                if (emotionResponse != null && emotionResponse.getScore() != null) {
                                    // 根据分数确定情绪标签
                                    String emotion = getEmotionLabel(emotionResponse.getScore());
                                    
                                    // 构建最终响应
                                    Map analyzeEmotions = new HashMap();
                                    analyzeEmotions.put("emotion", emotion);
                                    analyzeEmotions.put("emotionRatio", emotionResponse.getScore());
                                    analyzeEmotions.put("reminder", emotionResponse.getSuggestion());
                                    Map result = new HashMap();
                                    result.put("data", analyzeEmotions);
                                    
                                    // 保存到数据库
                                    userService.saveSubmit(
                                        userPrincipal.getUserId(), 
                                        text, 
                                        JSONUtil.toJsonStr(result), 
                                        emotionResponse.getScore().doubleValue(), 
                                        JSONUtil.toJsonStr(emotionResponse)
                                    );
                                    
                                    // ✅ 检查emitter状态后再发送
                                    if (!emitterCompleted.get()) {
                                        // 发送完成事件，包含完整的结果
                                        emitter.send(SseEmitter.event()
                                                .name("complete")
                                                .data(StreamEmotionResponse.complete(JSONUtil.toJsonStr(analyzeEmotions))));
                                    }
                                } else {
                                    log.warn("情感分析结果为空或分数为null");
                                }
                                
                                // ✅ 最后才complete emitter
                                emitter.complete();
                            } catch (IllegalStateException e) {
                                log.warn("SSE emitter已完成，跳过complete: {}", e.getMessage());
                            } catch (IOException e) {
                                log.error("发送SSE完成消息失败", e);
                                try {
                                    emitter.completeWithError(e);
                                } catch (IllegalStateException ex) {
                                    log.warn("emitter已关闭，忽略错误: {}", ex.getMessage());
                                }
                            }
                        })
                        .doOnTerminate(() -> {
                            log.info("Flux流终止（完成或错误）");
                            // ✅ 在Flux终止后清理SecurityContext
                            org.springframework.security.core.context.SecurityContextHolder.clearContext();
                        })
                        .subscribe(
                            chunk -> {
                                // 数据已在 doOnNext 中处理
                            },
                            error -> {
                                log.error("订阅错误", error);
                            },
                            () -> {
                                log.info("订阅完成");
                            }
                        );
                        
                        log.info("Flux订阅已完成，等待数据...");
                        
            } catch (Exception e) {
                log.error("流式分析处理异常", e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(StreamEmotionResponse.error("服务器异常")));
                    emitter.completeWithError(e);
                } catch (IOException ex) {
                    log.error("发送SSE错误消息失败", ex);
                } finally {
                    // ✅ 异常时也清理SecurityContext
                    org.springframework.security.core.context.SecurityContextHolder.clearContext();
                }
            }
        }).start();
        
        return emitter;
    }
    
    /**
     * 从AI响应中提取JSON字符串
     * 处理可能包含markdown代码块的情况
     */
    /**
     * 从AI响应中提取JSON（可能包含Markdown格式）
     */
    private String extractJsonFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            log.warn("AI响应为空");
            return "{}";
        }
        
        String trimmed = response.trim();
        
        // 如果响应包含```json ... ```或``` ... ```，提取其中的内容
        int jsonStart = trimmed.indexOf("```");
        if (jsonStart != -1) {
            int jsonEnd = trimmed.lastIndexOf("```");
            if (jsonEnd > jsonStart) {
                String jsonContent = trimmed.substring(jsonStart + 3, jsonEnd).trim();
                // 移除可能的"json"标记
                if (jsonContent.toLowerCase().startsWith("json")) {
                    jsonContent = jsonContent.substring(4).trim();
                }
                log.info("从Markdown中提取JSON成功");
                return jsonContent;
            }
        }
        
        // 尝试直接查找JSON对象
        int braceStart = trimmed.indexOf('{');
        int braceEnd = trimmed.lastIndexOf('}');
        if (braceStart != -1 && braceEnd > braceStart) {
            String jsonContent = trimmed.substring(braceStart, braceEnd + 1);
            log.info("从文本中提取JSON对象");
            return jsonContent;
        }
        
        // 否则直接返回原始响应
        log.warn("未找到JSON格式，返回原始响应");
        return trimmed;
    }
    
    /**
     * 根据分数获取情绪标签
     */
    private String getEmotionLabel(Integer score) {
        if (score <= 10) {
            return "绝望";
        } else if (score <= 20) {
            return "痛苦";
        } else if (score <= 30) {
            return "愤怒";
        } else if (score <= 40) {
            return "沮丧";
        } else if (score <= 50) {
            return "平静";
        } else if (score <= 60) {
            return "好奇";
        } else if (score <= 70) {
            return "满足";
        } else if (score <= 80) {
            return "开心";
        } else if (score <= 90) {
            return "兴奋";
        } else {
            return "狂喜";
        }
    }

    /**
     * 查询剩余使用次数
     * @param userPrincipal
     * @return
     */
    @GetMapping("/getRemaining")
    public BaseResult<Map> getRemakeLimit(@CurrentUser UserPrincipal userPrincipal) {
        UserFunctionRecord one = userFunctionRecordService.getUserFunctionRecord(userPrincipal.getUserId());
        long daily = one.getDailyLimitTimes() - one.getUsedTimesToday();
        long total = one.getTotalUsageLimit() - one.getUsedUsageCount();
        Map<String, Long> map = new HashMap<>();
        map.put("daily", daily);
        map.put("total", total);
        return BaseResult.success(map);
    }

    @Autowired
    private IRewardRecordService rewardRecordService;
    
    @Autowired
    private RedisUtil redisUtil;
    @GetMapping("/grantReward")
    public BaseResult<String> grantReward(@CurrentUser UserPrincipal userPrincipal, @RequestParam("param") String param) {
        log.info("param: {}", param);
        boolean grantReward = redisUtil.lock("grantReward"+ userPrincipal.getUserId().toString(), userPrincipal.getUserId().toString(), 30000);
        if (!grantReward) {
            return BaseResult.error("500", "操作太频繁，请稍后再试");
        }
        // 获取用户的功能记录
        UserFunctionRecord userFunctionRecord = userFunctionRecordService.getUserFunctionRecord(userPrincipal.getUserId());
        if (userFunctionRecord == null) {
            return BaseResult.error("用户功能记录不存在");
        }
        int count = 3;
        
        // 记录增加前的余额
        int balanceBefore = (int) (userFunctionRecord.getTotalUsageLimit() - userFunctionRecord.getUsedUsageCount());
        
        // 增加使用次数
        userFunctionRecord.setTotalUsageLimit(userFunctionRecord.getTotalUsageLimit() + count);
        userFunctionRecordService.updateById(userFunctionRecord);
        
        // 记录增加后的余额
        int balanceAfter = (int) (userFunctionRecord.getTotalUsageLimit() - userFunctionRecord.getUsedUsageCount());
        
        // 记录次数变动日志
        UserUsageLog usageLog = new UserUsageLog();
        usageLog.setUserId(userPrincipal.getUserId());
        usageLog.setOperationType("AD_REWARD");
        usageLog.setChangeAmount(count);
        usageLog.setBalanceBefore(balanceBefore);
        usageLog.setBalanceAfter(balanceAfter);
        usageLog.setRemark("观看广告奖励");
        usageLog.setCreateTime(java.time.LocalDateTime.now());
        userUsageLogMapper.insert(usageLog);
        
        // 记录奖励数据
        rewardRecordService.saveRewardRecord(userPrincipal.getUserId(), param, count);
        return BaseResult.success(param);
    }

    @GetMapping("/api/v1/historySubmit")
    public BaseResult<PageResult<HistoryEmotion>> getUserHistorySubmit(@CurrentUser UserPrincipal userPrincipal,
                                                                       @RequestParam(defaultValue = "1") int page,
                                                                       @RequestParam(defaultValue = "10") int size,
                                                                       @RequestParam(defaultValue = "0") int type) {
        PageResult<HistoryEmotion> userTextInteractions
                = userService.getUserHistorySubmit(userPrincipal, page, size, type);
        return BaseResult.success(userTextInteractions);
    }

    @GetMapping("/api/v1/historySubmit/detail")
    public BaseResult<HistoryEmotion> getUserHistorySubmitDetail(@CurrentUser UserPrincipal userPrincipal,
                                                                 @RequestParam Long id) {
        HistoryEmotion detail = userService.getUserHistorySubmitDetail(userPrincipal, id);
        if (detail == null) {
            return BaseResult.error("404", "记录不存在");
        }
        return BaseResult.success(detail);
    }

    @GetMapping("/api/v1/signInHistory")
    public BaseResult<List<SignInHistoryDTO>> getUserSignInHistory(@CurrentUser UserPrincipal userPrincipal,
                                                                   @RequestParam String month) {
        List<SignInHistoryDTO> userHistorySubmit = userService.getUserSignInHistory(userPrincipal, month);
        return BaseResult.success(userHistorySubmit);
    }

    @GetMapping("/api/v1/lineChart")
    public BaseResult<List<LineChartData>> getLineChart(@CurrentUser UserPrincipal userPrincipal,
                                                         @RequestParam(defaultValue = "7") int days) {
        List<LineChartData> lineChartData = userService.getLineChart(userPrincipal, days);
        log.info("{} : {} days", JSONUtil.toJsonStr(userPrincipal), days);
        return BaseResult.success(lineChartData);
    }

    @GetMapping("/api/v1/getArticle")
    public BaseResult<WechatArticle> getArticle(@CurrentUser UserPrincipal userPrincipal) {
        WechatArticle wechatArticle = wechatArticleService.getLastArticle();
        log.info("{} : {}", JSONUtil.toJsonStr(userPrincipal), wechatArticle);
        return BaseResult.success(wechatArticle);
    }

    @PostMapping("/api/v1/share")
    public BaseResult<String> handleShare(@CurrentUser UserPrincipal userPrincipal, @RequestBody ShareDTO shareDTO) {
        boolean isNewShare = wechatUserService.handleShare(userPrincipal.getUserOpenId(), shareDTO.getShareUserId());
        log.info("{} : {}", JSONUtil.toJsonStr(userPrincipal), isNewShare);
        if (isNewShare) {
            return BaseResult.success("1");
        }
        return BaseResult.success("0");
    }

    @GetMapping("/api/v1/getNumberOfSlySignIn")
    public BaseResult<Integer> getRemainingNumberOfSupplementarySignIn(@CurrentUser UserPrincipal userPrincipal) {
        // 如果userPrincipal为空，返回错误
        if (ObjectUtil.isNull(userPrincipal) || ObjectUtil.isNull(userPrincipal.getUserId())) {
            return BaseResult.error("400", "用户信息为空");
        }
        Integer remainingNumberOfSupplementarySignIn = userService.getRemakeLimit(userPrincipal);
        log.info("{} : {}", JSONUtil.toJsonStr(userPrincipal), remainingNumberOfSupplementarySignIn);
        return BaseResult.success(remainingNumberOfSupplementarySignIn);
    }

    @GetMapping("/api/v1/getTiAnalysisData")
    public BaseResult<UserTextInteractionsAnalysisData> getUserTextInteractionsAnalysisData(
            @CurrentUser UserPrincipal userPrincipal, @RequestParam(defaultValue = "0") int type) {
        // 如果userPrincipal为空，返回错误
        if (ObjectUtil.isNull(userPrincipal) || ObjectUtil.isNull(userPrincipal.getUserOpenId())) {
            return BaseResult.error("400", "用户信息为空");
        }
        UserTextInteractionsAnalysisData userTextInteractionsAnalysisData
                = userService.getUserTextInteractionsAnalysisData(userPrincipal.getUserId(), type);
        log.info("{} : {}", JSONUtil.toJsonStr(userPrincipal), userTextInteractionsAnalysisData);
        return BaseResult.success(userTextInteractionsAnalysisData);
    }

    @GetMapping("/api/v1/test")
    public String test() {
        return "ok";
    }

    @Autowired
    private DingTalkMsg dingTalkMsg;

    @GetMapping("/getProcessId")
    public String getProcessId() throws IOException {
        String pid = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        // 执行 jmap -histo 命令
        Process process = Runtime.getRuntime().exec("jmap -histo " + pid);

        // 读取命令输出
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        StringBuilder output = new StringBuilder();
        int count = 0;
        while ((line = reader.readLine()) != null) {
            if (count <= 10) {
                output.append(line).append("\n");
            }
            count++;
        }
        dingTalkMsg.sendMsgToDingTalk(output.toString());
        return pid;
    }

    @PostMapping("/api/v1/covertImage")
    public BaseResult<String> covertImage(@CurrentUser UserPrincipal userPrincipal, @RequestBody SubmitTextDTO text) {
        // 提交文本不能为空
        if (ObjectUtil.isNull(text) || ObjectUtil.isNull(text.getText())) {
            return BaseResult.error("400", "用户输入参数不能为空");
        }
        return userService.covertImage(userPrincipal.getUserId(), text);
    }

    /**
     * 获取用户资料
     * @param userPrincipal 当前用户
     * @return 用户资料（昵称、星座）
     */
    @GetMapping("/api/v1/profile")
    public BaseResult<Map<String, String>> getUserProfile(@CurrentUser UserPrincipal userPrincipal) {
        WechatUser wechatUser = wechatUserService.getById(userPrincipal.getUserId());
        Map<String, String> profile = new HashMap<>();
        profile.put("nickname", wechatUser.getNickname() != null ? wechatUser.getNickname() : "");
        profile.put("constellation", wechatUser.getConstellation() != null ? wechatUser.getConstellation() : "");
        return BaseResult.success(profile);
    }

    /**
     * 更新用户资料
     * @param userPrincipal 当前用户
     * @param updateDTO 更新数据
     * @return 更新结果
     */
    @PostMapping("/api/v1/profile/update")
    public BaseResult<String> updateUserProfile(
        @CurrentUser UserPrincipal userPrincipal, 
        @RequestBody UserProfileUpdateDTO updateDTO) {
        
        WechatUser wechatUser = wechatUserService.getById(userPrincipal.getUserId());
        
        // 只更新非空字段
        if (updateDTO.getNickname() != null) {
            // 验证昵称长度
            if (updateDTO.getNickname().length() > 50) {
                return BaseResult.error("昵称不能超过50个字符");
            }
            wechatUser.setNickname(updateDTO.getNickname());
        }
        
        if (updateDTO.getConstellation() != null) {
            // 验证星座有效性
            String[] validConstellations = {"白羊座", "金牛座", "双子座", "巨蟹座", "狮子座", 
                                            "处女座", "天秤座", "天蝎座", "射手座", "摩羯座", 
                                            "水瓶座", "双鱼座"};
            boolean isValid = java.util.Arrays.asList(validConstellations).contains(updateDTO.getConstellation());
            if (!isValid) {
                return BaseResult.error("无效的星座");
            }
            wechatUser.setConstellation(updateDTO.getConstellation());
        }
        
        wechatUserService.updateById(wechatUser);
        return BaseResult.success("更新成功");
    }

    /**
     * 获取微信小程序码（用于心情卡片分享海报）
     * @param userPrincipal 当前用户
     * @return Base64 编码的小程序码
     */
    @GetMapping("/api/v1/wxacode")
    public BaseResult<Map<String, String>> getWxacode(@CurrentUser UserPrincipal userPrincipal) {
        try {
            String scene = "uid_" + userPrincipal.getUserId();
            String base64 = wxacodeService.getWxacodeBase64(scene, "pages/index/index");
            Map<String, String> result = new HashMap<>();
            result.put("wxacode", base64);
            return BaseResult.success(result);
        } catch (Exception e) {
            log.error("获取小程序码失败", e);
            return BaseResult.error("获取小程序码失败: " + e.getMessage());
        }
    }
}
