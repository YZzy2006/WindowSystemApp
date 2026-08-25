package com.window.controller;

import com.window.entity.OrderLock;
import com.window.service.OrderLockService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/order-lock")
@RequiredArgsConstructor
public class OrderLockController {
    private final OrderLockService orderLockService;

    /**
     * 尝试加锁（编辑前调用）
     * 返回 { locked: false } 表示加锁成功
     * 返回 { locked: true, username: "xxx", lockTime: "..." } 表示被他人锁定
     */
    @PostMapping("/acquire")
    public ResponseEntity<?> acquire(@RequestParam String orderType,
                                     @RequestParam Integer orderId,
                                     HttpServletRequest request) {
        Integer adminId = Integer.valueOf(request.getAttribute("adminId").toString());
        String username = request.getAttribute("adminUsername").toString();
        OrderLock lock = orderLockService.tryAcquire(orderType, orderId, adminId, username);
        Map<String, Object> result = new HashMap<>();
        if (lock == null) {
            result.put("locked", false);
        } else {
            result.put("locked", true);
            result.put("username", lock.getUsername());
            result.put("lockTime", lock.getLockTime());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 释放锁（保存/关闭时调用）
     */
    @PostMapping("/release")
    public ResponseEntity<?> release(@RequestParam String orderType,
                                     @RequestParam Integer orderId,
                                     HttpServletRequest request) {
        Integer adminId = Integer.valueOf(request.getAttribute("adminId").toString());
        orderLockService.release(orderType, orderId, adminId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * 查询当前锁状态
     */
    @GetMapping("/check")
    public ResponseEntity<?> check(@RequestParam String orderType,
                                   @RequestParam Integer orderId) {
        OrderLock lock = orderLockService.check(orderType, orderId);
        Map<String, Object> result = new HashMap<>();
        if (lock == null) {
            result.put("locked", false);
        } else {
            result.put("locked", true);
            result.put("username", lock.getUsername());
            result.put("lockTime", lock.getLockTime());
        }
        return ResponseEntity.ok(result);
    }
}
