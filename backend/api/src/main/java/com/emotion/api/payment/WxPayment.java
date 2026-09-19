package com.emotion.api.payment;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import org.apache.commons.codec.binary.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class WxPayment {

    /**
     * 商户号
     */
    public static String merchantId = "1699691753";
    /**
     * 商户API私钥路径
     */
    public static String privateKeyPath = "apiclient_key.pem";
    /**
     * 商户证书序列号
     */
    public static String merchantSerialNumber = "2E5D5D3849811623AF21FC53E61E4267F326A264";
    /**
     * 商户APIV3密钥
     */
    public static String apiV3key = "sDO8CTjk9czCUhAg736qNW8rUVN10hzj";

    public static String appId = "wxc5dd3169f9790fa3";

    public static String prePayNotifyUrl = "https://onekey.vip.cpolar.top/pay/xiajing/onekey/prepay/notify";
    public static String refundNotifyUrl = "https://onekey.vip.cpolar.top/pay/xiajing/onekey/refund/notify";

    // 定义一个静态的 Config 实例
    private static volatile RSAAutoCertificateConfig configInstance;
    private static final Object lock = new Object();

    // 提供一个静态方法来获取 Config 实例
    public static RSAAutoCertificateConfig getConfigInstance() {
        if (configInstance == null) {
            synchronized (lock) {
                if (configInstance == null) {
                    configInstance = new RSAAutoCertificateConfig.Builder()
                            .merchantId(merchantId)
                            .privateKeyFromPath(privateKeyPath)
                            .merchantSerialNumber(merchantSerialNumber)
                            .apiV3Key(apiV3key)
                            .build();
                }
            }
        }
        return configInstance;
    }

    /**
     * 统一下单
     * @param total
     * @param productTitle
     * @param outTradeNo
     * @param userOpenId
     * @return
     */
    public static PrepayWithRequestPaymentResponse prepay(Integer total, String productTitle, String outTradeNo, String userOpenId) {
        // 使用单例模式获取配置
        Config config = getConfigInstance();
        // 构建service
        JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(config).build();
        // request.setXxx(val)设置所需参数，具体参数可见Request定义
        PrepayRequest request = new PrepayRequest();
        Amount amount = new Amount();
        // 金额(单位分)
        amount.setTotal(total);
        request.setAmount(amount);
        request.setAppid(appId);
        request.setMchid(merchantId);
        // 支付项目名称
        request.setDescription(productTitle);
        // 回调地址
        request.setNotifyUrl(prePayNotifyUrl);
        // 交易编号
        request.setOutTradeNo(outTradeNo);
        Payer payer = new Payer();
        // 用户openid
        payer.setOpenid(userOpenId);
        request.setPayer(payer);
        // 调用下单方法，得到应答
        PrepayWithRequestPaymentResponse prepayWithRequestPaymentResponse = service.prepayWithRequestPayment(request);
        return prepayWithRequestPaymentResponse;
    }

    /**
     * 退款
     * @param outTradeNo 内部支付单号
     * @param outRefundNo 内部退款单号, 自己生成
     * @param reason 退款原因, 不必填, 业务必须填, 用于用户画像
     * @param total 订单金额
     * @param refund 退款金额
     * @return
     */
    public static Refund createRefund(String outTradeNo,String outRefundNo, String reason, Long total, Long refund) {
        // 使用单例模式获取配置
        Config config = getConfigInstance();
        // 构建service
        RefundService service = new RefundService.Builder().config(config).build();
        CreateRequest request = new CreateRequest();
        request.setOutTradeNo(outTradeNo);
        request.setOutRefundNo(outRefundNo);
        request.setReason(reason);
        request.setNotifyUrl(refundNotifyUrl);
        AmountReq amount = new AmountReq();
        amount.setTotal(total);
        amount.setRefund(refund);
        amount.setCurrency("CNY");
        request.setAmount(amount);
        Refund refundResponse = service.create(request);
        return refundResponse;
    }
}
