package com.window.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.window.common.JwtUtil;
import com.window.entity.Admin;
import com.window.mapper.AdminMapper;
import com.window.service.AdminService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TokenInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TokenInterceptor.class);
    private final JwtUtil jwtUtil;
    private final AdminService adminService;
    private final AdminMapper adminMapper;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String authHeader = request.getHeader("Authorization");
        String token = extractToken(authHeader);
        if (token == null || token.isEmpty()) {
            log.warn("请求被拦截 — Authorization header 缺失: {} {}", request.getMethod(), request.getRequestURI());
            write401(response);
            return false;
        }
        Claims claims = jwtUtil.validateAndGetClaims(token);
        if (claims == null) {
            log.warn("请求被拦截 — JWT 无效或已过期: {} {}", request.getMethod(), request.getRequestURI());
            write401(response);
            return false;
        }
        String adminId = claims.getSubject();
        String tokenId = claims.get("tokenId", String.class);

        // 验证 tokenId：如果 DB 中的 tokenId 不匹配，说明同账号在其他设备登录，踢掉旧会话
        if (!adminService.validateTokenId(Integer.valueOf(adminId), tokenId)) {
            log.warn("请求被拦截 — 会话已被同账号新登录覆盖: adminId={}", adminId);
            write401(response, "会话已失效，请重新登录");
            return false;
        }

        // 冻结检查 + 账号存在性检查
        Admin admin = adminMapper.selectById(Integer.valueOf(adminId));
        if (admin == null) {
            log.warn("请求被拦截 — 管理员账号已删除: adminId={}", adminId);
            write401(response, "账号不存在，请重新登录");
            return false;
        }
        if (admin.getIsFrozen() != null && admin.getIsFrozen() == 1) {
            log.warn("请求被拦截 — 账号已被冻结: adminId={}", adminId);
            write403(response, "账号已被冻结，请联系管理员");
            return false;
        }

        request.setAttribute("adminId", adminId);
        request.setAttribute("adminRole", admin.getRole() != null ? admin.getRole() : "admin");
        request.setAttribute("adminUsername", admin.getUsername());
        return true;
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        return authHeader.substring(7).trim();
    }

    private void write401(HttpServletResponse response) throws Exception {
        write401(response, "未登录或已过期");
    }
    private void write401(HttpServletResponse response, String msg) throws Exception {
        writeError(response, 401, msg);
    }
    private void write403(HttpServletResponse response, String msg) throws Exception {
        writeError(response, 403, msg);
    }
    private void writeError(HttpServletResponse response, int code, String msg) throws Exception {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("msg", msg);
        body.put("data", null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

}
