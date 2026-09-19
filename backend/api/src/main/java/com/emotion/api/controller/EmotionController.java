package com.emotion.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.emotion.api.config.BaseResult;
import com.emotion.api.config.user.CurrentUser;
import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.dto.EmotionEventDTO;
import com.emotion.api.dto.EmotionFavoriteDTO;
import com.emotion.api.entity.EmotionEventLog;
import com.emotion.api.entity.EmotionReplyFavorite;
import com.emotion.api.mapper.EmotionEventLogMapper;
import com.emotion.api.mapper.EmotionReplyFavoriteMapper;
import com.emotion.api.repository.dao.rds.UserTextInteractionMapper;
import com.emotion.api.repository.po.UserTextInteraction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/emotion/api/v1")
public class EmotionController {

    @Autowired
    private EmotionEventLogMapper eventLogMapper;

    @Autowired
    private EmotionReplyFavoriteMapper favoriteMapper;

    @Autowired
    private UserTextInteractionMapper userTextInteractionMapper;

    @PostMapping("/events")
    public BaseResult<Void> logEvent(@CurrentUser UserPrincipal userPrincipal, @RequestBody EmotionEventDTO dto) {
        try {
            if (dto == null || dto.getEventType() == null || dto.getEventType().trim().isEmpty()) {
                return BaseResult.error("400", "事件类型不能为空");
            }
            EmotionEventLog event = new EmotionEventLog();
            event.setUserId(userPrincipal.getUserId());
            event.setInteractionId(dto.getInteractionId());
            event.setEventType(dto.getEventType());
            event.setTaskType(dto.getTaskType());
            event.setScenarioKey(dto.getScenarioKey());
            event.setReplyStyle(dto.getReplyStyle());
            event.setExtra(dto.getExtra());
            event.setCreateTime(LocalDateTime.now());
            eventLogMapper.insert(event);
            return BaseResult.success(null);
        } catch (Exception e) {
            log.error("记录情绪事件失败, userId={}, dto={}", userPrincipal.getUserId(), dto, e);
            return BaseResult.success(null);
        }
    }

    @PostMapping("/favorites")
    public BaseResult<EmotionReplyFavorite> createFavorite(@CurrentUser UserPrincipal userPrincipal, @RequestBody EmotionFavoriteDTO dto) {
        if (dto == null || dto.getReplyText() == null || dto.getReplyText().trim().isEmpty()) {
            return BaseResult.error("400", "收藏内容不能为空");
        }
        if (dto.getInteractionId() != null) {
            UserTextInteraction record = userTextInteractionMapper.selectById(dto.getInteractionId());
            if (record == null || !userPrincipal.getUserId().equals(record.getUserId())) {
                return BaseResult.error("403", "无权收藏该记录");
            }
        }
        EmotionReplyFavorite favorite = new EmotionReplyFavorite();
        favorite.setUserId(userPrincipal.getUserId());
        favorite.setInteractionId(dto.getInteractionId());
        favorite.setScenarioKey(dto.getScenarioKey());
        favorite.setReplyStyle(dto.getReplyStyle());
        favorite.setReplyText(dto.getReplyText().trim());
        favorite.setCreateTime(LocalDateTime.now());
        favoriteMapper.insert(favorite);
        logFavoriteEvent(userPrincipal, dto);
        return BaseResult.success(favorite);
    }

    @GetMapping("/favorites")
    public BaseResult<Page<EmotionReplyFavorite>> listFavorites(@CurrentUser UserPrincipal userPrincipal,
                                                                @RequestParam(defaultValue = "1") int page,
                                                                @RequestParam(defaultValue = "20") int size) {
        Page<EmotionReplyFavorite> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<EmotionReplyFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmotionReplyFavorite::getUserId, userPrincipal.getUserId())
                .orderByDesc(EmotionReplyFavorite::getCreateTime);
        return BaseResult.success(favoriteMapper.selectPage(pageObj, wrapper));
    }

    @DeleteMapping("/favorites/{id}")
    public BaseResult<Void> deleteFavorite(@CurrentUser UserPrincipal userPrincipal, @PathVariable Long id) {
        LambdaQueryWrapper<EmotionReplyFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmotionReplyFavorite::getId, id)
                .eq(EmotionReplyFavorite::getUserId, userPrincipal.getUserId());
        favoriteMapper.delete(wrapper);
        return BaseResult.success(null);
    }

    private void logFavoriteEvent(UserPrincipal userPrincipal, EmotionFavoriteDTO dto) {
        try {
            EmotionEventLog event = new EmotionEventLog();
            event.setUserId(userPrincipal.getUserId());
            event.setInteractionId(dto.getInteractionId());
            event.setEventType("favorite_create");
            event.setScenarioKey(dto.getScenarioKey());
            event.setReplyStyle(dto.getReplyStyle());
            event.setCreateTime(LocalDateTime.now());
            eventLogMapper.insert(event);
        } catch (Exception e) {
            log.warn("收藏埋点失败, userId={}", userPrincipal.getUserId(), e);
        }
    }
}
