package com.window.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.window.entity.OrderLock;
import com.window.mapper.OrderLockMapper;
import com.window.service.OrderLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderLockServiceImpl implements OrderLockService {
    private final OrderLockMapper orderLockMapper;
    private static final int LOCK_TIMEOUT_MINUTES = 30;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderLock tryAcquire(String orderType, Integer orderId, Integer adminId, String username) {
        // 先清理超时锁
        cleanupExpired();

        LambdaQueryWrapper<OrderLock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderLock::getOrderType, orderType)
               .eq(OrderLock::getOrderId, orderId);
        OrderLock existing = orderLockMapper.selectOne(wrapper);

        if (existing != null) {
            // 自己的锁 → 续期
            if (existing.getAdminId().equals(adminId)) {
                existing.setLockTime(LocalDateTime.now());
                orderLockMapper.updateById(existing);
                return null;
            }
            // 别人的锁 → 返回锁信息
            return existing;
        }

        // 无锁 → 加锁
        OrderLock lock = new OrderLock();
        lock.setOrderType(orderType);
        lock.setOrderId(orderId);
        lock.setAdminId(adminId);
        lock.setUsername(username);
        lock.setLockTime(LocalDateTime.now());
        try {
            orderLockMapper.insert(lock);
            return null;
        } catch (Exception e) {
            // 唯一约束冲突 → 并发加锁，返回已存在的锁
            LambdaQueryWrapper<OrderLock> retry = new LambdaQueryWrapper<>();
            retry.eq(OrderLock::getOrderType, orderType)
                 .eq(OrderLock::getOrderId, orderId);
            return orderLockMapper.selectOne(retry);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void release(String orderType, Integer orderId, Integer adminId) {
        LambdaQueryWrapper<OrderLock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderLock::getOrderType, orderType)
               .eq(OrderLock::getOrderId, orderId)
               .eq(OrderLock::getAdminId, adminId);
        orderLockMapper.delete(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderLock check(String orderType, Integer orderId) {
        cleanupExpired();
        LambdaQueryWrapper<OrderLock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderLock::getOrderType, orderType)
               .eq(OrderLock::getOrderId, orderId);
        return orderLockMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpired() {
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(LOCK_TIMEOUT_MINUTES);
        LambdaQueryWrapper<OrderLock> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(OrderLock::getLockTime, expireTime);
        return orderLockMapper.delete(wrapper);
    }
}
