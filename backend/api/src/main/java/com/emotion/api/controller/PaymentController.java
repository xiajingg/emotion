// ============================================================
// 微信支付接口已暂时禁用 (2026-05-21)
// 如需重新启用，删除整个文件内容的注释即可
// ============================================================

// package com.emotion.api.controller;

// import cn.hutool.json.JSONUtil;
// import com.emotion.api.config.BaseResult;
// import com.emotion.api.config.user.CurrentUser;
// import com.emotion.api.config.user.UserPrincipal;
// import com.emotion.api.payment.WxPayment;
// import com.emotion.api.repository.po.UserFunctionRecord;
// import com.emotion.api.repository.po.WechatPaymentRecords;
// import com.emotion.api.service.IPaymentSequenceService;
// import com.emotion.api.service.IUserFunctionRecordService;
// import com.emotion.api.service.IWechatPaymentRecordsService;
// import com.emotion.api.util.DingTalkMsg;
// import com.wechat.pay.java.core.notification.NotificationParser;
// import com.wechat.pay.java.service.partnerpayments.jsapi.model.Transaction;
// import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
// import com.wechat.pay.java.service.refund.model.Refund;
// import com.wechat.pay.java.service.refund.model.RefundNotification;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.web.bind.annotation.*;

// import java.time.LocalDateTime;
// import java.util.List;

// @Slf4j
// @RestController
// @RequestMapping("/pay")
// public class PaymentController {

//     @Autowired
//     private DingTalkMsg dingTalkMsg;
//     @Autowired
//     private IPaymentSequenceService paymentSequenceService;
//     @Autowired
//     private IWechatPaymentRecordsService wechatPaymentRecordsService;
//     @Autowired
//     private IUserFunctionRecordService userFunctionRecordService;

//     @PostMapping("/xiajing/onekey/prepay/notify")
//     public void payNotify(@RequestHeader("Wechatpay-Signature") String wechatSignature,
//                           @RequestHeader("Wechatpay-Serial") String wechatPaySerial,
//                           @RequestHeader("Wechatpay-Nonce") String wechatpayNonce,
//                           @RequestHeader("Wechatpay-Timestamp") String wechatTimestamp,
//                           @RequestHeader("Wechatpay-Signature-Type") String signatureType,
//                           @RequestBody String requestBody) {
//         // 打印上面的所有参数, 方便调试
//         log.info("wechatSignature: {}, wechatPaySerial: {}, wechatpayNonce: {}, wechatTimestamp: {}, signatureType: {}, requestBody: {}",
//                 wechatSignature, wechatPaySerial, wechatpayNonce, wechatTimestamp, signatureType, requestBody);
//         // 构建RequestParam, 这是下面的回调通知解析器需要用到的参数
//         com.wechat.pay.java.core.notification.RequestParam requestParam = new com.wechat.pay.java.core.notification.RequestParam.Builder()
//                 .serialNumber(wechatPaySerial)
//                 .nonce(wechatpayNonce)
//                 .signature(wechatSignature)
//                 .timestamp(wechatTimestamp)
//                 .body(requestBody)
//                 .build();
//         // 初始化 回调通知解析器, 传参和支付的配置一样
//         NotificationParser parser = new NotificationParser(WxPayment.getConfigInstance());
//         // sdk自动 验签、解密并转换成 Transaction(交易信息)
//         Transaction transaction = parser.parse(requestParam, Transaction.class);
//         // 更新支付记录表
//         WechatPaymentRecords wechatPaymentRecord = wechatPaymentRecordsService.findByOutTradeNo(transaction.getOutTradeNo());
//         wechatPaymentRecord.setStatus(transaction.getTradeState().name());
//         wechatPaymentRecord.setTransactionId(transaction.getTransactionId());
//         wechatPaymentRecordsService.updateById(wechatPaymentRecord);
//         // 上面都成功后, 处理业务: 增加用户可使用次数
//         UserFunctionRecord userFunctionRecord = userFunctionRecordService.getUserFunctionRecord(wechatPaymentRecord.getUserId());
//         userFunctionRecord.setTotalUsageLimit(userFunctionRecord.getTotalUsageLimit() + 1);
//         userFunctionRecordService.updateById(userFunctionRecord);
//         // 自己好玩, 发个钉钉消息
//         dingTalkMsg.sendMsgToDingTalk(JSONUtil.toJsonPrettyStr(transaction));
//     }

//     @GetMapping("/prepay")
//     public PrepayWithRequestPaymentResponse prepay(@RequestParam("type") Integer type, @CurrentUser UserPrincipal userPrincipal) {
//         // 使用钉钉发送预支付消息
//         dingTalkMsg.sendMsgToDingTalk("user: " + userPrincipal.getUserOpenId() + "发起预支付");
//         // 单位分
//         Integer total = 10;
//         String productTitle = "分析次数+1";
//         String outTradeNo = paymentSequenceService.getPrePaySequence();
//         WechatPaymentRecords wechatPaymentRecord = new WechatPaymentRecords();
//         wechatPaymentRecord.setUserId(userPrincipal.getUserId());
//         wechatPaymentRecord.setOutTradeNo(outTradeNo);
//         wechatPaymentRecord.setAmount(total.longValue());
//         wechatPaymentRecord.setPaymentMethod("微信");
//         wechatPaymentRecord.setDescription(productTitle);
//         wechatPaymentRecord.setPaymentTime(LocalDateTime.now());
//         wechatPaymentRecord.setStatus("PREPAY");
//         wechatPaymentRecordsService.save(wechatPaymentRecord);
//         return WxPayment.prepay(total, productTitle, outTradeNo, userPrincipal.getUserOpenId());
//     }


