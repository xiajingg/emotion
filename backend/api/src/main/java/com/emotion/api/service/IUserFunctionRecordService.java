package com.emotion.api.service;

import com.emotion.api.repository.po.UserFunctionRecord;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 业务次数记录表 服务类
 * </p>
 *
 * @author xiajing
 * @since 2024-11-27
 */
public interface IUserFunctionRecordService extends IService<UserFunctionRecord> {

    /**
     * 获取用户剩余次数
     */
    UserFunctionRecord getUserFunctionRecord(Long userId);

}
