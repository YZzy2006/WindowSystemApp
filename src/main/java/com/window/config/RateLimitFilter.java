package com.window.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int AI_LIMIT = 10;         // 10 次/分钟
    private static final int API_LIMIT = 300;       // 300 次/分钟
    private static final long WINDOW_MS = 60_000;   // 1 分钟窗口
    private static final long CLEANUP_INTERVAL = 300_000; // 5 分钟清理一次

    private final ConcurrentHashMap<String, Window> aiWindows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Window> apiWindows = new ConcurrentHashMap<>();
    private volatile long lastCleanup = System.currentTimeMillis();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String ip = request.getRemoteAddr();

        // AI 接口限流
        if (path.startsWith("/api/admin/ai/")) {
            if (!tryAcquire(aiWindows, ip, AI_LIMIT)) {
                send429(response, "AI 请求过于频繁，请1分钟后再试");
                return;
            }
        }

        // 通用 API 限流
        if (path.startsWith("/api/")) {
            if (!tryAcquire(apiWindows, ip, API_LIMIT)) {
                send429(response, "请求过于频繁，请稍后再试");
                return;
            }
        }

        chain.doFilter(request, response);
        maybeCleanup();
    }

    private boolean tryAcquire(ConcurrentHashMap<String, Window> windows, String ip, int limit) {
        Window window = windows.compute(ip, (key, existing) -> {
            if (existing == null || System.currentTimeMillis() - existing.start > WINDOW_MS) {
                return new Window();
            }
            return existing;
        });
        return window.count.incrementAndGet() <= limit;
    }

    private void send429(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new java.util.HashMap<>() {{
            put("code", 429);
            put("msg", message);
            put("data", null);
        }});
    }

    private void maybeCleanup() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup < CLEANUP_INTERVAL) return;
        lastCleanup = now;
        long cutoff = now - WINDOW_MS;
        aiWindows.entrySet().removeIf(e -> e.getValue().start < cutoff);
        apiWindows.entrySet().removeIf(e -> e.getValue().start < cutoff);
    }

    private static class Window {
        final long start = System.currentTimeMillis();
        final AtomicInteger count = new AtomicInteger(0);
    }
}
