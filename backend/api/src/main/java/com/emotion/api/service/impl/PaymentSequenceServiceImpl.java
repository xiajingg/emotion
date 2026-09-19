package com.emotion.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emotion.api.repository.dao.rds.PaymentSequenceMapper;
import com.emotion.api.repository.po.PaymentSequence;
import com.emotion.api.service.IPaymentSequenceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * <p>
 * 支付序列表，确保每个支付请求都有唯一的编号 服务实现类
 * </p>
 *
 * @author xiajing
 * @since 2024-11-25
 */
@Service
public class PaymentSequenceServiceImpl extends ServiceImpl<PaymentSequenceMapper, PaymentSequence> implements IPaymentSequenceService {

    @Override
    public synchronized String getPrePaySequence() {
        // 用当前日期查询一条数据
        PaymentSequence todaySequence = getTodayPaymentSequence();
        todaySequence.setPrePayNumber(todaySequence.getPrePayNumber() + 1);
        this.updateById(todaySequence);
        LocalDate date = todaySequence.getDate();
        // date格式化成yyyyMMdd
        String replaceDate = date.toString().replace("-", "");
        // currentId设置为6个字符的字符串, 如果不够6个字符, 前面用0补充
        String id = String.format("%06d", todaySequence.getPrePayNumber());
        return (replaceDate+id);
    }

    @Override
    public synchronized String getRefundSequence() {
        // 用当前日期查询一条数据
        PaymentSequence todaySequence = getTodayPaymentSequence();
        todaySequence.setRefundNumber(todaySequence.getRefundNumber() + 1);
        this.updateById(todaySequence);
        LocalDate date = todaySequence.getDate();
        // date格式化成yyyyMMdd
        String replaceDate = date.toString().replace("-", "");
        // currentId设置为6个字符的字符串, 如果不够6个字符, 前面用0补充
        String id = String.format("%06d", todaySequence.getRefundNumber());
        return (replaceDate+id);
    }

    private synchronized PaymentSequence getTodayPaymentSequence() {
        LambdaQueryWrapper<PaymentSequence> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PaymentSequence::getDate, LocalDate.now());
        PaymentSequence paymentSequence = this.getOne(queryWrapper);
        if (paymentSequence == null) {
            paymentSequence = new PaymentSequence();
            paymentSequence.setDate(LocalDate.now());
            paymentSequence.setPrePayNumber(0);
            paymentSequence.setRefundNumber(0);
            this.save(paymentSequence);
        }
        return paymentSequence;
    }
}
