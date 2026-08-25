package com.window.common;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PublicRateLimiter {

    private final ConcurrentHashMap<String, long[]> attempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 10 * 60 * 1000; // 10分钟窗口
    private volatile long lastCleanup = System.currentTimeMillis();

    /**
     * 检查 IP 是否允许提交询价（10分钟内最多5次）
     */
    public synchronized boolean allowEnquiry(String ip) {
        long now = System.currentTimeMillis();
        if (now - lastCleanup > 10 * 60 * 1000) {
            lastCleanup = now;
            cleanupStale(now);
        }
        return checkAndRecord("enquiry:" + ip, now, MAX_ATTEMPTS);
    }

    /** 原子地检查并记录，消除 TOCTOU 竞态 */
    private boolean checkAndRecord(String key, long now, int maxAttempts) {
        long[] times = attempts.computeIfAbsent(key, k -> new long[maxAttempts]);
        int count = 0;
        for (long t : times) {
            if (t > 0 && now - t < WINDOW_MS) count++;
        }
        if (count >= maxAttempts) return false;
        // 记录本次尝试
        for (int i = times.length - 1; i > 0; i--) times[i] = times[i - 1];
        times[0] = now;
        return true;
    }

    private void cleanupStale(long now) {
        attempts.entrySet().removeIf(entry -> {
            long[] times = entry.getValue();
            for (long t : times) {
                if (t > 0 && now - t < WINDOW_MS) return false;
            }
            return true;
        });
    }
}
