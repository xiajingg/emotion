package com.emotion.api.service.impl;

import com.emotion.api.repository.po.EnglishScores;
import com.emotion.api.repository.dao.rds.EnglishScoresMapper;
import com.emotion.api.service.IEnglishScoresService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 记录用户的三套题分数及加权总分 服务实现类
 * </p>
 *
 * @author xiajing
 * @since 2024-12-20
 */
@Service
public class EnglishScoresServiceImpl extends ServiceImpl<EnglishScoresMapper, EnglishScores> implements IEnglishScoresService {

}
