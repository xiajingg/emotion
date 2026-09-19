package com.emotion.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emotion.api.repository.po.WechatPaymentRecords;
import com.emotion.api.repository.dao.rds.WechatPaymentRecordsMapper;
import com.emotion.api.service.IWechatPaymentRecordsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 微信支付记录表 服务实现类
 * </p>
 *
 * @author xiajing
 * @since 2024-11-26
 */
@Service
public class WechatPaymentRecordsServiceImpl extends ServiceImpl<WechatPaymentRecordsMapper, WechatPaymentRecords> implements IWechatPaymentRecordsService {

    @Override
    public WechatPaymentRecords findByUserIdAndOutTradeNo(Long userId, String outTradeNo) {
        LambdaQueryWrapper<WechatPaymentRecords> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WechatPaymentRecords::getUserId, userId);
        queryWrapper.eq(WechatPaymentRecords::getOutTradeNo, outTradeNo);
        WechatPaymentRecords one = this.getOne(queryWrapper);
        return one;
    }

    @Override
    public WechatPaymentRecords findByOutTradeNo(String outTradeNo) {
        LambdaQueryWrapper<WechatPaymentRecords> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WechatPaymentRecords::getOutTradeNo, outTradeNo);
        WechatPaymentRecords one = this.getOne(queryWrapper);
        return one;
    }

    @Override
    public List<WechatPaymentRecords> findByUserId(Long userId) {
        LambdaQueryWrapper<WechatPaymentRecords> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WechatPaymentRecords::getUserId, userId);
        List<WechatPaymentRecords> list = this.list(queryWrapper);
        return list;
    }
}
