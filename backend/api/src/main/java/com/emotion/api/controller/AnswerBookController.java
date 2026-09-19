package com.emotion.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.emotion.api.config.user.CurrentUser;
import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.dto.AnswerBookRequest;
import com.emotion.api.dto.AnswerBookResponse;
import com.emotion.api.repository.po.AnswerBookRecord;
import com.emotion.api.service.AnswerBookService;
import com.emotion.api.config.BaseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 答案之书控制器
 *
 * @author system
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequestMapping("/answer-book/api/v1")
public class AnswerBookController {

    @Autowired
    private AnswerBookService answerBookService;

    /**
     * 提问获取答案
     *
     * @param userPrincipal 当前用户
     * @param request 请求参数
     * @return 答案响应
     */
    @PostMapping("/ask")
    public BaseResult<AnswerBookResponse> askQuestion(
            @CurrentUser UserPrincipal userPrincipal,
            @RequestBody AnswerBookRequest request) {
        try {
            log.info("用户 {} 提问: {}", userPrincipal.getUserId(), request.getQuestion());
            AnswerBookResponse response = answerBookService.askQuestion(userPrincipal.getUserId(), request);
            return BaseResult.success(response);
        } catch (RuntimeException e) {
            log.error("答案之书提问失败", e);
            return BaseResult.error("500", e.getMessage());
        } catch (Exception e) {
            log.error("答案之书提问异常", e);
            return BaseResult.error("500", "服务器异常");
        }
    }

    /**
     * 第一步：快速获取预设答案（不扣次数，不调用AI）
     *
     * @param userPrincipal 当前用户
     * @param request 请求参数
     * @return 预设答案
     */
    @PostMapping("/get-random-answer")
    public BaseResult<AnswerBookResponse> getRandomAnswer(
            @CurrentUser UserPrincipal userPrincipal,
            @RequestBody AnswerBookRequest request) {
        try {
            log.info("用户 {} 获取预设答案: {}", userPrincipal.getUserId(), request.getQuestion());
            AnswerBookResponse response = answerBookService.getRandomAnswerOnly(userPrincipal.getUserId(), request);
            return BaseResult.success(response);
        } catch (RuntimeException e) {
            log.error("获取预设答案失败", e);
            return BaseResult.error("500", e.getMessage());
        } catch (Exception e) {
            log.error("获取预设答案异常", e);
            return BaseResult.error("500", "服务器异常");
        }
    }

    /**
     * 第二步：异步获取AI解读（基于已有的预设答案）
     *
     * @param userPrincipal 当前用户
     * @param request 请求参数（包含问题、预设答案和记录ID）
     * @return AI解读结果
     */
    @PostMapping("/get-ai-explanation")
    public BaseResult<String> getAiExplanation(
            @CurrentUser UserPrincipal userPrincipal,
            @RequestBody AnswerBookRequest request) {
        try {
            log.info("用户 {} 获取AI解读 - 问题: {}, 预设答案: {}, 记录ID: {}", 
                userPrincipal.getUserId(), 
                request.getQuestion(),
                request.getRandomAnswer(),
                request.getId());
            
            String explanation = answerBookService.generateAiExplanationOnly(
                request.getQuestion(), 
                request.getRandomAnswer(),
                request.getId()
            );
            
            log.info("返回AI解读结果: {}", explanation != null ? explanation.substring(0, Math.min(50, explanation.length())) : "null");
            
            return BaseResult.success(explanation);
        } catch (RuntimeException e) {
            log.error("获取AI解读失败", e);
            return BaseResult.error("500", e.getMessage());
        } catch (Exception e) {
            log.error("获取AI解读异常", e);
            return BaseResult.error("500", "服务器异常");
        }
    }

    /**
     * 获取历史记录
     *
     * @param userPrincipal 当前用户
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    @GetMapping("/history")
    public BaseResult<Page<AnswerBookRecord>> getHistory(
            @CurrentUser UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        try {
            Page<AnswerBookRecord> result = answerBookService.getHistory(userPrincipal.getUserId(), page, size);
            return BaseResult.success(result);
        } catch (Exception e) {
            log.error("获取答案之书历史失败", e);
            return BaseResult.error("500", "服务器异常");
        }
    }

    /**
     * 删除记录
     *
     * @param userPrincipal 当前用户
     * @param id 记录ID
     * @return 结果
     */
    @DeleteMapping("/record/{id}")
    public BaseResult<Void> deleteRecord(
            @CurrentUser UserPrincipal userPrincipal,
            @PathVariable Long id) {
        try {
            answerBookService.deleteRecord(userPrincipal.getUserId(), id);
            return BaseResult.success(null);
        } catch (RuntimeException e) {
            log.error("删除答案之书记录失败", e);
            return BaseResult.error("500", e.getMessage());
        } catch (Exception e) {
            log.error("删除答案之书记录异常", e);
            return BaseResult.error("500", "服务器异常");
        }
    }
}
