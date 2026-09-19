package com.emotion.api.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emotion.api.config.BaseResult;
import com.emotion.api.repository.po.VideoFiles;
import com.emotion.api.service.IVideoFilesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 视频文件存储表 前端控制器
 * </p>
 *
 * @author xiajing
 * @since 2024-04-11
 */
@RestController
@RequestMapping("/video-files")
public class VideoFilesController {

    @Autowired
    private IVideoFilesService videoFilesService;

    /**
     * 上传视频
     */
    @PostMapping("/upload")
    public BaseResult<String> uploadVideo(@RequestBody VideoFiles videoFiles) {
        videoFiles.setUserId(1);
        videoFilesService.save(videoFiles);
        return BaseResult.success("上传成功");
    }

    /**
     * 视频列表
     */
    @GetMapping("/page")
    public BaseResult<List<VideoFiles>> pageVideo(@RequestParam("authorName") String authorName) {
        LambdaQueryWrapper<VideoFiles> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VideoFiles::getAuthorName, authorName);
        List<VideoFiles> list = videoFilesService.list(queryWrapper);
        return BaseResult.success(list);
    }
}

