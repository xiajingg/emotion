package com.emotion.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.emotion.api.repository.po.PaymentSequence;

/**
 * <p>
 * 支付序列表，确保每个支付请求都有唯一的编号 服务类
 * </p>
 *
 * @author xiajing
 * @since 2024-11-25
 */
public interface IPaymentSequenceService extends IService<PaymentSequence> {

    /**
     * 获取当前日期的支付流水号
     */
    String getPrePaySequence();

    /**
     * 获取当前日期的退款流水号
     */
    String getRefundSequence();
}
