package com.emotion.api.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.dto.*;
import com.emotion.api.repository.dao.rds.UserFunctionRecordMapper;
import com.emotion.api.repository.dao.rds.UserRelationshipMapper;
import com.emotion.api.repository.dao.rds.UserTextInteractionTogetherMapper;
import com.emotion.api.repository.po.UserRelationship;
import com.emotion.api.repository.po.UserTextInteractionTogether;
import com.emotion.api.service.IUserRelationshipService;
import com.emotion.api.util.ShortCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
@Service
public class UserRelationshipServiceImpl extends ServiceImpl<UserRelationshipMapper, UserRelationship> implements IUserRelationshipService {

    @Autowired
    private UserFunctionRecordMapper userFunctionRecordMapper;
    @Autowired
    private UserTextInteractionTogetherMapper userTextInteractionTogetherMapper;
    @Autowired
    private UserRelationshipMapper userRelationshipMapper;

    /**
     * 根据用户id获取用户关系
     *
     * @param userId userId
     * @return
     */
    @Override
    public RelationDTO getUserRelationship(Long userId) {
        LambdaQueryWrapper<UserRelationship> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserRelationship::getUserId, userId);
        // 根据userid查询他的所有关系
        List<UserRelationship> userRelationships = baseMapper.selectList(queryWrapper);
        RelationDTO relationDTO = new RelationDTO();
        List<UserRelationshipDTO> userRelationshipDTOS = new ArrayList<>();
        // 如果没有关系就创建一个，并生成code让他人绑定，返回
        if (CollUtil.isEmpty(userRelationships)) {
            UserRelationship userRelationship = new UserRelationship();
            userRelationship.setUserId(userId);
            String code = generateUniqueCode();
            userRelationship.setUniqueCode(code);
            userRelationship.setCreateTime(new Date());
            baseMapper.insert(userRelationship);

            UserRelationshipDTO userRelationshipDTO = new UserRelationshipDTO();
            List<Long> userIds = new ArrayList<>();
            userIds.add(userId);
            userRelationshipDTO.setUserIds(userIds);
            userRelationshipDTO.setUniqueCode(code);
            userRelationshipDTOS.add(userRelationshipDTO);
            relationDTO.setUserRelationships(userRelationshipDTOS);
            return relationDTO;
        }
        int bindStatus = 0;
        // 如果有关系用code去找出所有绑定的用户，然后返回
        for (UserRelationship userRelationship : userRelationships) {
            // 创建返回关系对象
            UserRelationshipDTO userRelationshipDTO = new UserRelationshipDTO();
            String uniqueCode = userRelationship.getUniqueCode();
            userRelationshipDTO.setUniqueCode(uniqueCode);
            Long userRelationshipId = userRelationship.getUserId();
            List<Long> userIds = new ArrayList<>();
            userIds.add(userRelationshipId);
            LambdaQueryWrapper<UserRelationship> relatedQueryWrapper = new LambdaQueryWrapper<>();
            relatedQueryWrapper.eq(UserRelationship::getUniqueCode, uniqueCode);
            List<UserRelationship> userRelatedRelationships = baseMapper.selectList(relatedQueryWrapper);
            for (UserRelationship userRelatedRelationship : userRelatedRelationships) {
                Long id = userRelatedRelationship.getUserId();
                // 可能会查到自己，相同的就跳过，不重复添加
                if (Objects.equals(userRelationshipId, id)) {
                    continue;
                }
                userIds.add(id);
            }
            if (userIds.size() >= 2) {
                bindStatus = 1;
            }
            userRelationshipDTO.setUserIds(userIds);
        }
        relationDTO.setBindStatus(bindStatus);
        return relationDTO;
    }

    @Override
    public UserRelationshipDTO createRelationship(String relation, UserPrincipal userPrincipal) {
        UserRelationship userRelationship = new UserRelationship();
        userRelationship.setUserId(userPrincipal.getUserId());
        userRelationship.setRelation(relation);
        String code = generateUniqueCode();
        userRelationship.setUniqueCode(code);
        userRelationship.setCreateTime(new Date());
        baseMapper.insert(userRelationship);

        UserRelationshipDTO userRelationshipDTO = new UserRelationshipDTO();
        userRelationshipDTO.setRelation(relation);
        userRelationshipDTO.setUniqueCode(code);
        List<Long> userIds = new ArrayList<>();
        userIds.add(userPrincipal.getUserId());
        userRelationshipDTO.setUserIds(userIds);
        return userRelationshipDTO;
    }

    @Override
    public int bindRelationship(BindUserRequest request, Long bindUserId, UserRelationship userRelationship) {
        // 创建同意绑定用户的关系
        UserRelationship newUserRelationship = new UserRelationship();
        newUserRelationship.setUserId(bindUserId);
        newUserRelationship.setUniqueCode(userRelationship.getUniqueCode());
        newUserRelationship.setRelation(request.getRelation());
        newUserRelationship.setCreateTime(new Date());
        baseMapper.insert(newUserRelationship);

        // 如果是第一次绑定需要，需要设置用户关系
        String relation = userRelationship.getRelation();
        if (StrUtil.isEmpty(relation)) {
            userRelationship.setRelation(request.getRelation());
            baseMapper.updateById(userRelationship);
        }
        return 200;
    }

    @Override
    public String saveSubmit(UserPrincipal userPrincipal, SubmitTextDTO text, String result) {
        return null;
//        UserTextInteractionTogether userTextInteractionTogether = new UserTextInteractionTogether();
//        Long userId = userPrincipal.getUserId();
//        userTextInteractionTogether.setUserId(userId);
//
//        // 解析result中的emotionRatio值
//        JSONObject resultJson = new JSONObject(result);
//        JSONObject data = resultJson.getJSONObject("data");
//        String emotionRatioStr = data.getString("emotionRatio");
//        String emotionStr = data.getString("emotion");
//
//        // 去掉百分号并转换为Double类型
//        double score = (int) Math.floor(Double.parseDouble(emotionRatioStr.replace("%", "")));
//        if (emotionStr.equals("消极的情绪")) {
//            score = 100 - score;
//            userTextInteractionTogether.setScore(score);
//        } else {
//            userTextInteractionTogether.setScore(score);
//        }
//
//        userTextInteractionTogether.setAiResponse(result);
//        userTextInteractionTogether.setInputText(text.getText());
//        userTextInteractionTogether.setLongitude(text.getLongitude());
//        userTextInteractionTogether.setLatitude(text.getLatitude());
//        userTextInteractionTogether.setCreateTime(new Date());
//        // 设置签到时间，补签到设置用户设定的时间
//        if (text.getSupplementarySignIn() == 1 && StrUtil.isNotEmpty(text.getSupplementarySignInTime())) {
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//            userTextInteractionTogether.setSignInTime(LocalDateTime.of(
//                    LocalDate.parse(text.getSupplementarySignInTime(), formatter), java.time.LocalTime.MIDNIGHT));
//            UserFunctionRecord record = userFunctionRecordMapper.selectOne(
//                    new LambdaQueryWrapper<UserFunctionRecord>()
//                            .eq(UserFunctionRecord::getUserId, userPrincipal.getUserId()));
//            // 判断用户功能记录表是否存在，不存在就创建一个
//            if (ObjectUtil.isNull(record)) {
//                UserFunctionRecord userFunctionRecord = new UserFunctionRecord();
//                userFunctionRecord.setUserId(userPrincipal.getUserOpenId());
//                userFunctionRecord.setDailyLimitTimes(5);
//                userFunctionRecord.setAvailableTimesToday(5);
//                userFunctionRecord.setCreateTime(LocalDateTime.now());
//                userFunctionRecordMapper.insert(userFunctionRecord);
//                record = userFunctionRecord;
//            }
//            // TODO 补签到次数-1，当前不限制用户补签到
//            record.setRemainingNumberOfSupplementarySignIn(record.getRemainingNumberOfSupplementarySignIn() - 1);
//            userFunctionRecordMapper.updateById(record);
//        } else {
//            userTextInteractionTogether.setSignInTime(LocalDateTime.now());
//        }
//        userTextInteractionTogether.setSupplementarySignIn(text.getSupplementarySignIn());
//
//        userTextInteractionTogether.setUniqueCode(text.getUniqueCode());
//        userTextInteractionTogetherMapper.insert(userTextInteractionTogether);
//        // 更新 result 中的 emotionRatio
//        resultJson.getJSONObject("data").put("emotionRatio", score);
//        return resultJson.toString();
    }

    @Override
    public PageResult<TogetherHistory> getTogetherHistory(String code, int page, int size) {
        // 2. 查询用户的交互历史
        IPage<UserTextInteractionTogether> interactions = queryInteractionsByCode(code, page, size);
        List<UserTextInteractionTogether> records = interactions.getRecords();
        if (CollUtil.isEmpty(records)) {
            return new PageResult<>(Collections.emptyList(), 0, page, size, 0);
        }

        // 3. 按日期分组交互数据
        Map<String, List<HistoryEmotion>> groupedData = new HashMap<>();
        for (UserTextInteractionTogether record : records) {
            String dateKey = record.getSignInTime().toLocalDate().toString();

            // 创建 HistoryEmotion 对象
            String aiResponseStr = record.getAiResponse();
            AiResponse aiResponse = JSONUtil.toBean(aiResponseStr, AiResponse.class);
            HistoryEmotion historyEmotion = new HistoryEmotion();
            historyEmotion.setInputText(record.getInputText());
            AiResponse.Data data = aiResponse.getData();
            historyEmotion.setEmotion(data.getEmotion());
            historyEmotion.setReminder(data.getReminder());
            historyEmotion.setScore(record.getScore().intValue());

            // 按日期分组
            groupedData
                    .computeIfAbsent(dateKey, k -> new ArrayList<>())
                    .add(historyEmotion);
        }

        // 4. 构建 TogetherHistory 对象
        List<TogetherHistory> resultList = new ArrayList<>();
        for (Map.Entry<String, List<HistoryEmotion>> entry : groupedData.entrySet()) {
            TogetherHistory togetherHistory = new TogetherHistory();
            togetherHistory.setTime(entry.getKey());
            togetherHistory.setHistoryEmotions(entry.getValue());
            resultList.add(togetherHistory);
        }

        // 5. 计算总记录数
        long total = groupedData.size();

        // 6. 创建 PageResult 对象并返回
        int pages = (int) Math.ceil((double) total / size); // 计算总页数
        return new PageResult<>(resultList, total, page, size, pages);
    }

    private IPage<UserTextInteractionTogether> queryInteractionsByCode(String code, int page, int size) {
        // 创建Page对象，设置当前页和每页大小
        Page<UserTextInteractionTogether> pageObj = new Page<>(page, size);
        // 创建LambdaQueryWrapper对象
        LambdaQueryWrapper<UserTextInteractionTogether> queryWrapper = new LambdaQueryWrapper<>();
        // 设置查询条件，这里使用eq方法表示等于userId
        queryWrapper.eq(UserTextInteractionTogether::getUniqueCode, code);
        queryWrapper.orderByDesc(UserTextInteractionTogether::getSignInTime);
        return userTextInteractionTogetherMapper.selectPage(pageObj, queryWrapper);
    }

    private List<Long> getUserIdsByCode(String code) {
        LambdaQueryWrapper<UserRelationship> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserRelationship::getUniqueCode, code);

        List<UserRelationship> relationships = userRelationshipMapper.selectList(queryWrapper);
        return relationships.stream().map(UserRelationship::getUserId).collect(Collectors.toList());
    }

    @Override
    public List<SignInHistoryDTO> getTogetherSignInHistory(UserPrincipal userPrincipal, String month, String code) {
        try {
            // 创建一个日期格式化器，用于解析和格式化日期字符串为指定的格式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
            // 解析给定的月份字符串（格式为"yyyy-MM"），并将其转换为该月的第一天的日期对象
            LocalDate startOfMonth = LocalDate.parse(month + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            // 计算并获取给定月份的最后一天的日期对象
            LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

            // 创建一个查询包装器，用于筛选用户文本互动记录
            LambdaQueryWrapper<UserTextInteractionTogether> queryWrapper = new LambdaQueryWrapper<>();
            // 通过用户ID筛选记录，确保查询的是当前用户（通过用户主体获取用户开放ID）
            queryWrapper.eq(UserTextInteractionTogether::getUserId, userPrincipal.getUserId())
                    // 筛选类型为1的记录，即设置查询条件为特定类型的用户文本互动
                    .eq(UserTextInteractionTogether::getUniqueCode, code)
                    // 筛选创建时间大于等于本月开始时间的记录，即设置查询条件的开始时间
                    .ge(UserTextInteractionTogether::getSignInTime, startOfMonth.atStartOfDay())
                    // 筛选创建时间小于等于本月结束时间的记录，即设置查询条件的结束时间
                    .le(UserTextInteractionTogether::getSignInTime, endOfMonth.atTime(23, 59, 59));

            List<UserTextInteractionTogether> signInRecords = userTextInteractionTogetherMapper.selectList(queryWrapper);

            List<SignInHistoryDTO> result = new ArrayList<>();
            for (LocalDate date = startOfMonth; !date.isAfter(endOfMonth); date = date.plusDays(1)) {
                SignInHistoryDTO item = new SignInHistoryDTO();
                item.setData(date.format(formatter.ofPattern("yyyy-MM-dd")));
                item.setSignIn(0);
                result.add(item);
            }

            // 签到记录为空直接返回初始化的签到记录
            if (signInRecords.isEmpty()) {
                return result;
            }

            for (UserTextInteractionTogether record : signInRecords) {
                String dateStr = record.getSignInTime().toLocalDate().format(formatter.ofPattern("yyyy-MM-dd"));
                for (SignInHistoryDTO item : result) {
                    if (item.getData().equals(dateStr)) {
                        item.setSignIn(1);
                        break;
                    }
                }
            }

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to get check-in history", e);
        }
    }


    private String generateUniqueCode() {
        String s = ShortCodeGenerator.generateShortCode(8);
        // 使用baseMapper查询code是否已存在
        while (baseMapper.selectOne(new LambdaQueryWrapper<UserRelationship>().eq(UserRelationship::getUniqueCode, s)) != null) {
            s = ShortCodeGenerator.generateShortCode(8);
        }
        return s;
    }
}
