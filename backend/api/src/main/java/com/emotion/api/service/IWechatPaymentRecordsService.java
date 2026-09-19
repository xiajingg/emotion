package com.emotion.api.service;

import com.emotion.api.repository.po.WechatPaymentRecords;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 微信支付记录表 服务类
 * </p>
 *
 * @author xiajing
 * @since 2024-11-26
 */
public interface IWechatPaymentRecordsService extends IService<WechatPaymentRecords> {

    /**
     * 通过userId和outTradeNo查询一条支付记录
     */
    WechatPaymentRecords findByUserIdAndOutTradeNo(Long userId, String outTradeNo);

    WechatPaymentRecords findByOutTradeNo(String outTradeNo);

    List<WechatPaymentRecords> findByUserId(Long userId);
}
