package com.emotion.api.repository.dao.rds;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emotion.api.repository.po.Video;

import java.util.List;

public interface VideoMapper extends BaseMapper<Video> {
    List<Video> selectVideosByUserId(String userOpenId);
}
