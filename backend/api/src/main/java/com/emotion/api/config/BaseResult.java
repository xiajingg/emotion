package com.emotion.api.config;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 统一的业务返回包装类
 */
@Data
@Accessors(chain = true)
@RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
public class BaseResult<T> implements java.io.Serializable {

    private static final long serialVersionUID = -1702542373929399568L;

    private boolean success = false;
    private String code;
    private String msg;
    private T data;

    /**
     * 正常返回
     *
     * @param data 数据内容
     * @param <T>  返回数据的类型
     * @return 包装后的返回
     */
    public static <T> BaseResult<T> create(T data) {
        return BaseResult.<T>of().setSuccess(true).setData(data);
    }

    /**
     * 正常返回，包含正常 BaseErrorCodeEnum.SUCCESS
     *
     * @param data 数据内容
     * @param <T>  返回数据的类型
     * @return 包装后的返回
     */
    public static <T> BaseResult<T> success(T data) {
        return BaseResult.<T>of().setSuccess(true).setData(data).setCode("0").setMsg("成功");
    }

    /**
     * 错误返回
     */
    public static <T> BaseResult<T> error(String code, String msg) {
        return BaseResult.<T>of().setSuccess(false).setCode(code).setMsg(msg);
    }

    /**
     * 错误返回
     */
    public static <T> BaseResult<T> error(BaseResult BaseResult) {
        return BaseResult.<T>error(BaseResult.getCode(), BaseResult.getMsg());
    }

    /**
     * 错误返回
     */
    public static <T> BaseResult<T> error(String desc) {
        return BaseResult.<T>error("500", desc);
    }
    
}