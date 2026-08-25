package com.window.exception;

/**
 * 订单号已存在（含软删除记录，唯一索引仍占用）。
 * 导入场景下前端会将其归类为"跳过"，而不是失败。
 */
public class OrderNoExistsException extends RuntimeException {

    public OrderNoExistsException(String message) {
        super(message);
    }
}
