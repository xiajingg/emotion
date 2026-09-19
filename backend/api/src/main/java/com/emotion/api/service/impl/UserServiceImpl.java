package com.emotion.api.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.emotion.api.config.BaseResult;
import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.dto.*;
import com.emotion.api.repository.dao.rds.ImageTextMapper;
import com.emotion.api.repository.dao.rds.ImageUploadRecordMapper;
import com.emotion.api.repository.dao.rds.UserMapper;
import com.emotion.api.repository.dao.rds.UserTextInteractionMapper;
import com.emotion.api.repository.dao.rds.UserTextToImgMapper;
import com.emotion.api.repository.po.*;
import com.emotion.api.repository.mapper.UserUsageLogMapper;
import com.emotion.api.service.IUserFunctionRecordService;
import com.emotion.api.service.IUserService;
import com.emotion.api.util.ZhipuAI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author xiajing
 * @since 2024-03-29
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    private UserTextInteractionMapper userTextInteractionMapper;

    @Autowired
    private IUserFunctionRecordService userFunctionRecordService;

    @Autowired
    private UserUsageLogMapper userUsageLogMapper;
    @Autowired
    private ImageTextMapper imageTextMapper;
    @Autowired
    private ImageUploadRecordMapper imageUploadRecordMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private UserTextToImgMapper userTextToImgMapper;

    @Override
    public User findUserByUserName(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return this.getOne(wrapper);
    }
    @Override
    public synchronized Long saveSubmit(Long userId, SubmitTextDTO text, String result, Double emotionAvgScore, String aiText) {
        // 查询用户功能记录
        UserFunctionRecord record = userFunctionRecordService.getUserFunctionRecord(userId);
        LocalDateTime signInTime = LocalDateTime.now();
        // 如果是签到类型，且签到时间不为空
        if (text.getType() == 1 && text.getSupplementarySignIn() == 1) {
            // 计算还有没有签到次数
            int i = record.getTotalRemakeLimit() - record.getUsedRemakeCount();
            if (i <= 0){
                throw new RuntimeException("补签次数不足");
            }
            record.setUsedRemakeCount(record.getUsedRemakeCount() + 1);
            userFunctionRecordService.updateById(record);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            signInTime= LocalDateTime.of(
                    LocalDate.parse(text.getSupplementarySignInTime(), formatter), java.time.LocalTime.MIDNIGHT);
        }

        // ===== 已禁用：次数限制（推广期无限使用，2026-05-21）=====
        // if (text.getType() == 0){
        //     long daily = record.getDailyLimitTimes() - record.getUsedTimesToday();
        //     long total = record.getTotalUsageLimit() - record.getUsedUsageCount();
        //     if (daily <= 0 && total <= 0){
        //         throw new RuntimeException("使用次数不足, 点击增加次数");
        //     }
        //     
        //     // 记录扣减前的余额
        //     int balanceBefore = (int) (daily > 0 ? daily : total);
        //     
        //     if (daily > 0){
        //         record.setUsedTimesToday(record.getUsedTimesToday() + 1);
        //         userFunctionRecordService.updateById(record);
        //     }else {
        //         record.setUsedUsageCount(record.getUsedUsageCount() + 1);
        //         userFunctionRecordService.updateById(record);
        //     }
        //     
        //     // 记录扣减后的余额
        //     int balanceAfter = (int) (daily > 0 ? 
        //         (record.getDailyLimitTimes() - record.getUsedTimesToday()) : 
        //         (record.getTotalUsageLimit() - record.getUsedUsageCount()));
        //     
        //     // 记录次数变动日志
        //     UserUsageLog usageLog = new UserUsageLog();
        //     usageLog.setUserId(userId);
        //     usageLog.setOperationType("EMOTION_ANALYSIS");
        //     usageLog.setChangeAmount(-1);
        //     usageLog.setBalanceBefore(balanceBefore);
        //     usageLog.setBalanceAfter(balanceAfter);
        //     usageLog.setRemark("情绪分析：" + text.getText().substring(0, Math.min(50, text.getText().length())));
        //     usageLog.setCreateTime(LocalDateTime.now());
        //     userUsageLogMapper.insert(usageLog);
        // }
        UserTextInteraction userTextInteraction = new UserTextInteraction();
        userTextInteraction.setUserId(userId);
        // 解析result中的emotionRatio值
        userTextInteraction.setScore(emotionAvgScore);
        userTextInteraction.setAiResponse(result);
        userTextInteraction.setInputText(text.getText());
        userTextInteraction.setLongitude(text.getLongitude());
        userTextInteraction.setLatitude(text.getLatitude());
        userTextInteraction.setCreateTime(LocalDateTime.now());
        userTextInteraction.setSignInTime(signInTime);
        userTextInteraction.setSupplementarySignIn(text.getSupplementarySignIn());
        userTextInteraction.setType(text.getType());
        userTextInteraction.setAiText(aiText.toString());
        userTextInteractionMapper.insert(userTextInteraction);
        // 插入图片
        if (text.getUploadFileId() != null) {
            text.getUploadFileId().forEach(fileId -> {
                ImageText imageText = new ImageText();
                imageText.setTextId(userTextInteraction.getId());
                imageText.setImgId(fileId);
                imageTextMapper.insert(imageText);
            });
        }
        // 更新 result 中的 emotionRatio
        return userTextInteraction.getId();
    }

    @Override
    public PageResult<HistoryEmotion> getUserHistorySubmit(UserPrincipal userPrincipal, int page, int size, int type) {
        // 创建Page对象，设置当前页和每页大小
        Page<UserTextInteraction> pageObj = new Page<>(page, size);
        // 创建LambdaQueryWrapper对象
        LambdaQueryWrapper<UserTextInteraction> queryWrapper = new LambdaQueryWrapper<>();
        // 设置查询条件，这里使用eq方法表示等于userId
        queryWrapper.eq(UserTextInteraction::getUserId, userPrincipal.getUserId());
        // 设置查询条件，这里使用eq方法表示等于type，0:等于普通提交，1:代表签到提交
        queryWrapper.eq(UserTextInteraction::getType, type);
        // 使用select方法选择需要返回的字段
        if (type == 0) {
            // 设置排序规则，这里使用orderByDesc方法表示降序排序
            queryWrapper.orderByDesc(UserTextInteraction::getCreateTime);
            queryWrapper.select(UserTextInteraction::getId, UserTextInteraction::getInputText, UserTextInteraction::getAiResponse,
                    UserTextInteraction::getScore, UserTextInteraction::getCreateTime);
        } else {
            queryWrapper.orderByDesc(UserTextInteraction::getSignInTime);
            queryWrapper.select(UserTextInteraction::getId, UserTextInteraction::getInputText, UserTextInteraction::getAiResponse,
                    UserTextInteraction::getScore, UserTextInteraction::getSignInTime);
        }
        // 执行分页查询
        IPage<UserTextInteraction> userTextInteractions = userTextInteractionMapper.selectPage(pageObj, queryWrapper);
        List<HistoryEmotion> historyEmotionList = new ArrayList<>();
        List<UserTextInteraction> records = userTextInteractions.getRecords();
        for (UserTextInteraction record : records) {
            historyEmotionList.add(buildHistoryEmotion(record, type));
        }

        // 创建PageResult对象，包含分页数据和总页数
        return new PageResult<>(
                historyEmotionList,
                userTextInteractions.getTotal(),
                (int) userTextInteractions.getCurrent(),
                (int) userTextInteractions.getSize(),
                (int) userTextInteractions.getPages()
        );
    }

    @Override
    public HistoryEmotion getUserHistorySubmitDetail(UserPrincipal userPrincipal, Long id) {
        if (id == null) {
            return null;
        }
        UserTextInteraction record = userTextInteractionMapper.selectById(id);
        if (record == null || !Objects.equals(record.getUserId(), userPrincipal.getUserId())) {
            return null;
        }
        return buildHistoryEmotion(record, record.getType());
    }

    private HistoryEmotion buildHistoryEmotion(UserTextInteraction record, int type) {
        HistoryEmotion historyEmotion = new HistoryEmotion();
        historyEmotion.setId(record.getId());
        historyEmotion.setInputText(record.getInputText());
        historyEmotion.setScore(record.getScore() == null ? 0 : record.getScore().intValue());
        historyEmotion.setViewHint("点击查看完整分析");

        if (type == 0) {
            historyEmotion.setCreateTime(DateUtil.format(record.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
        } else {
            historyEmotion.setCreateTime(DateUtil.format(record.getSignInTime(), "yyyy-MM-dd HH:mm:ss"));
        }

        List<Long> imageIds = imageTextMapper.selectList(
                new LambdaQueryWrapper<ImageText>().eq(ImageText::getTextId, record.getId()))
                .stream()
                .map(ImageText::getImgId)
                .collect(Collectors.toList());
        historyEmotion.setImgId(imageIds);
        historyEmotion.setImageUrls(getImageUrls(imageIds));

        boolean doodle = isImageUrl(record.getInputText())
                || (record.getInputText() != null && record.getInputText().contains("情绪涂鸦") && CollUtil.isNotEmpty(historyEmotion.getImageUrls()));
        historyEmotion.setDoodle(doodle);

        fillAnalysisFields(historyEmotion, record.getAiResponse());
        return historyEmotion;
    }

    private List<String> getImageUrls(List<Long> imageIds) {
        if (CollUtil.isEmpty(imageIds)) {
            return Collections.emptyList();
        }
        List<ImageUploadRecord> imageRecords = imageUploadRecordMapper.selectBatchIds(imageIds);
        Map<Long, String> imageUrlMap = imageRecords.stream()
                .collect(Collectors.toMap(ImageUploadRecord::getId, ImageUploadRecord::getImageUrl, (left, right) -> left));
        return imageIds.stream()
                .map(imageUrlMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean isImageUrl(String value) {
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return (lower.startsWith("http://") || lower.startsWith("https://"))
                && (lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains(".png")
                || lower.contains(".webp") || lower.contains("/emotion/"));
    }

    private void fillAnalysisFields(HistoryEmotion historyEmotion, String aiResponseStr) {
        if (aiResponseStr == null || aiResponseStr.trim().isEmpty()) {
            return;
        }
        try {
            JSONObject root = JSONUtil.parseObj(aiResponseStr);
            JSONObject data = root.getJSONObject("data");
            if (data != null) {
                String emotionTag = data.getStr("emotion_tag");
                String aiComment = data.getStr("ai_comment");
                historyEmotion.setEmotion(emotionTag != null ? emotionTag : data.getStr("emotion"));
                historyEmotion.setAiComment(aiComment);
                historyEmotion.setReminder(aiComment != null ? aiComment : data.getStr("reminder"));
                historyEmotion.setReliefActions(data.getBeanList("relief_actions", String.class));
                historyEmotion.setAnswerBook(data.getStr("answer_book"));

                JSONObject repliesObj = data.getJSONObject("replies");
                if (repliesObj != null) {
                    Map<String, String> replies = new HashMap<>();
                    putIfNotBlank(replies, "high_eq", repliesObj.getStr("high_eq"));
                    putIfNotBlank(replies, "crazy", repliesObj.getStr("crazy"));
                    putIfNotBlank(replies, "gentle", repliesObj.getStr("gentle"));
                    putIfNotBlank(replies, "sarcastic", repliesObj.getStr("sarcastic"));
                    historyEmotion.setReplies(replies);
                }
                if (historyEmotion.getEmotion() != null || historyEmotion.getReminder() != null) {
                    return;
                }
            }
        } catch (Exception ignored) {
            // 继续尝试兼容旧版 ai_response。
        }

        try {
            AiResponse aiResponse = JSONUtil.toBean(aiResponseStr, AiResponse.class);
            if (aiResponse != null && aiResponse.getData() != null) {
                AiResponse.Data data = aiResponse.getData();
                historyEmotion.setEmotion(data.getEmotion());
                historyEmotion.setReminder(data.getReminder());
            }
        } catch (Exception ignored) {
            // 历史脏数据不影响列表展示。
        }
    }

    private void putIfNotBlank(Map<String, String> map, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            map.put(key, value);
        }
    }

    @Override
    public List<LineChartData> getLineChart(UserPrincipal userPrincipal, int days) {
        // 获取当前日期的days天前的日期
        LocalDateTime today = LocalDateTime.now();

        // 计算指定天数的日期范围
        LocalDateTime endOfRange = today.minusDays(1).withHour(23).withMinute(59).withSecond(59).withNano(999); // 前一天
        LocalDateTime startOfRange = endOfRange.minusDays(days - 1).withHour(0).withMinute(0).withSecond(0).withNano(0); // days天前

        // 创建查询条件
        LambdaQueryWrapper<UserTextInteraction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserTextInteraction::getUserId, userPrincipal.getUserId())
                .between(UserTextInteraction::getCreateTime, startOfRange, endOfRange);

        // 查询数据库中的数据
        List<UserTextInteraction> interactions = userTextInteractionMapper.selectList(queryWrapper);

        // 使用Map按天分组，key为LocalDate，value为该天的所有UserTextInteraction
        Map<LocalDate, List<UserTextInteraction>> groupedByDay = interactions.stream()
                .collect(Collectors.groupingBy(interaction -> interaction.getCreateTime().toLocalDate()));

        // 创建一个列表来保存LineChartData
        List<LineChartData> lineChartDataList = new ArrayList<>();

        // 保存前一天的score，初始值为-1表示无数据
        double previousScore = -1;

        // 在方法开始部分添加日期格式化器
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M月d");

        // 从结束日期往前遍历days天（从endOfRange到startOfRange）
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = endOfRange.minusDays(i).toLocalDate(); // 获取每一天的日期

            // 获取当天的交互记录
            List<UserTextInteraction> dailyInteractions = groupedByDay.getOrDefault(date, Collections.emptyList());

            double averageScore;

            if (!dailyInteractions.isEmpty()) {
                // 如果当天有数据，计算平均分数
                averageScore = Math.floor(dailyInteractions.stream()
                        .mapToDouble(UserTextInteraction::getScore)
                        .average()
                        .orElse(0));
                previousScore = averageScore; // 更新前一天的分数
            } else {
                // 如果当天没有数据，返回-1表示无数据（前端用虚线表示）
                averageScore = -1;
            }

            // 创建LineChartData对象并设置数据
            LineChartData lineChartData = new LineChartData();
            lineChartData.setScore(averageScore);
            lineChartData.setTime(date.format(formatter));

            // 将LineChartData对象添加到列表中
            lineChartDataList.add(lineChartData);
        }

        // 返回最后生成的折线图数据
        return lineChartDataList;
    }

    @Override
    public List<SignInHistoryDTO> getUserSignInHistory(UserPrincipal userPrincipal, String month) {
        try {
            // 创建一个日期格式化器，用于解析和格式化日期字符串为指定的格式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
            // 解析给定的月份字符串（格式为"yyyy-MM"），并将其转换为该月的第一天的日期对象
            LocalDate startOfMonth = LocalDate.parse(month + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            // 计算并获取给定月份的最后一天的日期对象
            LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

            // 创建一个查询包装器，用于筛选用户文本互动记录
            LambdaQueryWrapper<UserTextInteraction> queryWrapper = new LambdaQueryWrapper<>();
            // 通过用户ID筛选记录，确保查询的是当前用户（通过用户主体获取用户开放ID）
            queryWrapper.eq(UserTextInteraction::getUserId, userPrincipal.getUserId())
                    // 筛选类型为1的记录，即设置查询条件为特定类型的用户文本互动
                    .eq(UserTextInteraction::getType, 1)
                    // 筛选创建时间大于等于本月开始时间的记录，即设置查询条件的开始时间
                    .ge(UserTextInteraction::getSignInTime, startOfMonth.atStartOfDay())
                    // 筛选创建时间小于等于本月结束时间的记录，即设置查询条件的结束时间
                    .le(UserTextInteraction::getSignInTime, endOfMonth.atTime(23, 59, 59));

            List<UserTextInteraction> signInRecords = userTextInteractionMapper.selectList(queryWrapper);

            List<SignInHistoryDTO> result = new ArrayList<>();
            for (LocalDate date = startOfMonth; !date.isAfter(endOfMonth); date = date.plusDays(1)) {
                SignInHistoryDTO item = new SignInHistoryDTO();
                item.setData(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                item.setSignIn(0);
                result.add(item);
            }

            // 签到记录为空直接返回初始化的签到记录
            if (signInRecords.isEmpty()) {
                return result;
            }

            for (UserTextInteraction record : signInRecords) {
                String dateStr = record.getSignInTime().toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                for (SignInHistoryDTO item : result) {
                    if (item.getData().equals(dateStr)) {
                        item.setSignIn(1);
                        break;
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Integer getRemakeLimit(UserPrincipal userPrincipal) {
        UserFunctionRecord userFunctionRecord = userFunctionRecordService.getUserFunctionRecord(userPrincipal.getUserId());
        return userFunctionRecord.getTotalRemakeLimit()- userFunctionRecord.getUsedRemakeCount();
    }

    @Override
    public UserTextInteractionsAnalysisData getUserTextInteractionsAnalysisData(Long userId, Integer type) {
        // 查询用户总条数
        LambdaQueryWrapper<UserTextInteraction> totalQueryWrapper = new LambdaQueryWrapper<>();
        totalQueryWrapper.eq(UserTextInteraction::getUserId, userId);
        totalQueryWrapper.eq(UserTextInteraction::getType, type);
        List<UserTextInteraction> totalUserTextInteractions = userTextInteractionMapper.selectList(totalQueryWrapper);
        UserTextInteractionsAnalysisData analysisData = new UserTextInteractionsAnalysisData();
        int totalCount = totalUserTextInteractions.size();
        analysisData.setTotalCount(totalCount);

        // 查询用户总平均分
        analysisData.setTotalAvgScore(calculateAverageScore(totalUserTextInteractions));

        // 查询用户月平均分
        Date date = new Date();
        // 查询用户本月平均分
        DateTime beginOfMonth = DateUtil.beginOfMonth(date);
        DateTime endOfMonth = DateUtil.endOfMonth(date);
        List<UserTextInteraction> monthInteractions = queryUserTextInteractions(userId, type, beginOfMonth, endOfMonth);
        analysisData.setMonthAvgScore(calculateAverageScore(monthInteractions));

        // 查询用户周平均
        DateTime beginOfWeek = DateUtil.beginOfWeek(date);
        DateTime endOfWeek = DateUtil.endOfWeek(date);
        List<UserTextInteraction> weekInteractions = queryUserTextInteractions(userId, type, beginOfWeek, endOfWeek);
        analysisData.setWeekAvgScore(calculateAverageScore(weekInteractions));
        return analysisData;
    }

    @Override
    public BaseResult<String> covertImage(Long userId, SubmitTextDTO text) {
        // ===== 已禁用：次数限制（推广期无限使用，2026-05-21）=====
        // UserFunctionRecord record = userFunctionRecordService.getUserFunctionRecord(userId);
        // long daily = record.getTxt2ImgDailyTotal() - record.getTxt2ImgDailyUsed();
        // long total = record.getTxt2ImgTotal() - record.getTxt2ImgUsed();
        // if (daily <= 0 && total <= 0){
        //     throw new RuntimeException("使用次数不足, 点击增加次数");
        // }
        // 调用AI接口
        String s = restTemplate
                .getForObject("http://100.107.179.128:8090/api/ai/chatImg?text=" + text.getText(), String.class);
        String img = ZhipuAI.textToImaUrl(s + ", 风格: 抽象");

        // ===== 已禁用：次数扣减（2026-05-21）=====
        // if (daily > 0) {
        //     record.setTxt2ImgDailyUsed(record.getTxt2ImgDailyUsed() + 1);
        // } else {
        //     record.setTxt2ImgUsed(record.getTxt2ImgUsed() + 1);
        // }
        // record.setUpdateTime(LocalDateTime.now());
        // userFunctionRecordService.updateById(record);
        // 保存用户使用记录
        UserTextToImg userTextToImg = new UserTextToImg();
        userTextToImg.setUserId(userId);
        userTextToImg.setImgUrl(img);
        userTextToImg.setCreateTime(LocalDateTime.now());
        userTextToImgMapper.insert(userTextToImg);
        return BaseResult.success(img);
    }

    private List<UserTextInteraction> queryUserTextInteractions(Long userId, Integer type, DateTime startDate,
                                                                DateTime endDate) {
        LambdaQueryWrapper<UserTextInteraction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserTextInteraction::getUserId, userId);
        queryWrapper.eq(UserTextInteraction::getType, type);

        if (startDate != null && endDate != null) {
            if (type == 1) {
                queryWrapper.between(UserTextInteraction::getSignInTime, startDate, endDate);
            } else {
                queryWrapper.between(UserTextInteraction::getCreateTime, startDate, endDate);
            }
        }

        return userTextInteractionMapper.selectList(queryWrapper);
    }

    // 计算平均分的方法
    private int calculateAverageScore(List<UserTextInteraction> interactions) {
        if (CollUtil.isEmpty(interactions)) {
            return 0;
        }
        int totalScore = 0;
        for (UserTextInteraction interaction : interactions) {
            totalScore += interaction.getScore();
        }
        return totalScore / interactions.size();
    }

}
