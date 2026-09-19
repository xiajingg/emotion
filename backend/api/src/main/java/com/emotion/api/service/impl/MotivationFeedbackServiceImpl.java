package com.emotion.api.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.emotion.api.repository.dao.rds.MotivationFeedbackMapper;
import com.emotion.api.repository.po.MotivationFeedback;
import com.emotion.api.service.IMotivationFeedbackService;
import org.springframework.stereotype.Service;

@Service
public class MotivationFeedbackServiceImpl extends ServiceImpl<MotivationFeedbackMapper, MotivationFeedback>
        implements IMotivationFeedbackService {
}
