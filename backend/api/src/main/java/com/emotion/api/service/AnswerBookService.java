package com.emotion.api.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.emotion.api.dto.AnswerBookRequest;
import com.emotion.api.dto.AnswerBookResponse;
import com.emotion.api.repository.mapper.AnswerBookAnswerMapper;
import com.emotion.api.repository.mapper.AnswerBookRecordMapper;
import com.emotion.api.repository.po.AnswerBookAnswer;
import com.emotion.api.repository.po.AnswerBookRecord;
import com.emotion.api.repository.po.UserFunctionRecord;
import com.emotion.api.repository.po.UserUsageLog;
import com.emotion.api.repository.mapper.UserUsageLogMapper;
import com.emotion.api.service.OllamaDirectService;
import com.emotion.api.service.IUserFunctionRecordService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 答案之书服务类
 *
 * @author system
 * @since 2026-05-05
 */
@Slf4j
@Service
public class AnswerBookService {

    // HTTP客户端（与OllamaDirectService保持一致）
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)  // ✅ 增加到120秒，适应大模型推理时间
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    @Autowired
    private AnswerBookAnswerMapper answerBookAnswerMapper;

    @Autowired
    private AnswerBookRecordMapper answerBookRecordMapper;

    @Autowired
    private IUserFunctionRecordService userFunctionRecordService;

    @Autowired
    private UserUsageLogMapper userUsageLogMapper;

    @Autowired
    private OllamaDirectService ollamaDirectService;

    /**
     * 提问获取答案
     *
     * @param userId 用户ID
     * @param request 请求参数
     * @return 答案响应
     */
    @Transactional(rollbackFor = Exception.class)
    public AnswerBookResponse askQuestion(Long userId, AnswerBookRequest request) {
        // 1. 参数校验
        String question = request.getQuestion();
        if (StrUtil.isBlank(question)) {
            throw new RuntimeException("问题不能为空");
        }
        if (question.length() < 5 || question.length() > 200) {
            throw new RuntimeException("问题长度应在5-200字之间");
        }

        // ===== 已禁用：次数限制（推广期无限使用，2026-05-21）=====
        // 2. 检查用户剩余次数
        // UserFunctionRecord record = userFunctionRecordService.getUserFunctionRecord(userId);
        // long daily = record.getDailyLimitTimes() - record.getUsedTimesToday();
        // long total = record.getTotalUsageLimit() - record.getUsedUsageCount();
        // if (daily <= 0 && total <= 0) {
        //     throw new RuntimeException("使用次数不足，请获取更多次数");
        // }

        // 3. 随机抽取预设答案
        AnswerBookAnswer randomAnswer = answerBookAnswerMapper.getRandomAnswer();
        if (randomAnswer == null) {
            throw new RuntimeException("答案之书暂时无法回答，请稍后再试");
        }

        // 4. 调用AI生成个性化解读
        String aiExplanation = generateAiExplanation(question, randomAnswer.getContent());

        // ===== 已禁用：次数扣减和日志记录（2026-05-21）=====
        // 5. 扣减使用次数并记录日志
        // int balanceBefore = (int) (daily > 0 ? 
        //     (record.getDailyLimitTimes() - record.getUsedTimesToday()) : 
        //     (record.getTotalUsageLimit() - record.getUsedUsageCount()));
        // 
        // if (daily > 0) {
        //     record.setUsedTimesToday(record.getUsedTimesToday() + 1);
        // } else {
        //     record.setUsedUsageCount(record.getUsedUsageCount() + 1);
        // }
        // userFunctionRecordService.updateById(record);
        // 
        // int balanceAfter = (int) (daily > 0 ? 
        //     (record.getDailyLimitTimes() - record.getUsedTimesToday()) : 
        //     (record.getTotalUsageLimit() - record.getUsedUsageCount()));

        // 5. 保存记录
        AnswerBookRecord answerBookRecord = new AnswerBookRecord();
        answerBookRecord.setUserId(userId);
        answerBookRecord.setQuestion(question);
        answerBookRecord.setRandomAnswer(randomAnswer.getContent());
        answerBookRecord.setAiExplanation(aiExplanation);
        answerBookRecord.setUsageCount(1);
        answerBookRecord.setCreateTime(LocalDateTime.now());
        answerBookRecordMapper.insert(answerBookRecord);
        
        // ===== 已禁用：次数变动日志（2026-05-21）=====
        // 记录次数变动日志
        // UserUsageLog usageLog = new UserUsageLog();
        // usageLog.setUserId(userId);
        // usageLog.setOperationType("ANSWER_BOOK");
        // usageLog.setChangeAmount(-1);
        // usageLog.setBalanceBefore(balanceBefore);
        // usageLog.setBalanceAfter(balanceAfter);
        // usageLog.setRelatedId(answerBookRecord.getId());
        // usageLog.setRemark("答案之书提问：" + question.substring(0, Math.min(50, question.length())));
        // usageLog.setCreateTime(LocalDateTime.now());
        // userUsageLogMapper.insert(usageLog);

        // 6. 返回结果
        AnswerBookResponse response = new AnswerBookResponse();
        response.setId(answerBookRecord.getId());
        response.setQuestion(question);
        response.setRandomAnswer(randomAnswer.getContent());
        response.setAiExplanation(aiExplanation);
        response.setUsageCount(1);
        response.setCreateTime(answerBookRecord.getCreateTime());

        return response;
    }

    /**
     * 第一步：仅获取预设答案（扣次数，保存记录，不调用AI）
     *
     * @param userId 用户ID
     * @param request 请求参数
     * @return 答案响应（只包含预设答案）
     */
    @Transactional(rollbackFor = Exception.class)
    public AnswerBookResponse getRandomAnswerOnly(Long userId, AnswerBookRequest request) {
        // 1. 参数校验
        String question = request.getQuestion();
        if (StrUtil.isBlank(question)) {
            throw new RuntimeException("问题不能为空");
        }
        if (question.length() < 5 || question.length() > 200) {
            throw new RuntimeException("问题长度应在5-200字之间");
        }

        // ===== 已禁用：次数限制（推广期无限使用，2026-05-21）=====
        // 2. 检查用户剩余次数
        // UserFunctionRecord record = userFunctionRecordService.getUserFunctionRecord(userId);
        // long daily = record.getDailyLimitTimes() - record.getUsedTimesToday();
        // long total = record.getTotalUsageLimit() - record.getUsedUsageCount();
        // if (daily <= 0 && total <= 0) {
        //     throw new RuntimeException("使用次数不足，请获取更多次数");
        // }

        // 2. 随机抽取预设答案
        AnswerBookAnswer randomAnswer = answerBookAnswerMapper.getRandomAnswer();
        if (randomAnswer == null) {
            throw new RuntimeException("答案之书暂时无法回答，请稍后再试");
        }

        // ===== 已禁用：次数扣减（2026-05-21）=====
        // 3. 扣减使用次数
        // int balanceBefore = (int) (daily > 0 ? 
        //     (record.getDailyLimitTimes() - record.getUsedTimesToday()) : 
        //     (record.getTotalUsageLimit() - record.getUsedUsageCount()));
        // 
        // if (daily > 0) {
        //     record.setUsedTimesToday(record.getUsedTimesToday() + 1);
        // } else {
        //     record.setUsedUsageCount(record.getUsedUsageCount() + 1);
        // }
        // userFunctionRecordService.updateById(record);
        // 
        // int balanceAfter = (int) (daily > 0 ? 
        //     (record.getDailyLimitTimes() - record.getUsedTimesToday()) : 
        //     (record.getTotalUsageLimit() - record.getUsedUsageCount()));

        // 3. 保存记录（AI解读为空，等待第二步更新）
        AnswerBookRecord answerBookRecord = new AnswerBookRecord();
        answerBookRecord.setUserId(userId);
        answerBookRecord.setQuestion(question);
        answerBookRecord.setRandomAnswer(randomAnswer.getContent());
        answerBookRecord.setAiExplanation("");  // 空字符串，等待第二步更新
        answerBookRecord.setUsageCount(1);
        answerBookRecord.setCreateTime(LocalDateTime.now());
        answerBookRecordMapper.insert(answerBookRecord);
        
        // ===== 已禁用：次数变动日志（2026-05-21）=====
        // 4. 记录次数变动日志
        // UserUsageLog usageLog = new UserUsageLog();
        // usageLog.setUserId(userId);
        // usageLog.setOperationType("ANSWER_BOOK");
        // usageLog.setChangeAmount(-1);
        // usageLog.setBalanceBefore(balanceBefore);
        // usageLog.setBalanceAfter(balanceAfter);
        // usageLog.setRelatedId(answerBookRecord.getId());
        // usageLog.setRemark("答案之书提问：" + question.substring(0, Math.min(50, question.length())));
        // usageLog.setCreateTime(LocalDateTime.now());
        // userUsageLogMapper.insert(usageLog);

        // 4. 返回结果（不包含AI解读）
        AnswerBookResponse response = new AnswerBookResponse();
        response.setId(answerBookRecord.getId());
        response.setQuestion(question);
        response.setRandomAnswer(randomAnswer.getContent());
        response.setAiExplanation("");  // 空字符串，等待第二步填充

        return response;
    }

    /**
     * 第二步：仅生成AI解读并更新记录（不扣次数）
     *
     * @param question 用户问题
     * @param randomAnswer 预设答案
     * @param recordId 记录ID
     * @return AI解读
     */
    public String generateAiExplanationOnly(String question, String randomAnswer, Long recordId) {
        log.info("开始生成AI解读 - 问题: {}, 预设答案: {}, 记录ID: {}", question, randomAnswer, recordId);
        
        // 1. 生成AI解读
        String aiExplanation = generateAiExplanation(question, randomAnswer);
        
        log.info("AI解读生成结果: {}", aiExplanation != null ? aiExplanation.substring(0, Math.min(50, aiExplanation.length())) : "null");
        
        // 2. 更新记录的AI字段
        if (recordId != null && recordId > 0) {
            try {
                AnswerBookRecord record = answerBookRecordMapper.selectById(recordId);
                if (record != null) {
                    record.setAiExplanation(aiExplanation);
                    answerBookRecordMapper.updateById(record);
                    log.info("成功更新记录 {} 的AI解读", recordId);
                } else {
                    log.warn("未找到记录 ID: {}", recordId);
                }
            } catch (Exception e) {
                log.error("更新AI解读失败", e);
                // 更新失败不影响返回结果
            }
        }
        
        return aiExplanation;
    }

    /**
     * 生成AI个性化解读
     *
     * @param question 用户问题
     * @param randomAnswer 随机答案
     * @return AI解读
     */
    private String generateAiExplanation(String question, String randomAnswer) {
        try {
            log.info("调用Ollama生成AI解读");
            
            // 构建Prompt（参考OllamaDirectService的实现方式）
            String prompt = String.format(
                "你是一个温暖的心灵导师。用户提出了一个问题，我从古老的答案之书中为他随机翻开了一页，得到了一句智慧的启示。\n\n" +
                "请你结合用户的具体问题和这句启示，给出一个简短而温暖的解读，帮助用户理解这个答案对他的意义。\n\n" +
                "要求：\n" +
                "- 用第二人称你来对话\n" +
                "- 语气温暖、治愈、有力量\n" +
                "- 控制在50-100字以内\n" +
                "- 不要解释答案之书是什么，直接给出解读\n\n" +
                "用户问题：%s\n" +
                "答案之书的启示：%s\n\n" +
                "请给出你的解读：",
                question, randomAnswer
            );
    
            // 直接调用Ollama API（与submitText保持一致）
            String result = callOllamaApi(prompt);
            
            log.info("Ollama返回结果长度: {}", result != null ? result.length() : 0);
            log.info("Ollama返回结果: {}", result != null ? result.substring(0, Math.min(50, result.length())) : "null");
            
            // 检查结果是否为空
            if (result == null || result.trim().isEmpty()) {
                log.warn("Ollama返回空结果，使用默认解读");
                return "这句话在告诉你，相信自己的内心，答案就在其中。无论前方如何，都要保持信心和勇气。";
            }
            
            return result;
        } catch (Exception e) {
            log.error("AI生成解读失败 - 错误类型: {}, 错误消息: {}", e.getClass().getName(), e.getMessage(), e);
            // 如果AI失败，返回默认解读
            return "这句话在告诉你，相信自己的内心，答案就在其中。无论前方如何，都要保持信心和勇气。";
        }
    }

    /**
     * 直接调用Ollama API（参考OllamaDirectService实现）
     *
     * @param prompt 提示词
     * @return AI回复内容
     */
    private String callOllamaApi(String prompt) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "qwen3.5:9b");
            requestBody.put("stream", false);
            requestBody.put("think", false);  // 🔑 关闭思考模式，避免返回空字符串
            
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            requestBody.put("messages", new Object[]{message});

            // 发送HTTP请求
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                    cn.hutool.json.JSONUtil.toJsonStr(requestBody),
                    okhttp3.MediaType.parse("application/json")
            );

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url("http://localhost:11434/api/chat")
                    .post(body)
                    .build();

            log.info("发送请求到Ollama API...");
            
            try (okhttp3.Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("HTTP请求失败: " + response.code());
                }

                String responseBody = response.body().string();
                log.info("Ollama原始响应长度: {}", responseBody != null ? responseBody.length() : 0);

                // 解析响应
                Map<String, Object> responseMap = cn.hutool.json.JSONUtil.toBean(responseBody, Map.class);
                Map<String, Object> messageObj = (Map<String, Object>) responseMap.get("message");
                
                if (messageObj == null || !messageObj.containsKey("content")) {
                    throw new RuntimeException("Ollama返回的响应中没有message.content字段");
                }

                String content = (String) messageObj.get("content");
                log.info("AI回复内容长度: {}", content != null ? content.length() : 0);

                if (content == null || content.trim().isEmpty()) {
                    throw new RuntimeException("AI返回空响应");
                }

                return content;
            }
        } catch (Exception e) {
            log.error("调用Ollama API失败", e);
            throw new RuntimeException("调用Ollama API失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取历史记录
     *
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    public Page<AnswerBookRecord> getHistory(Long userId, Integer page, Integer size) {
        Page<AnswerBookRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AnswerBookRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnswerBookRecord::getUserId, userId)
               .orderByDesc(AnswerBookRecord::getCreateTime);
        return answerBookRecordMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 删除记录
     *
     * @param userId 用户ID
     * @param recordId 记录ID
     */
    public void deleteRecord(Long userId, Long recordId) {
        LambdaQueryWrapper<AnswerBookRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnswerBookRecord::getId, recordId)
               .eq(AnswerBookRecord::getUserId, userId);
        AnswerBookRecord record = answerBookRecordMapper.selectOne(wrapper);
        if (record == null) {
            throw new RuntimeException("记录不存在或无权删除");
        }
        answerBookRecordMapper.deleteById(recordId);
    }
}
