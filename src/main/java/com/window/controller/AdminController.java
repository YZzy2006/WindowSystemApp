package com.window.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import com.window.common.JwtUtil;
import io.jsonwebtoken.Claims;
import com.window.dto.EnquiryUpdateDto;
import com.window.dto.LoginDto;
import com.window.dto.Result;
import com.window.entity.*;
import com.window.mapper.LoginLogMapper;
import com.window.service.*;
import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final CategoryService categoryService;
    private final ProductService productService;
    private final CaseService caseService;
    private final EnquiryService enquiryService;
    private final OssService ossService;
    private final JwtUtil jwtUtil;
    private final SiteConfigService siteConfigService;
    private final LoginLogMapper loginLogMapper;
    private final DataSource dataSource;
    private final SessionEventService sessionEventService;
    private static final long startTime = System.currentTimeMillis();

    /** 探活 */
    @GetMapping("/ping")
    public Result ping() {
        return Result.success("admin pong");
    }

    /** 会话事件 SSE 推送（登录后建立，用于实时踢出通知） */
    @GetMapping("/session/events")
    public SseEmitter sessionEvents(@RequestParam(required = false) String token,
                                    jakarta.servlet.http.HttpServletRequest request) {
        // EventSource 不支持自定义 header，优先从 query param 读 token
        String adminId;
        if (token != null && !token.isBlank()) {
            io.jsonwebtoken.Claims claims = jwtUtil.validateAndGetClaims(token);
            if (claims == null) {
                return new SseEmitter(1000L); // 返回短超时 emitter，前端自行处理
            }
            adminId = claims.getSubject();
        } else {
            adminId = (String) request.getAttribute("adminId");
        }
        if (adminId == null) {
            return new SseEmitter(1000L);
        }
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L); // 5分钟超时，前端会自动重连
        sessionEventService.register(Integer.valueOf(adminId), emitter);
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (Exception ignored) {}
        return emitter;
    }

    /** 管理员登录，返回 JWT token */
    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginDto dto, HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        String logIp = getClientIp(request); // 日志记录真实客户端 IP
        Admin admin;
        try {
            admin = adminService.login(dto.getUsername(), dto.getPassword());
        } catch (IllegalStateException e) {
            // 冻结账号也返回通用错误信息，防止用户名枚举
            saveLoginLog(null, dto.getUsername(), logIp, ua, 0, e.getMessage());
            return Result.error(401, "账号或密码错误");
        }
        if (admin == null) {
            saveLoginLog(null, dto.getUsername(), logIp, ua, 0, "账号或密码错误");
            return Result.error(401, "账号或密码错误");
        }
        saveLoginLog(admin.getId(), admin.getUsername(), logIp, ua, 1, null);
        // 踢掉同账号的旧会话（实时推送）
        sessionEventService.sendKickEvent(admin.getId(), "您的账号在其他设备登录，当前会话已失效");
        String token = jwtUtil.createToken(admin.getId().toString(), admin.getTokenId());
        return Result.success(java.util.Map.of("token", token, "role", admin.getRole() != null ? admin.getRole() : "admin"));
    }

    /** 忘记密码 — 通过超级管理员密码验证后重置任意账号密码 */
    @PostMapping("/forgot-password")
    public Result forgotPassword(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String username = body.get("username");
        String superAdminPassword = body.get("superAdminPassword");
        String newPassword = body.get("newPassword");
        if (username == null || username.trim().isEmpty()
                || superAdminPassword == null || superAdminPassword.isEmpty()
                || newPassword == null || newPassword.isEmpty()) {
            return Result.error(400, "所有字段必填");
        }
        try {
            adminService.resetPasswordBySuperAdmin(username.trim(), superAdminPassword, newPassword);
            log.info("忘记密码: 用户 [{}] 通过超级管理员验证重置了密码", username.trim());
            return Result.success("密码重置成功，请用新密码登录");
        } catch (IllegalArgumentException e) {
            // 统一错误信息，防止用户名枚举（不区分"超管密码错"和"目标用户不存在"）
            return Result.error(400, "用户名或超级管理员密码错误");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            // 取第一个 IP（即真实客户端 IP），防止伪造多层代理
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void saveLoginLog(Integer adminId, String username, String ip, String ua, int status, String reason) {
        try {
            LoginLog log = new LoginLog();
            log.setAdminId(adminId != null ? adminId : 0);
            log.setUsername(username);
            log.setLoginIp(ip);
            log.setUserAgent(ua != null && ua.length() > 500 ? ua.substring(0, 500) : ua);
            log.setStatus(status);
            log.setFailReason(reason);
            log.setLoginTime(LocalDateTime.now());
            loginLogMapper.insert(log);
        } catch (Exception e) {
            log.warn("记录登录日志失败: {}", e.getMessage());
        }
    }

    /** 获取最近登录记录 */
    @GetMapping("/login-logs")
    public Result loginLogs() {
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(LoginLog::getLoginTime).last("LIMIT 50");
        return Result.success(loginLogMapper.selectList(wrapper));
    }

    /** 更新登录记录备注 */
    @PatchMapping("/login-logs/{id}")
    public Result updateLoginLogRemark(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        LoginLog log = new LoginLog();
        log.setId(id);
        log.setRemark(body.get("remark"));
        loginLogMapper.updateById(log);
        return Result.success("已更新");
    }

    /** 系统信息 */
    @GetMapping("/system-info")
    public Result systemInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        Runtime rt = Runtime.getRuntime();
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("osName", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        info.put("cpuCores", rt.availableProcessors());
        info.put("totalMemory", formatBytes(rt.totalMemory()));
        info.put("freeMemory", formatBytes(rt.freeMemory()));
        info.put("usedMemory", formatBytes(rt.totalMemory() - rt.freeMemory()));
        info.put("maxMemory", formatBytes(rt.maxMemory()));
        long uptimeMs = System.currentTimeMillis() - startTime;
        info.put("uptime", formatUptime(uptimeMs));
        info.put("serverTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        // 数据统计
        try { info.put("productCount", productService.listAll().size()); } catch (Exception e) { info.put("productCount", 0); }
        try { info.put("categoryCount", categoryService.list().size()); } catch (Exception e) { info.put("categoryCount", 0); }
        try { info.put("caseCount", caseService.listAll().size()); } catch (Exception e) { info.put("caseCount", 0); }
        try { info.put("enquiryCount", enquiryService.listAll().size()); } catch (Exception e) { info.put("enquiryCount", 0); }
        // 数据库信息
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            String dbProduct = conn.getMetaData().getDatabaseProductName();
            boolean isH2 = dbProduct != null && dbProduct.toUpperCase().contains("H2");
            try (ResultSet rs = stmt.executeQuery("SELECT VERSION()")) {
                if (rs.next()) info.put("dbVersion", (isH2 ? "H2 " : "MySQL ") + rs.getString(1));
            }
            if (isH2) {
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = SCHEMA()")) {
                    if (rs.next()) info.put("tableCount", rs.getInt(1));
                }
            } else {
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT ROUND(SUM(data_length + index_length) / 1024 / 1024, 1) FROM information_schema.tables WHERE table_schema = DATABASE()")) {
                    if (rs.next()) info.put("dbSize", rs.getDouble(1) + " MB");
                }
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()")) {
                    if (rs.next()) info.put("tableCount", rs.getInt(1));
                }
            }
        } catch (Exception e) {
            info.put("dbVersion", "未知");
            info.put("dbSize", "未知");
            info.put("tableCount", 0);
        }
        return Result.success(info);
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1073741824) return String.format("%.1f GB", bytes / 1073741824.0);
        if (bytes >= 1048576) return String.format("%.0f MB", bytes / 1048576.0);
        return String.format("%.0f KB", bytes / 1024.0);
    }

    private String formatUptime(long ms) {
        long s = ms / 1000;
        long d = s / 86400; s %= 86400;
        long h = s / 3600; s %= 3600;
        long m = s / 60; s %= 60;
        if (d > 0) return d + "天" + h + "小时" + m + "分钟";
        if (h > 0) return h + "小时" + m + "分钟";
        return m + "分钟" + s + "秒";
    }

    /** 修改密码（需旧密码验证） */
    @PutMapping("/password")
    public Result changePassword(@RequestAttribute("adminId") String adminId,
                                  @RequestBody Map<String, String> body) {
        String oldPwd = body.get("oldPassword");
        String newPwd = body.get("newPassword");
        if (oldPwd == null || newPwd == null) {
            return Result.error(400, "原密码和新密码不能为空");
        }
        if (newPwd.length() < 8) {
            return Result.error(400, "新密码至少8位");
        }
        if (!newPwd.matches(".*[a-zA-Z].*") || !newPwd.matches(".*\\d.*")) {
            return Result.error(400, "新密码需包含字母和数字");
        }
        try {
            adminService.changePassword(Integer.valueOf(adminId), oldPwd, newPwd);
            return Result.success("密码修改成功");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /** 登出 — 服务端清 tokenId，使当前 token 立即失效 */
    @PostMapping("/logout")
    public Result logout(@RequestAttribute("adminId") String adminId) {
        adminService.invalidateToken(Integer.valueOf(adminId));
        return Result.success("已登出");
    }

    /** 刷新 token（前端定期调用，保持会话活跃） */
    @PostMapping("/refresh-token")
    public Result refreshToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.error(401, "未登录");
        }
        String oldToken = authHeader.substring(7).trim();
        Claims claims = jwtUtil.validateAndGetClaims(oldToken);
        if (claims == null) {
            return Result.error(401, "token已过期，请重新登录");
        }
        String adminId = claims.getSubject();
        String tokenId = claims.get("tokenId", String.class);
        // 验证 tokenId 是否仍有效
        if (!adminService.validateTokenId(Integer.valueOf(adminId), tokenId)) {
            return Result.error(401, "会话已失效，请重新登录");
        }
        // 签发新 token（tokenId 不变，仅续期）
        String newToken = jwtUtil.createToken(adminId, tokenId);
        return Result.success(newToken);
    }

    /** 文件上传，返回本地访问 URL */
    @PostMapping("/upload")
    public Result upload(@RequestParam("file") MultipartFile file) {
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.error(400, "文件大小不能超过10MB");
        }
        try {
            String url = ossService.upload(file);
            return Result.success(url);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (IOException e) {
            return Result.error(500, "文件上传失败");
        }
    }

    // ==================== 分类管理 ====================

    @GetMapping("/categories")
    public Result categories() {
        return Result.success(categoryService.list());
    }

    @PostMapping("/categories")
    public Result addCategory(@Valid @RequestBody Category category) {
        categoryService.save(category);
        return Result.success("添加成功");
    }

    @PutMapping("/categories/{id}")
    public Result updateCategory(@PathVariable Integer id, @Valid @RequestBody Category category) {
        category.setId(id);
        categoryService.updateById(category);
        return Result.success("更新成功");
    }

    @DeleteMapping("/categories/{id}")
    public Result deleteCategory(@PathVariable Integer id) {
        categoryService.removeById(id);
        return Result.success("删除成功");
    }

    // ==================== 产品管理 ====================

    @GetMapping("/products")
    public Result products() {
        return Result.success(productService.listAll());
    }

    @PostMapping("/products")
    public Result addProduct(@Valid @RequestBody Product product) {
        productService.save(product);
        return Result.success("添加成功");
    }

    @PutMapping("/products/{id}")
    public Result updateProduct(@PathVariable Integer id, @Valid @RequestBody Product product) {
        product.setId(id);
        productService.updateById(product);
        return Result.success("更新成功");
    }

    @DeleteMapping("/products/{id}")
    public Result deleteProduct(@PathVariable Integer id) {
        productService.removeById(id);
        return Result.success("删除成功");
    }

    // ==================== 案例管理 ====================

    @GetMapping("/cases")
    public Result cases() {
        return Result.success(caseService.listAll());
    }

    @PostMapping("/cases")
    public Result addCase(@Valid @RequestBody Case caseInfo) {
        caseService.save(caseInfo);
        return Result.success("添加成功");
    }

    @PutMapping("/cases/{id}")
    public Result updateCase(@PathVariable Integer id, @Valid @RequestBody Case caseInfo) {
        caseInfo.setId(id);
        caseService.update(caseInfo);
        return Result.success("更新成功");
    }

    @DeleteMapping("/cases/{id}")
    public Result deleteCase(@PathVariable Integer id) {
        caseService.removeById(id);
        return Result.success("删除成功");
    }

    // ==================== 询价管理 ====================

    @GetMapping("/enquiries")
    public Result enquiries() {
        return Result.success(enquiryService.listAll());
    }

    @PatchMapping("/enquiries/{id}")
    public Result updateEnquiry(@PathVariable Integer id, @RequestBody EnquiryUpdateDto body) {
        if (body.getIsRead() != null) {
            enquiryService.markRead(id);
        }
        if (body.getIsMeasured() != null) {
            enquiryService.markMeasured(id, body.getIsMeasured());
        }
        if (body.getIsStarred() != null) {
            enquiryService.markStarred(id, body.getIsStarred());
        }
        if (body.getIsCompleted() != null) {
            enquiryService.markCompleted(id, body.getIsCompleted());
        }
        if (body.getRemark() != null) {
            enquiryService.updateRemark(id, body.getRemark());
        }
        return Result.success("已更新");
    }

    @PostMapping("/enquiries/read-all")
    public Result markAllRead() {
        enquiryService.markAllRead();
        return Result.success("已全部标记为已读");
    }

    @DeleteMapping("/enquiries/{id}")
    public Result deleteEnquiry(@PathVariable Integer id) {
        enquiryService.removeById(id);
        return Result.success("删除成功");
    }

    // ==================== 站点配置 ====================

    @GetMapping("/site-config")
    public Result siteConfig() {
        return Result.success(siteConfigService.get().getConfigJson());
    }

    @PutMapping("/site-config")
    public Result updateSiteConfig(@RequestBody Map<String, String> body) {
        String json = body.get("configJson");
        if (json == null || json.isBlank()) {
            return Result.error(400, "配置内容不能为空");
        }
        siteConfigService.saveOrUpdate(json);
        return Result.success("站点配置已更新");
    }

}
