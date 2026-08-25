package com.window.service;

import com.window.entity.OrderLock;

public interface OrderLockService {
    /**
     * 尝试加锁。成功返回 null，失败返回当前锁信息（含 username）
     */
    OrderLock tryAcquire(String orderType, Integer orderId, Integer adminId, String username);

    /**
     * 释放锁（仅限自己的锁）
     */
    void release(String orderType, Integer orderId, Integer adminId);

    /**
     * 查询当前锁（null 表示未锁定）
     */
    OrderLock check(String orderType, Integer orderId);

    /**
     * 清理超时锁（默认 30 分钟过期）
     */
    int cleanupExpired();
}
