package org.supermarket.common.result;


import lombok.Getter;

@Getter
public enum ResultCode {
    // 成功
    SUCCESS(200, "操作成功"),

    // 通用错误
    FAILED(500, "操作失败"),
    VALIDATE_FAILED(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "资源不存在"),

    // 用户相关 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_DISABLED(1002, "用户已被禁用"),
    USERNAME_OR_PASSWORD_ERROR(1003, "用户名或密码错误"),
    USERNAME_EXISTS(1004, "用户名已存在"),
    OLD_PASSWORD_ERROR(1005, "旧密码错误"),

    // 商品相关 2xxx
    PRODUCT_NOT_FOUND(2001, "商品不存在"),
    PRODUCT_BARCODE_EXISTS(2002, "条形码已存在"),

    // 库存相关 3xxx
    INVENTORY_NOT_ENOUGH(3001, "库存不足"),
    INVENTORY_LOCKED(3002, "库存锁定失败，请重试");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
