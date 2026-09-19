package com.emotion.api.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.dto.EmotionResultResponse;
import com.emotion.api.dto.FriendAverageScoreVO;
import com.emotion.api.dto.FriendBindStatusVO;
import com.emotion.api.dto.FriendLinkAnalysisVO;
import com.emotion.api.dto.FriendShareHistoryVO;
import com.emotion.api.dto.FriendTimelineVO;
import com.emotion.api.dto.FriendTrendDataVO;
import com.emotion.api.dto.HistoryEmotion;
import com.emotion.api.repository.dao.rds.ImageTextMapper;
import com.emotion.api.repository.dao.rds.ImageUploadRecordMapper;
import com.emotion.api.repository.dao.rds.FriendLinkAnalysisMapper;
import com.emotion.api.exception.BusinessException;
import com.emotion.api.repository.mapper.FriendBindMapper;
import com.emotion.api.repository.po.ImageText;
import com.emotion.api.repository.po.ImageUploadRecord;
import com.emotion.api.repository.po.FriendBind;
import com.emotion.api.repository.po.FriendLinkAnalysis;
import com.emotion.api.repository.po.UserTextInteraction;
import com.emotion.api.repository.po.WechatUser;
import com.emotion.api.repository.dao.rds.UserTextInteractionMapper;
import com.emotion.api.repository.dao.rds.WechatUserMapper;
import com.emotion.api.service.IFriendBindService;
import com.emotion.api.service.OllamaDirectService;
import com.emotion.api.util.ShortCodeGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FriendBindServiceImpl implements IFriendBindService {

    @Autowired
    private FriendBindMapper friendBindMapper;

    @Autowired
    private UserTextInteractionMapper interactionMapper;
    
    @Autowired
    private WechatUserMapper wechatUserMapper;

    @Autowired
    private ImageTextMapper imageTextMapper;

    @Autowired
    private ImageUploadRecordMapper imageUploadRecordMapper;

    @Autowired
    private FriendLinkAnalysisMapper friendLinkAnalysisMapper;

    @Autowired
    private OllamaDirectService ollamaDirectService;
    
    // ✅ 用于并发控制的锁对象（key: userId, value: 锁对象）
    private final java.util.concurrent.ConcurrentMap<Long, Object> userLocks = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public String getOrCreateShareCode(UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();
        
        // ✅ 1. 第一次检查（无锁，快速路径）
        LambdaQueryWrapper<FriendBind> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendBind::getUserId, userId)
               .orderByDesc(FriendBind::getCreateTime)
               .last("LIMIT 1");
        FriendBind bind = friendBindMapper.selectOne(wrapper);

        if (bind != null && StrUtil.isNotBlank(bind.getBindCode())) {
            log.info("用户 {} 已有共享码: {}", userId, bind.getBindCode());
            return bind.getBindCode();
        }
        
        // ✅ 2. 获取用户级别的锁（确保同一用户的并发请求串行化）
        Object lock = userLocks.computeIfAbsent(userId, k -> new Object());
        
        synchronized (lock) {
            try {
                // ✅ 3. 第二次检查（持有锁，防止重复插入）
                LambdaQueryWrapper<FriendBind> retryWrapper = new LambdaQueryWrapper<>();
                retryWrapper.eq(FriendBind::getUserId, userId)
                           .orderByDesc(FriendBind::getCreateTime)
                           .last("LIMIT 1");
                FriendBind retryBind = friendBindMapper.selectOne(retryWrapper);
                
                if (retryBind != null && StrUtil.isNotBlank(retryBind.getBindCode())) {
                    log.info("用户 {} 在锁内查询到共享码: {}", userId, retryBind.getBindCode());
                    return retryBind.getBindCode();
                }
                
                // ✅ 4. 生成并插入共享码（此时已确保不会并发插入）
                String code = generateUniqueCode();
                FriendBind newBind = new FriendBind();
                newBind.setUserId(userId);
                newBind.setBindCode(code);
                newBind.setStatus(0); // 0 表示待绑定状态
                friendBindMapper.insert(newBind);
                
                log.info("为用户 {} 生成新共享码: {}", userId, code);
                return code;
            } finally {
                // ✅ 5. 清理锁对象，避免内存泄漏（可选，但推荐）
                // 注意：如果频繁创建/销毁用户，可以考虑保留锁对象或使用弱引用
                // userLocks.remove(userId);
            }
        }
    }

    @Override
    @Transactional
    public void bindFriend(UserPrincipal userPrincipal, String friendCode) {
        Long currentUserId = userPrincipal.getUserId();
        
        // ✅ 1. 前置校验：获取当前用户的共享码
        String myShareCode = getOrCreateShareCode(userPrincipal);
        if (StrUtil.isNotBlank(myShareCode) && myShareCode.equals(friendCode)) {
            throw new BusinessException("不能绑定自己，请输入好友的共享码");
        }
        
        // ✅ 2. 前置校验：检查当前用户是否已经绑定了其他好友（一对一限制）
        if (hasValidBinding(currentUserId)) {
            throw new BusinessException("您已经与好友绑定，无法再绑定其他好友。如需更换好友，请先解绑。");
        }
        
        // ✅ 3. 查找共享码对应的目标用户记录
        LambdaQueryWrapper<FriendBind> targetWrapper = new LambdaQueryWrapper<>();
        targetWrapper.eq(FriendBind::getBindCode, friendCode)
                     .eq(FriendBind::getStatus, 0); // 只查找待绑定的记录
        List<FriendBind> targetBinds = friendBindMapper.selectList(targetWrapper);
        
        // 有效性检查：确保只有一条待绑定记录
        if (targetBinds.isEmpty()) {
            throw new BusinessException("共享码无效或已被使用");
        }
        if (targetBinds.size() > 1) {
            log.warn("共享码 {} 存在多条待绑定记录，取第一条", friendCode);
        }
        
        FriendBind targetBind = targetBinds.get(0);
        Long targetUserId = targetBind.getUserId();
        
        // ✅ 4. 再次检查：目标用户是否就是当前用户（双重保险）
        if (targetUserId.equals(currentUserId)) {
            throw new BusinessException("不能绑定自己，请输入好友的共享码");
        }
        
        // ✅ 5. 检查目标用户是否也已经绑定了其他人
        if (hasValidBinding(targetUserId)) {
            throw new BusinessException("该好友已经与其他人绑定，无法重复绑定");
        }
        
        // ✅ 6. 更新双方记录的 status 为 1（已绑定）
        // 6.1 更新目标用户的记录
        targetBind.setStatus(1);
        friendBindMapper.updateById(targetBind);
        
        // 6.2 更新当前用户的记录
        LambdaQueryWrapper<FriendBind> currentUserWrapper = new LambdaQueryWrapper<>();
        currentUserWrapper.eq(FriendBind::getUserId, currentUserId)
                          .eq(FriendBind::getBindCode, myShareCode)
                          .eq(FriendBind::getStatus, 0);
        FriendBind currentUserBind = friendBindMapper.selectOne(currentUserWrapper);
        
        if (currentUserBind != null) {
            // 当前用户有待绑定记录，更新为已绑定
            currentUserBind.setStatus(1);
            // ✅ 统一使用目标共享码
            currentUserBind.setBindCode(friendCode);
            friendBindMapper.updateById(currentUserBind);
        } else {
            // 当前用户没有待绑定记录，创建一条新的已绑定记录
            FriendBind newBind = new FriendBind();
            newBind.setUserId(currentUserId);
            newBind.setBindCode(friendCode);
            newBind.setStatus(1);
            friendBindMapper.insert(newBind);
        }
        
        log.info("用户 {} 与用户 {} 成功建立好友绑定关系，共享码: {}", currentUserId, targetUserId, friendCode);
    }

    @Override
    public FriendShareHistoryVO getFriendShareHistory(UserPrincipal userPrincipal, String friendCode) {
        Long currentUserId = userPrincipal.getUserId();
        
        // ✅ 校验绑定关系并获取好友ID（必须是已绑定状态）
        LambdaQueryWrapper<FriendBind> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendBind::getUserId, currentUserId)
               .eq(FriendBind::getBindCode, friendCode)
               .eq(FriendBind::getStatus, 1); // 只允许查看已绑定的好友数据
        FriendBind bind = friendBindMapper.selectOne(wrapper);
        
        if (bind == null) {
            throw new BusinessException("无权查看该好友的数据或未建立绑定关系");
        }
        
        // ✅ 通过 bind_code 查询所有已绑定的用户，排除自己后即为好友
        Long friendUserId = getFriendUserIdByCode(friendCode, currentUserId);
        if (friendUserId == null) {
            throw new BusinessException("未找到好友信息");
        }
        
        // ✅ 获取最近8天的数据（包含当天 + 前7天）
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(7);  // 修正：从6改为7
        
        List<UserTextInteraction> myRecords = queryInteractions(currentUserId, startDate, endDate);
        List<UserTextInteraction> friendRecords = queryInteractions(friendUserId, startDate, endDate);
        
        return buildHistoryVO(myRecords, friendRecords, startDate, endDate);
    }

    @Override
    public FriendBindStatusVO getBindStatus(UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();
        FriendBindStatusVO vo = new FriendBindStatusVO();
        
        // 1. 获取用户的共享码
        vo.setShareCode(getOrCreateShareCode(userPrincipal));
        
        // ✅ 2. 获取当前用户昵称
        WechatUser currentUser = wechatUserMapper.selectById(userId);
        if (currentUser != null) {
            vo.setMyNickname(StrUtil.isNotBlank(currentUser.getNickname()) 
                ? currentUser.getNickname() 
                : "我");
        } else {
            vo.setMyNickname("我");
        }
        
        // ✅ 3. 检查是否已绑定好友（查询 status=1 的记录）
        LambdaQueryWrapper<FriendBind> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendBind::getUserId, userId)
               .eq(FriendBind::getStatus, 1);
        
        List<FriendBind> binds = friendBindMapper.selectList(wrapper);
        
        // 过滤出真正的好友绑定记录（status=1 且不是初始化记录）
        FriendBind bind = binds.stream()
                .filter(b -> b.getStatus() == 1)
                .findFirst()
                .orElse(null);
        
        if (bind != null) {
            vo.setIsBound(true);
            
            // ✅ 通过 bind_code 查询所有已绑定的用户，排除自己后即为好友
            Long friendUserId = getFriendUserIdByCode(bind.getBindCode(), userId);
            vo.setFriendUserId(friendUserId);
            
            // 查询好友的用户信息
            if (friendUserId != null) {
                WechatUser friendUser = wechatUserMapper.selectById(friendUserId);
                if (friendUser != null) {
                    // 优先使用昵称，如果昵称为空则使用OpenID
                    vo.setFriendNickname(StrUtil.isNotBlank(friendUser.getNickname()) 
                        ? friendUser.getNickname() 
                        : friendUser.getOpenId());
                    vo.setFriendOpenId(friendUser.getOpenId());
                } else {
                    vo.setFriendNickname("未知用户");
                }
                
                log.info("用户 {} 已绑定好友，好友ID: {}, 昵称: {}", userId, friendUserId, vo.getFriendNickname());
            }
        } else {
            vo.setIsBound(false);
            log.info("用户 {} 未绑定好友", userId);
        }
        
        return vo;
    }
    
    @Override
    public FriendAverageScoreVO getAverageScores(UserPrincipal userPrincipal) {
        Long currentUserId = userPrincipal.getUserId();
        validateBinding(currentUserId);
        
        // ✅ 获取最近8天的数据（包含当天 + 前7天）
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(7);  // 修正：从6改为7
        
        List<UserTextInteraction> myRecords = queryInteractions(currentUserId, startDate, endDate);
        Long friendUserId = getFriendUserId(currentUserId);
        List<UserTextInteraction> friendRecords = queryInteractions(friendUserId, startDate, endDate);
        
        FriendAverageScoreVO vo = new FriendAverageScoreVO();
        vo.setMyAverageScore(calculateAverage(myRecords));
        vo.setFriendAverageScore(calculateAverage(friendRecords));
        
        log.info("获取平均分数 - 我的平均分: {}, 好友平均分: {}", vo.getMyAverageScore(), vo.getFriendAverageScore());
        return vo;
    }
    
    @Override
    public FriendTrendDataVO getTrendData(UserPrincipal userPrincipal) {
        Long currentUserId = userPrincipal.getUserId();
        validateBinding(currentUserId);
        
        // ✅ 参照UserServiceImpl.getLineChart：从昨天往前7天
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime endOfRange = today.minusDays(1).withHour(23).withMinute(59).withSecond(59).withNano(999);
        LocalDateTime startOfRange = endOfRange.minusDays(6).withHour(0).withMinute(0).withSecond(0).withNano(0);
        
        List<UserTextInteraction> myRecords = queryInteractionsByTime(currentUserId, startOfRange, endOfRange);
        Long friendUserId = getFriendUserId(currentUserId);
        List<UserTextInteraction> friendRecords = queryInteractionsByTime(friendUserId, startOfRange, endOfRange);
        
        return buildTrendData(myRecords, friendRecords, startOfRange, endOfRange);
    }
    
    @Override
    public FriendTimelineVO getTimeline(UserPrincipal userPrincipal) {
        Long currentUserId = userPrincipal.getUserId();
        validateBinding(currentUserId);
        
        // ✅ 获取最近8天的数据（包含当天 + 前7天）
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(7);  // 修正：从6改为7
        
        List<UserTextInteraction> myRecords = queryInteractions(currentUserId, startDate, endDate);
        Long friendUserId = getFriendUserId(currentUserId);
        List<UserTextInteraction> friendRecords = queryInteractions(friendUserId, startDate, endDate);
        
        return buildTimeline(myRecords, friendRecords, startDate, endDate);
    }

    @Override
    public FriendLinkAnalysisVO getTimelineAnalysis(UserPrincipal userPrincipal) {
        Long currentUserId = userPrincipal.getUserId();
        validateBinding(currentUserId);
        String bindCode = getBindCode(currentUserId);
        LocalDate analysisDate = LocalDate.now();

        FriendLinkAnalysis stored = getStoredAnalysis(bindCode, analysisDate);
        if (stored != null) {
            return toFriendLinkAnalysisVO(stored);
        }

        FriendLinkAnalysis generated = generateAndSaveAnalysis(bindCode, currentUserId, analysisDate);
        return toFriendLinkAnalysisVO(generated);
    }

    @Override
    public void generateDailyFriendLinkAnalysisForAllBindings() {
        LocalDate analysisDate = LocalDate.now();
        LambdaQueryWrapper<FriendBind> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendBind::getStatus, 1);
        List<FriendBind> binds = friendBindMapper.selectList(wrapper);
        Map<String, List<FriendBind>> grouped = binds.stream()
                .filter(bind -> StrUtil.isNotBlank(bind.getBindCode()))
                .collect(Collectors.groupingBy(FriendBind::getBindCode));

        grouped.forEach((bindCode, groupBinds) -> {
            if (groupBinds.size() < 2 || getStoredAnalysis(bindCode, analysisDate) != null) {
                return;
            }
            try {
                generateAndSaveAnalysis(bindCode, groupBinds.get(0).getUserId(), analysisDate);
            } catch (Exception e) {
                log.warn("生成心灵链接每日分析失败，bindCode={}", bindCode, e);
            }
        });
    }

    private FriendLinkAnalysis generateAndSaveAnalysis(String bindCode, Long currentUserId, LocalDate analysisDate) {
        FriendLinkAnalysis existing = getStoredAnalysis(bindCode, analysisDate);
        if (existing != null) {
            return existing;
        }

        LocalDate endDate = analysisDate;
        LocalDate startDate = endDate.minusDays(7);
        Long friendUserId = getFriendUserIdByCode(bindCode, currentUserId);
        List<UserTextInteraction> myRecords = queryInteractions(currentUserId, startDate, endDate);
        List<UserTextInteraction> friendRecords = queryInteractions(friendUserId, startDate, endDate);

        FriendLinkAnalysisVO vo = buildFriendLinkFallback(myRecords, friendRecords, startDate, endDate);
        FriendLinkAnalysis analysis = new FriendLinkAnalysis();
        analysis.setBindCode(bindCode);
        analysis.setAnalysisDate(analysisDate);
        analysis.setTitle(vo.getTitle());
        analysis.setSummary(vo.getSummary());
        analysis.setSuggestion(vo.getSuggestion());
        analysis.setMeCount(vo.getMeCount());
        analysis.setFriendCount(vo.getFriendCount());
        analysis.setSameDayCount(vo.getSameDayCount());
        analysis.setCreateTime(LocalDateTime.now());
        analysis.setUpdateTime(LocalDateTime.now());

        String prompt = buildFriendLinkPrompt(myRecords, friendRecords);
        try {
            EmotionResultResponse result = ollamaDirectService.analyzeEmotion(prompt, "心灵链接");
            analysis.setTitle(result.getEmotion_tag() == null ? "今日共鸣分析" : result.getEmotion_tag());
            analysis.setSummary(result.getAi_comment() == null ? vo.getSummary() : result.getAi_comment());
            if (result.getRelief_actions() != null && !result.getRelief_actions().isEmpty()) {
                analysis.setSuggestion(result.getRelief_actions().get(0));
            }
            analysis.setAnswerBook(result.getAnswer_book());
            analysis.setAiResponse(JSONUtil.toJsonStr(result));
        } catch (Exception e) {
            log.warn("心灵链接AI分析失败，使用兜底结果", e);
        }

        try {
            friendLinkAnalysisMapper.insert(analysis);
            return analysis;
        } catch (Exception e) {
            FriendLinkAnalysis retry = getStoredAnalysis(bindCode, analysisDate);
            if (retry != null) {
                return retry;
            }
            throw e;
        }
    }

    @Override
    public HistoryEmotion getTimelineDetail(UserPrincipal userPrincipal, Long id) {
        if (id == null) {
            return null;
        }
        Long currentUserId = userPrincipal.getUserId();
        validateBinding(currentUserId);
        Long friendUserId = getFriendUserId(currentUserId);

        UserTextInteraction record = interactionMapper.selectById(id);
        if (record == null || (!Objects.equals(record.getUserId(), currentUserId) && !Objects.equals(record.getUserId(), friendUserId))) {
            return null;
        }
        return buildHistoryEmotion(record);
    }

    private FriendLinkAnalysisVO buildFriendLinkFallback(List<UserTextInteraction> myRecords,
                                                         List<UserTextInteraction> friendRecords,
                                                         LocalDate startDate,
                                                         LocalDate endDate) {
        FriendLinkAnalysisVO vo = new FriendLinkAnalysisVO();
        vo.setTitle("今日共鸣分析");
        vo.setMeCount(myRecords.size());
        vo.setFriendCount(friendRecords.size());
        vo.setSameDayCount(countSameDay(myRecords, friendRecords));
        vo.setUpdatedAt(DateTimeFormatter.ofPattern("MM-dd HH:mm").format(LocalDateTime.now()));

        if (myRecords.isEmpty() && friendRecords.isEmpty()) {
            vo.setSummary("最近 7 天还没有足够记录，先各自留下一个状态，再看你们的情绪节奏。");
            vo.setSuggestion("先发起一次轻量记录，不急着解释原因。");
            return vo;
        }
        vo.setSummary("AI 正在根据最近 7 天的记录观察你们的共鸣节奏。");
        vo.setSuggestion("用一句低压力问候开启连接，比追问原因更容易被接住。");
        return vo;
    }

    private FriendLinkAnalysis getStoredAnalysis(String bindCode, LocalDate analysisDate) {
        if (StrUtil.isBlank(bindCode) || analysisDate == null) {
            return null;
        }
        LambdaQueryWrapper<FriendLinkAnalysis> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendLinkAnalysis::getBindCode, bindCode)
                .eq(FriendLinkAnalysis::getAnalysisDate, analysisDate)
                .last("LIMIT 1");
        return friendLinkAnalysisMapper.selectOne(wrapper);
    }

    private FriendLinkAnalysisVO toFriendLinkAnalysisVO(FriendLinkAnalysis analysis) {
        FriendLinkAnalysisVO vo = new FriendLinkAnalysisVO();
        vo.setTitle(analysis.getTitle());
        vo.setSummary(analysis.getSummary());
        vo.setSuggestion(analysis.getSuggestion());
        vo.setAnswerBook(analysis.getAnswerBook());
        vo.setMeCount(analysis.getMeCount());
        vo.setFriendCount(analysis.getFriendCount());
        vo.setSameDayCount(analysis.getSameDayCount());
        vo.setUpdatedAt(analysis.getUpdateTime() == null
                ? ""
                : DateTimeFormatter.ofPattern("MM-dd HH:mm").format(analysis.getUpdateTime()));
        return vo;
    }

    private String getBindCode(Long currentUserId) {
        LambdaQueryWrapper<FriendBind> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendBind::getUserId, currentUserId)
                .eq(FriendBind::getStatus, 1)
                .last("LIMIT 1");
        FriendBind bind = friendBindMapper.selectOne(wrapper);
        return bind == null ? null : bind.getBindCode();
    }

    private Integer countSameDay(List<UserTextInteraction> myRecords, List<UserTextInteraction> friendRecords) {
        Set<LocalDate> myDays = myRecords.stream()
                .map(record -> record.getSignInTime().toLocalDate())
                .collect(Collectors.toSet());
        Set<LocalDate> friendDays = friendRecords.stream()
                .map(record -> record.getSignInTime().toLocalDate())
                .collect(Collectors.toSet());
        myDays.retainAll(friendDays);
        return myDays.size();
    }

    private String buildFriendLinkPrompt(List<UserTextInteraction> myRecords, List<UserTextInteraction> friendRecords) {
        StringBuilder builder = new StringBuilder();
        builder.append("你是一个心灵链接功能的AI观察员。请基于两个人最近7天的情绪记录，输出适合产品顶部展示的一段共鸣分析。");
        builder.append("不要泛泛鼓励，不要诊断心理疾病，不要说教。要像懂关系节奏的朋友，指出双方记录节奏、情绪距离和一个轻量行动。");
        builder.append("下面是我的记录：\n");
        appendRecords(builder, myRecords);
        builder.append("\n下面是好友的记录：\n");
        appendRecords(builder, friendRecords);
        return builder.toString();
    }

    private void appendRecords(StringBuilder builder, List<UserTextInteraction> records) {
        records.stream()
                .sorted(Comparator.comparing(UserTextInteraction::getSignInTime).reversed())
                .limit(10)
                .forEach(record -> builder
                        .append(DateTimeFormatter.ofPattern("MM-dd HH:mm").format(record.getSignInTime()))
                        .append(" | ")
                        .append(extractEmotion(record.getAiResponse()))
                        .append(" | ")
                        .append(isImageUrl(record.getInputText()) ? "一张情绪涂鸦" : record.getInputText())
                        .append("\n"));
    }

    /**
     * ✅ 按LocalDateTime范围查询（参照UserServiceImpl.getLineChart）
     */
    private List<UserTextInteraction> queryInteractionsByTime(Long userId, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<UserTextInteraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTextInteraction::getUserId, userId)
               .eq(UserTextInteraction::getType, 0)
               .between(UserTextInteraction::getCreateTime, start, end)
               .orderByAsc(UserTextInteraction::getCreateTime);
        return interactionMapper.selectList(wrapper);
    }
    
    private List<UserTextInteraction> queryInteractions(Long userId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<UserTextInteraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTextInteraction::getUserId, userId)
               .eq(UserTextInteraction::getType, 0) // ✅ 只查询 type=0 的记录
               .ge(UserTextInteraction::getSignInTime, start.atStartOfDay())
               .le(UserTextInteraction::getSignInTime, end.atTime(23, 59, 59))
               .orderByAsc(UserTextInteraction::getSignInTime);
        return interactionMapper.selectList(wrapper);
    }
    
    /**
     * ✅ 校验绑定关系（查询 status=1 的记录）
     */
    private void validateBinding(Long currentUserId) {
        LambdaQueryWrapper<FriendBind> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendBind::getUserId, currentUserId)
               .eq(FriendBind::getStatus, 1);
        
        Long count = friendBindMapper.selectCount(wrapper);
        
        if (count == 0) {
            throw new BusinessException("未绑定好友，无法查看共享数据");
        }
    }
    
    /**
     * ✅ 获取好友ID（通过 bind_code 查询）
     */
    private Long getFriendUserId(Long currentUserId) {
        // 先查询当前用户的绑定记录
        LambdaQueryWrapper<FriendBind> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendBind::getUserId, currentUserId)
               .eq(FriendBind::getStatus, 1);
        FriendBind bind = friendBindMapper.selectOne(wrapper);
        
        if (bind != null) {
            // 通过 bind_code 查询所有已绑定的用户，排除自己
            return getFriendUserIdByCode(bind.getBindCode(), currentUserId);
        }
        
        return null;
    }
    
    /**
     * ✅ 通过 bind_code 查询好友ID（排除当前用户）
     * 支持未来群组扩展：可以返回多个好友ID
     */
    private Long getFriendUserIdByCode(String bindCode, Long currentUserId) {
        LambdaQueryWrapper<FriendBind> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendBind::getBindCode, bindCode)
               .eq(FriendBind::getStatus, 1)
               .ne(FriendBind::getUserId, currentUserId); // 排除自己
        
        List<FriendBind> binds = friendBindMapper.selectList(wrapper);
        
        // 目前只支持一对一，返回第一个好友
        if (!binds.isEmpty()) {
            return binds.get(0).getUserId();
        }
        
        return null;
    }
    
    /**
     * ✅ 检查用户是否有有效的绑定关系（status=1）
     */
    private boolean hasValidBinding(Long userId) {
        LambdaQueryWrapper<FriendBind> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendBind::getUserId, userId)
               .eq(FriendBind::getStatus, 1);
        
        return friendBindMapper.selectCount(wrapper) > 0;
    }
    
    /**
     * ✅ 构建趋势数据（参照UserServiceImpl.getLineChart格式）
     * 无数据时取前一天的值，第一天无数据则取0
     */
    private FriendTrendDataVO buildTrendData(List<UserTextInteraction> myRecords, 
                                             List<UserTextInteraction> friendRecords,
                                             LocalDateTime startOfRange, LocalDateTime endOfRange) {
        // ✅ 按天分组（使用createTime.toLocalDate()）
        Map<LocalDate, List<UserTextInteraction>> myGroupedByDay = myRecords.stream()
                .collect(Collectors.groupingBy(r -> r.getCreateTime().toLocalDate()));
        Map<LocalDate, List<UserTextInteraction>> friendGroupedByDay = friendRecords.stream()
                .collect(Collectors.groupingBy(r -> r.getCreateTime().toLocalDate()));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M月d");
        
        List<FriendTrendDataVO.TrendDataItem> myData = new ArrayList<>();
        List<FriendTrendDataVO.TrendDataItem> friendData = new ArrayList<>();
        
        // ✅ 记录前一天的分数，用于填充无数据的日期
        double previousMyScore = 0; // 第一天无数据时取0
        double previousFriendScore = 0;
        
        // ✅ 从结束日期往前遍历7天
        for (int i = 6; i >= 0; i--) {
            LocalDate date = endOfRange.minusDays(i).toLocalDate();
            
            // 我的数据
            List<UserTextInteraction> myDailyRecords = myGroupedByDay.getOrDefault(date, Collections.emptyList());
            double myScore;
            if (!myDailyRecords.isEmpty()) {
                myScore = Math.floor(myDailyRecords.stream()
                        .mapToDouble(UserTextInteraction::getScore)
                        .average()
                        .orElse(0));
                previousMyScore = myScore; // ✅ 更新前一天的值
            } else {
                myScore = previousMyScore; // ✅ 无数据时取前一天的值
            }
            
            FriendTrendDataVO.TrendDataItem myItem = new FriendTrendDataVO.TrendDataItem();
            myItem.setScore(myScore);
            myItem.setTime(date.format(formatter));
            myData.add(myItem);
            
            // 好友的数据
            List<UserTextInteraction> friendDailyRecords = friendGroupedByDay.getOrDefault(date, Collections.emptyList());
            double friendScore;
            if (!friendDailyRecords.isEmpty()) {
                friendScore = Math.floor(friendDailyRecords.stream()
                        .mapToDouble(UserTextInteraction::getScore)
                        .average()
                        .orElse(0));
                previousFriendScore = friendScore; // ✅ 更新前一天的值
            } else {
                friendScore = previousFriendScore; // ✅ 无数据时取前一天的值
            }
            
            FriendTrendDataVO.TrendDataItem friendItem = new FriendTrendDataVO.TrendDataItem();
            friendItem.setScore(friendScore);
            friendItem.setTime(date.format(formatter));
            friendData.add(friendItem);
        }
        
        FriendTrendDataVO vo = new FriendTrendDataVO();
        vo.setMyData(myData);
        vo.setFriendData(friendData);
        
        return vo;
    }
    
    /**
     * ✅ 构建时间线数据
     */
    private FriendTimelineVO buildTimeline(List<UserTextInteraction> myRecords, 
                                           List<UserTextInteraction> friendRecords,
                                           LocalDate start, LocalDate end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<FriendTimelineVO.TimelineItem> timeline = new ArrayList<>();
        
        Map<String, List<UserTextInteraction>> myGrouped = groupByDate(myRecords, formatter);
        Map<String, List<UserTextInteraction>> friendGrouped = groupByDate(friendRecords, formatter);
        
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            String dateStr = date.format(formatter);
            
            List<FriendTimelineVO.EmotionRecord> dailyRecords = new ArrayList<>();
            if (myGrouped.containsKey(dateStr)) {
                // ✅ 按时间倒序（最新的在最上面）
                List<UserTextInteraction> sortedMyRecords = new ArrayList<>(myGrouped.get(dateStr));
                sortedMyRecords.sort((r1, r2) -> r2.getSignInTime().compareTo(r1.getSignInTime()));
                
                for (UserTextInteraction r : sortedMyRecords) {
                    FriendTimelineVO.EmotionRecord record = convertToTimelineRecord(r, "me");
                    if (record != null) dailyRecords.add(record);
                }
            }
            if (friendGrouped.containsKey(dateStr)) {
                // ✅ 按时间倒序（最新的在最上面）
                List<UserTextInteraction> sortedFriendRecords = new ArrayList<>(friendGrouped.get(dateStr));
                sortedFriendRecords.sort((r1, r2) -> r2.getSignInTime().compareTo(r1.getSignInTime()));
                
                for (UserTextInteraction r : sortedFriendRecords) {
                    FriendTimelineVO.EmotionRecord record = convertToTimelineRecord(r, "friend");
                    if (record != null) dailyRecords.add(record);
                }
            }
            
            if (!dailyRecords.isEmpty()) {
                FriendTimelineVO.TimelineItem item = new FriendTimelineVO.TimelineItem();
                item.setDate(dateStr);
                item.setRecords(dailyRecords);
                timeline.add(item);
            }
        }
        
        // ✅ 时间线按日期倒序（最新的在最上面）
        Collections.reverse(timeline);
        
        FriendTimelineVO vo = new FriendTimelineVO();
        vo.setTimeline(timeline);
        
        return vo;
    }
    
    /**
     * ✅ 转换为时间线记录
     */
    private FriendTimelineVO.EmotionRecord convertToTimelineRecord(UserTextInteraction r, String owner) {
        if (r.getScore() == null) return null;
        FriendTimelineVO.EmotionRecord record = new FriendTimelineVO.EmotionRecord();
        record.setId(r.getId());
        record.setOwner(owner);
        record.setScore(r.getScore().intValue());
        List<Long> imageIds = getImageIds(r.getId());
        List<String> imageUrls = getImageUrls(imageIds);
        boolean doodle = isImageUrl(r.getInputText()) || (r.getInputText() != null && r.getInputText().contains("情绪涂鸦") && !imageUrls.isEmpty());
        record.setDoodle(doodle);
        record.setImgId(imageIds);
        record.setImageUrls(imageUrls);
        record.setContent(doodle ? "" : r.getInputText());
        // ✅ 增加 AI 回复内容
        record.setAiResponse(r.getAiResponse());
        // ✅ 显示完整时间：年月日 时分秒
        record.setCreateTime(r.getSignInTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        record.setEmotion(extractEmotion(r.getAiResponse()));
        return record;
    }

    private HistoryEmotion buildHistoryEmotion(UserTextInteraction record) {
        HistoryEmotion historyEmotion = new HistoryEmotion();
        historyEmotion.setId(record.getId());
        historyEmotion.setInputText(record.getInputText());
        historyEmotion.setScore(record.getScore() == null ? 0 : record.getScore().intValue());
        historyEmotion.setCreateTime(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(record.getSignInTime()));
        List<Long> imageIds = getImageIds(record.getId());
        List<String> imageUrls = getImageUrls(imageIds);
        historyEmotion.setImgId(imageIds);
        historyEmotion.setImageUrls(imageUrls);
        historyEmotion.setDoodle(isImageUrl(record.getInputText()) || (record.getInputText() != null && record.getInputText().contains("情绪涂鸦") && !imageUrls.isEmpty()));
        fillAnalysisFields(historyEmotion, record.getAiResponse());
        return historyEmotion;
    }

    private List<Long> getImageIds(Long textId) {
        return imageTextMapper.selectList(new LambdaQueryWrapper<ImageText>().eq(ImageText::getTextId, textId))
                .stream()
                .map(ImageText::getImgId)
                .collect(Collectors.toList());
    }

    private List<String> getImageUrls(List<Long> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> imageUrlMap = imageUploadRecordMapper.selectBatchIds(imageIds)
                .stream()
                .collect(Collectors.toMap(ImageUploadRecord::getId, ImageUploadRecord::getImageUrl, (left, right) -> left));
        return imageIds.stream().map(imageUrlMap::get).filter(Objects::nonNull).collect(Collectors.toList());
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

    private String extractEmotion(String aiResponseStr) {
        if (StrUtil.isBlank(aiResponseStr)) {
            return "情绪记录";
        }
        try {
            JSONObject data = JSONUtil.parseObj(aiResponseStr).getJSONObject("data");
            if (data != null) {
                String emotion = data.getStr("emotion_tag");
                if (StrUtil.isBlank(emotion)) {
                    emotion = data.getStr("emotion");
                }
                return StrUtil.isBlank(emotion) ? "情绪记录" : emotion;
            }
        } catch (Exception ignored) {
        }
        return "情绪记录";
    }

    private void fillAnalysisFields(HistoryEmotion historyEmotion, String aiResponseStr) {
        if (StrUtil.isBlank(aiResponseStr)) {
            return;
        }
        try {
            JSONObject data = JSONUtil.parseObj(aiResponseStr).getJSONObject("data");
            if (data == null) {
                return;
            }
            String emotionTag = data.getStr("emotion_tag");
            String aiComment = data.getStr("ai_comment");
            historyEmotion.setEmotion(StrUtil.isBlank(emotionTag) ? data.getStr("emotion") : emotionTag);
            historyEmotion.setAiComment(aiComment);
            historyEmotion.setReminder(StrUtil.isBlank(aiComment) ? data.getStr("reminder") : aiComment);
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
        } catch (Exception ignored) {
        }
    }

    private void putIfNotBlank(Map<String, String> map, String key, String value) {
        if (StrUtil.isNotBlank(value)) {
            map.put(key, value);
        }
    }

    private FriendShareHistoryVO buildHistoryVO(List<UserTextInteraction> myRecords, 
                                                List<UserTextInteraction> friendRecords,
                                                LocalDate start, LocalDate end) {
        FriendShareHistoryVO vo = new FriendShareHistoryVO();
        
        // ✅ 计算平均分（基于最近7天的所有记录）
        double myAvgScore = calculateAverage(myRecords);
        double friendAvgScore = calculateAverage(friendRecords);
        vo.setMyAverageScore(myAvgScore);
        vo.setFriendAverageScore(friendAvgScore);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<String> dates = new ArrayList<>();
        List<Integer> myScores = new ArrayList<>();
        List<Integer> friendScores = new ArrayList<>();
        List<FriendShareHistoryVO.TimelineItem> timeline = new ArrayList<>();
        
        Map<String, List<UserTextInteraction>> myGrouped = groupByDate(myRecords, formatter);
        Map<String, List<UserTextInteraction>> friendGrouped = groupByDate(friendRecords, formatter);
        
        // ✅ 按日期正序遍历（用于图表），但时间线会倒序展示
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            String dateStr = date.format(formatter);
            dates.add(dateStr);
            
            // ✅ 计算当天的平均分（如果当天有多条记录）
            int myAvg = getDailyAverage(myGrouped.get(dateStr));
            int friendAvg = getDailyAverage(friendGrouped.get(dateStr));
            
            myScores.add(myAvg);
            friendScores.add(friendAvg);
            
            // ✅ 构建时间线（收集所有记录，后续会倒序）
            List<FriendShareHistoryVO.EmotionRecord> dailyRecords = new ArrayList<>();
            if (myGrouped.containsKey(dateStr)) {
                for (UserTextInteraction r : myGrouped.get(dateStr)) {
                    FriendShareHistoryVO.EmotionRecord record = convertToEmotionRecord(r, "me");
                    if (record != null) dailyRecords.add(record);
                }
            }
            if (friendGrouped.containsKey(dateStr)) {
                for (UserTextInteraction r : friendGrouped.get(dateStr)) {
                    FriendShareHistoryVO.EmotionRecord record = convertToEmotionRecord(r, "friend");
                    if (record != null) dailyRecords.add(record);
                }
            }
            
            if (!dailyRecords.isEmpty()) {
                FriendShareHistoryVO.TimelineItem item = new FriendShareHistoryVO.TimelineItem();
                item.setDate(dateStr);
                item.setRecords(dailyRecords);
                timeline.add(item);
            }
        }
        
        // ✅ 时间线按日期倒序（最新的在最上面）
        Collections.reverse(timeline);
        
        FriendShareHistoryVO.TrendData trend = new FriendShareHistoryVO.TrendData();
        trend.setDates(dates);
        trend.setMyScores(myScores);
        trend.setFriendScores(friendScores);
        
        vo.setTrendData(trend);
        vo.setTimeline(timeline);
        
        log.info("生成好友共享历史 - 我的平均分: {}, 好友平均分: {}, 数据范围: {} ~ {}", 
                 myAvgScore, friendAvgScore, start, end);
        
        return vo;
    }

    private Map<String, List<UserTextInteraction>> groupByDate(List<UserTextInteraction> records, DateTimeFormatter formatter) {
        Map<String, List<UserTextInteraction>> map = new HashMap<>();
        for (UserTextInteraction r : records) {
            String dateStr = r.getSignInTime().format(formatter);
            map.computeIfAbsent(dateStr, k -> new ArrayList<>()).add(r);
        }
        return map;
    }

    private int getDailyAverage(List<UserTextInteraction> records) {
        if (records == null || records.isEmpty()) return 0;
        return calculateAverage(records).intValue();
    }

    private FriendShareHistoryVO.EmotionRecord convertToEmotionRecord(UserTextInteraction r, String owner) {
        if (r.getScore() == null) return null;
        FriendShareHistoryVO.EmotionRecord record = new FriendShareHistoryVO.EmotionRecord();
        record.setOwner(owner);
        record.setScore(r.getScore().intValue());
        record.setContent(r.getInputText());
        // ✅ 只显示时分（日期已在时间线节点显示）
        record.setCreateTime(r.getSignInTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        // 尝试从 AI 响应中提取情绪标签，这里简化处理
        record.setEmotion("情绪记录"); 
        return record;
    }

    private Double calculateAverage(List<UserTextInteraction> records) {
        if (records == null || records.isEmpty()) return 0.0;
        double sum = records.stream().mapToInt(r -> r.getScore() != null ? r.getScore().intValue() : 0).sum();
        return sum / records.size();
    }

    private String generateUniqueCode() {
        String code = ShortCodeGenerator.generateShortCode(8);
        while (friendBindMapper.selectCount(new LambdaQueryWrapper<FriendBind>().eq(FriendBind::getBindCode, code)) > 0) {
            code = ShortCodeGenerator.generateShortCode(8);
        }
        return code;
    }
}
