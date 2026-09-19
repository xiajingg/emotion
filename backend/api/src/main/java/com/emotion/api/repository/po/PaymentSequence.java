package com.emotion.api.repository.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 支付序列表，确保每个支付请求都有唯一的编号
 * </p>
 *
 * @author xiajing
 * @since 2024-11-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("payment_sequence")
public class PaymentSequence implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，自动递增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 日期，表示每一天的支付序列
     */
    private LocalDate date;

    /**
     * 当前数字，表示该天的用支付编号
     */
    private Integer prePayNumber;


    private Integer refundNumber;
}
