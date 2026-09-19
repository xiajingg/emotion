package com.emotion.api.repository.dao.rds;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emotion.api.repository.po.PaymentSequence;

/**
 * <p>
 * 支付序列表，确保每个支付请求都有唯一的编号 Mapper 接口
 * </p>
 *
 * @author xiajing
 * @since 2024-11-25
 */
public interface PaymentSequenceMapper extends BaseMapper<PaymentSequence> {

}
