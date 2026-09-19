package com.emotion.api.service;

import com.emotion.api.repository.po.EnglishMainText;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 存储英文学习材料的主表 服务类
 * </p>
 *
 * @author xiajing
 * @since 2024-12-20
 */
public interface IEnglishMainTextService extends IService<EnglishMainText> {

    boolean addQuestion(String text);
}