//     @GetMapping("/list/payments")
//     public BaseResult<List<WechatPaymentRecords>> listPayments(@CurrentUser UserPrincipal userPrincipal) {
//         // 基于springSecurity管理的CurrentUser拿到用户ID, 去查询支付记录
//         List<WechatPaymentRecords> byUserId = wechatPaymentRecordsService.findByUserId(userPrincipal.getUserId());
//         return BaseResult.success(byUserId);
//     }

//     @GetMapping("/refund")
//     public BaseResult<String> refund(@RequestParam("outTradeNo") String outTradeNo, @RequestParam("reason") String reason, @CurrentUser UserPrincipal userPrincipal) {
//         // 生成唯一的退款单号
//         String outRefundNo = paymentSequenceService.getRefundSequence();
//         // 通过userId和唯一支付编号查询支付记录, 这样就能避免别人直接调接口乱退
//         WechatPaymentRecords wechatPaymentRecord = wechatPaymentRecordsService.findByUserIdAndOutTradeNo(userPrincipal.getUserId(), outTradeNo);
//         if (wechatPaymentRecord == null) {
//             return null;
//         }
//         // 先记录用户的操作位申请退款状态
//         wechatPaymentRecord.setRefundReason(reason);
//         wechatPaymentRecord.setRefundAmount(wechatPaymentRecord.getAmount());
//         wechatPaymentRecord.setRefundStatus("APPLY");
//         wechatPaymentRecord.setRefundTime(LocalDateTime.now());
//         wechatPaymentRecordsService.updateById(wechatPaymentRecord);
//         // 从支付记录表查询订单金额, 填充到总金额和退款金额中, 我这里退款就是退全部
//         Refund refund = WxPayment.createRefund(outTradeNo, outRefundNo, reason, wechatPaymentRecord.getAmount(), wechatPaymentRecord.getAmount());
//         wechatPaymentRecord.setRefundStatus(refund.getStatus().name());
//         wechatPaymentRecord.setRefundTime(LocalDateTime.now());
//         wechatPaymentRecordsService.updateById(wechatPaymentRecord);
//         return BaseResult.success(refund.getStatus().name());
//     }


//     @PostMapping("/xiajing/onekey/refund/notify")
//     public void refundNotify(@RequestHeader("Wechatpay-Signature") String wechatSignature,
//                              @RequestHeader("Wechatpay-Serial") String wechatPaySerial,
//                              @RequestHeader("Wechatpay-Nonce") String wechatpayNonce,
//                              @RequestHeader("Wechatpay-Timestamp") String wechatTimestamp,
//                              @RequestHeader("Wechatpay-Signature-Type") String signatureType,
//                              @RequestBody String requestBody) {
//         // 打印上面的所有参数
//         log.info("wechatSignature: {}, wechatPaySerial: {}, wechatpayNonce: {}, wechatTimestamp: {}, signatureType: {}, requestBody: {}",
//                 wechatSignature, wechatPaySerial, wechatpayNonce, wechatTimestamp, signatureType, requestBody);
//         // 构建RequestParam
//         com.wechat.pay.java.core.notification.RequestParam requestParam = new com.wechat.pay.java.core.notification.RequestParam.Builder()
//                 .serialNumber(wechatPaySerial)
//                 .nonce(wechatpayNonce)
//                 .signature(wechatSignature)
//                 .timestamp(wechatTimestamp)
//                 .body(requestBody)
//                 .build();
//         // 初始化 NotificationParser
//         NotificationParser parser = new NotificationParser(WxPayment.getConfigInstance());
//         // sdk自动 验签、解密并转换成 RefundNotification(退款信息)
//         RefundNotification transaction = parser.parse(requestParam, RefundNotification.class);
//         WechatPaymentRecords wechatPaymentRecord = wechatPaymentRecordsService.findByOutTradeNo(transaction.getOutTradeNo());
//         wechatPaymentRecord.setRefundStatus(transaction.getRefundStatus().name());
//         wechatPaymentRecord.setRefundAmount(transaction.getAmount().getRefund());
//         wechatPaymentRecord.setRefundTime(LocalDateTime.now());
//         wechatPaymentRecord.setOutRefundNo(transaction.getOutRefundNo());
//         wechatPaymentRecord.setRefundId(transaction.getRefundId());
//         wechatPaymentRecordsService.updateById(wechatPaymentRecord);
//         dingTalkMsg.sendMsgToDingTalk(JSONUtil.toJsonPrettyStr(transaction));
//     }
// }
