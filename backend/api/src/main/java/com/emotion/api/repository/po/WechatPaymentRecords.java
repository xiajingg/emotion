package com.emotion.api.repository.po;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.time.LocalDateTime;
import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 微信支付记录表
 * </p>
 *
 * @author xiajing
 * @since 2024-11-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wechat_payment_records")
public class WechatPaymentRecords implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键，自增ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 微信支付交易号，用于唯一标识一次支付交易
     */
    private String transactionId;

    /**
     * 商户订单号，由商户生成，用于唯一标识一次支付请求
     */
    private String outTradeNo;

    /**
     * 用户ID，关联用户表
     */
    private Long userId;

    /**
     * 支付金额，单位为元
     */
    private Long amount;

    /**
     * 货币种类，如CNY
     */
    private String currency;

    /**
     * 支付时间，记录实际支付成功的时间
     */
    private LocalDateTime paymentTime;

    /**
     * 支付状态，如SUCCESS、FAIL、REFUND等
     */
    private String status;

    /**
     * 支付方式，如JSAPI、APP、H5等
     */
    private String paymentMethod;

    /**
     * 订单描述，用于记录支付的具体内容
     */
    private String description;

    /**
     * 回调通知URL，微信支付回调的地址
     */
    private String notifyUrl;

    /**
     * 记录创建时间，即发起支付请求的时间
     */
    private LocalDateTime createdAt;

    /**
     * 记录更新时间，每次更新记录时自动更新
     */
    private LocalDateTime updatedAt;

    /**
     * 用户发起支付请求时的IP地址
     */
    private String ipAddress;

    /**
     * 设备信息，如设备型号、操作系统等
     */
    private String deviceInfo;

    /**
     * 退款状态，如NOT_REFUND、PARTIAL_REFUND、FULL_REFUND等
     */
    private String refundStatus;

    /**
     * 退款金额，单位为元
     */
    private Long refundAmount;

    /**
     * 退款时间，记录实际退款成功的时间
     */
    private LocalDateTime refundTime;


    private String outRefundNo;

    private String refundId;

    private String refundReason;

}
