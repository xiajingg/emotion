//package com.emotion.api.controller;
//
//import cn.hutool.core.collection.CollUtil;
//import cn.hutool.core.date.DateUtil;
//import cn.hutool.core.util.ObjectUtil;
//import cn.hutool.core.util.StrUtil;
//import cn.hutool.json.JSONUtil;
//import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
//import com.emotion.api.config.BaseResult;
//import com.emotion.api.config.user.CurrentUser;
//import com.emotion.api.config.user.UserPrincipal;
//import com.emotion.api.dto.*;
//import com.emotion.api.repository.dao.rds.UserRelationshipMapper;
//import com.emotion.api.repository.dao.rds.UserTextInteractionTogetherMapper;
//import com.emotion.api.repository.po.UserRelationship;
//import com.emotion.api.repository.po.UserTextInteractionTogether;
//import com.emotion.api.service.IUserRelationshipService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.core.env.Environment;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//
//@Slf4j
//@RestController
//@RequestMapping("/together")
//public class TogetherController {
//    @Autowired
//    private IUserRelationshipService userRelationshipService;
//
//    @Autowired
//    private UserTextInteractionTogetherMapper userTextInteractionTogetherMapper;
//
//    @Autowired
//    private UserRelationshipMapper userRelationshipMapper;
//
//    @Autowired
//    private RestTemplate restTemplate;
//
//    /**
//     * 获取用户的绑定关系
//     *
//     * @param userPrincipal
//     * @return
//     */
//    @GetMapping("/api/v1/getRelationship")
//    public BaseResult<RelationDTO> getRelationship(@CurrentUser UserPrincipal userPrincipal) {
//        RelationDTO relationDTO = userRelationshipService.getUserRelationship(userPrincipal.getUserId());
//        return BaseResult.success(relationDTO);
//    }
//
//    @PostMapping("/api/v1/createRelationship")
//    public BaseResult<UserRelationshipDTO> createRelationship(@RequestBody CreateRelationship createRelationship,
//                                                              @CurrentUser UserPrincipal userPrincipal) {
//        if (Objects.isNull(createRelationship) || StrUtil.isEmpty(createRelationship.getRelation())) {
//            return BaseResult.error("400", "参数错误");
//        }
//        // TODO 限制绑定关系上限
//        UserRelationshipDTO userRelationshipDTO
//                = userRelationshipService.createRelationship(createRelationship.getRelation(), userPrincipal);
//        log.info("{} : {}", JSONUtil.toJsonStr(userPrincipal), JSONUtil.toJsonStr(userRelationshipDTO));
//        return BaseResult.success(userRelationshipDTO);
//    }
//
//    /**
//     *
//     * @param deleteRelationship
//     * @param userPrincipal
//     * @return
//     */
//    @PostMapping("/api/v1/deleteRelationship")
//    public BaseResult<Boolean> deleteRelationship(@RequestBody CreateRelationship deleteRelationship,
//                                                              @CurrentUser UserPrincipal userPrincipal) {
//        return BaseResult.success(true);
//    }
//
//
//    /**
//     * 绑定关系
//     *
//     * @param request
//     * @param userPrincipal
//     * @return
//     */
//    @PostMapping("/api/v1/bindRelationship")
//    public BaseResult<Integer> bindRelationship(@RequestBody BindUserRequest request, @CurrentUser UserPrincipal userPrincipal) {
//        if (StrUtil.isEmpty(request.getRelation()) || StrUtil.isEmpty(request.getUniqueCode())) {
//            return BaseResult.error("400", "参数错误");
//        }
//        LambdaQueryWrapper<UserRelationship> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(UserRelationship::getUniqueCode, request.getUniqueCode());
//        // 通过code去查询绑定的用户
//        List<UserRelationship> userRelationships = userRelationshipMapper.selectList(queryWrapper);
//        if (CollUtil.isEmpty(userRelationships) || userRelationships.get(0) == null) {
//            return BaseResult.error("401", "该用户不存在");
//        }
//        if (userRelationships.size() >= 5) {
//            return BaseResult.error("400", "绑定关系已满5人");
//        }
//        // 判断是否有绑定过关系，不能重复绑定
//        for (UserRelationship userRelationship : userRelationships) {
//            if (Objects.equals(userRelationship.getUserId(), userPrincipal.getUserId())) {
//                return BaseResult.error("500", "不能绑定自己");
//            }
//        }
//        // 如果通过以上条件则可以绑定关系
//        UserRelationship userRelationship = userRelationships.get(0);
//        int bindRelationship = userRelationshipService
//                .bindRelationship(request, userPrincipal.getUserId(), userRelationship);
//        log.info("{} : {}", JSONUtil.toJsonStr(userPrincipal), bindRelationship);
//        return BaseResult.success(bindRelationship);
//    }
//
//    /**
//     * 提交文字、签到补签到
//     *
//     * @param userPrincipal
//     * @param text
//     * @return
//     */
//    @PostMapping("/api/v1/submitText")
//    public BaseResult<Object> submitText(@CurrentUser UserPrincipal userPrincipal, @RequestBody SubmitTextDTO text) {
//        String date = DateUtil.now();
//        if (text.getSupplementarySignIn() == 1) {
//            if (ObjectUtil.isNull(text.getSupplementarySignInTime())) {
//                return BaseResult.error("400", "补签时间不正确");
//            }
//            // 获取补签的时间，用hutool的方法判断如果date大于等于今天，返回签到时间不正确
//            if (DateUtil.parse(date).isBeforeOrEquals(DateUtil.parse(text.getSupplementarySignInTime()))) {
//                return BaseResult.error("400", "补签时间不正确");
//            }
//            date = text.getSupplementarySignInTime();
//        }
//        // 使用data + userId查询text表是否有数据
//        LambdaQueryWrapper<UserTextInteractionTogether> queryWrapper = new LambdaQueryWrapper<>();
//        // 设置查询条件，这里使用eq方法表示等于userId
//        queryWrapper.eq(UserTextInteractionTogether::getId, userPrincipal.getUserId());
//        // 把date放到一个数组参数里去
//        queryWrapper.apply("DATE_FORMAT(sign_in_time, '%Y-%m-%d') = DATE_FORMAT({0}, '%Y-%m-%d')", date);
//        List<UserTextInteractionTogether> userTextInteractionTogethers = userTextInteractionTogetherMapper.selectList(queryWrapper);
//        if (CollUtil.isNotEmpty(userTextInteractionTogethers)) {
//            return BaseResult.error("400", "已签到过");
//        }
//
//        String inputText = text.getText();
//        if (inputText.length() > 100) {
//            return BaseResult.error("500", "输入文本过长");
//        }
//        String activeProfile = getActiveProfile();
//        String result = null;
//        if (activeProfile.equals("prod")) {
//            // 使用RestTemplate调用post形式调用 http://127.0.0.1:8080/submitText
//            result = restTemplate
//                    .getForObject("http://127.0.0.1:8000/submitText?text=" + text.getText(), String.class);
//        } else {
//            result = "{\"data\":{\"emotion\":\"消极的情绪\",\"emotionRatio\":\"97.94%\",\"reminder\":\"内容展示了一个情绪极度消极的人说出的一句话。这个消极的情绪可以被形容为沉重或者悲伤的感觉。建议结合其他内容给出更加全面的情绪判断分析再得到具体答案，可精准为传达情感和传递准确的情感分析结果，表述个人综合体会和建议可以表达出此语境的主观感受。客观感觉可以表达为：“听起来情绪很低落。”\"}}";
//        }
//        result = userRelationshipService.saveSubmit(userPrincipal, text, result);
//        log.info("{} : {}", JSONUtil.toJsonStr(userPrincipal), result);
//        Map bean = JSONUtil.toBean(result, Map.class);
//        return BaseResult.success(bean.get("data"));
//    }
//
//    @Autowired
//    private Environment environment;
//
//    private String getActiveProfile() {
//        String[] activeProfiles = environment.getActiveProfiles();
//        if (activeProfiles[0].equals("prod")) {
//            return "prod";
//        }
//        return "dev";
//    }
//
//    /**
//     * 获取历史记录
//     *
//     * @param userPrincipal
//     * @param page
//     * @param size
//     * @param code
//     * @return
//     */
//    @GetMapping("/api/v1/getHistory")
//    public BaseResult<PageResult<TogetherHistory>> getTogetherHistory(@CurrentUser UserPrincipal userPrincipal,
//                                                                      @RequestParam(defaultValue = "1") int page,
//                                                                      @RequestParam(defaultValue = "10") int size,
//                                                                      @RequestParam String code) {
//        // 如果根据userId和code查不到关系表示userId和code没有关联，返回错误
//        List<UserRelationship> userRelationships = getUserRelationship(userPrincipal, code);
//        if (CollUtil.isEmpty(userRelationships)) {
//            return BaseResult.error("400", "用户关系未绑定");
//        }
//        PageResult<TogetherHistory> userHistorySubmits = userRelationshipService.getTogetherHistory(code, page, size);
//        log.info("{} : {}", JSONUtil.toJsonStr(userPrincipal), JSONUtil.toJsonStr(userHistorySubmits));
//        return BaseResult.success(userHistorySubmits);
//    }
//
//    @GetMapping("/api/v1/signInHistory")
//    public BaseResult<List<SignInHistoryDTO>> getUserSignInHistory(@CurrentUser UserPrincipal userPrincipal,
//                                                                   @RequestParam String month, @RequestParam String code) {
//        // 如果根据userId和code查不到关系表示userId和code没有关联，返回错误
//        List<UserRelationship> userRelationships = getUserRelationship(userPrincipal, code);
//        if (CollUtil.isEmpty(userRelationships)) {
//            return BaseResult.error("400", "用户关系未绑定");
//        }
//        List<SignInHistoryDTO> userHistorySubmit = userRelationshipService.getTogetherSignInHistory(userPrincipal, month, code);
//        log.info("{} : {}", JSONUtil.toJsonStr(userPrincipal), JSONUtil.toJsonStr(userHistorySubmit));
//        return BaseResult.success(userHistorySubmit);
//    }
//
//    private List<UserRelationship> getUserRelationship(UserPrincipal userPrincipal, String code) {
//        LambdaQueryWrapper<UserRelationship> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(UserRelationship::getUserId, userPrincipal.getUserId());
//        queryWrapper.eq(UserRelationship::getUniqueCode, code);
//        return userRelationshipMapper.selectList(queryWrapper);
//    }
//}
