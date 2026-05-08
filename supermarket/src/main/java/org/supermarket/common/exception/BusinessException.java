package org.supermarket.common.exception;

import lombok.Getter;
import org.supermarket.common.result.ResultCode;

@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    // 直接传错误信息
    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.FAILED.getCode();
    }

    // 传枚举（推荐用这个）
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    // 自定义 code + message
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}