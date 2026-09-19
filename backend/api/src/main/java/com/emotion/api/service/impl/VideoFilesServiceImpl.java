package com.emotion.api.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.emotion.api.repository.dao.rds.VideoFilesMapper;
import com.emotion.api.repository.po.VideoFiles;
import com.emotion.api.service.IVideoFilesService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 视频文件存储表 服务实现类
 * </p>
 *
 * @author xiajing
 * @since 2024-04-11
 */
@Service
public class VideoFilesServiceImpl extends ServiceImpl<VideoFilesMapper, VideoFiles> implements IVideoFilesService {

}
