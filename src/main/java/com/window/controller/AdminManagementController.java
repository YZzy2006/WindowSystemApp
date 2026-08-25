package com.window.controller;

import com.window.dto.Result;
import com.window.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
public class AdminManagementController {

    private final AdminService adminService;

    private boolean isSuperAdmin(HttpServletRequest request) {
        return "super_admin".equals(request.getAttribute("adminRole"));
    }

    private String getAdminId(HttpServletRequest request) {
        return (String) request.getAttribute("adminId");
    }

    private String getAdminUsername(HttpServletRequest request) {
        return (String) request.getAttribute("adminUsername");
    }

    @GetMapping
    public Result list(HttpServletRequest request) {
        if (!isSuperAdmin(request)) return Result.error(403, "权限不足");
        return Result.success(adminService.listAllAdmins());
    }

    @PostMapping
    public Result create(@RequestBody Map<String, String> body, HttpServletRequest request) {
        if (!isSuperAdmin(request)) return Result.error(403, "权限不足");
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return Result.error(400, "用户名和密码不能为空");
        }
        if (username.trim().length() > 50) {
            return Result.error(400, "用户名不能超过50个字符");
        }
        try {
            adminService.createAdmin(username.trim(), password);
            log.info("管理员操作: {} 创建了管理员 [{}]", getAdminUsername(request), username.trim());
            return Result.success("创建成功");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/freeze")
    public Result freeze(@PathVariable Integer id, HttpServletRequest request) {
        if (!isSuperAdmin(request)) return Result.error(403, "权限不足");
        try {
            adminService.freezeAdmin(id);
            log.info("管理员操作: {} 冻结了管理员 id={}", getAdminUsername(request), id);
            return Result.success("已冻结");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/unfreeze")
    public Result unfreeze(@PathVariable Integer id, HttpServletRequest request) {
        if (!isSuperAdmin(request)) return Result.error(403, "权限不足");
        try {
            adminService.unfreezeAdmin(id);
            log.info("管理员操作: {} 解冻了管理员 id={}", getAdminUsername(request), id);
            return Result.success("已解冻");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/reset-password")
    public Result resetPassword(@PathVariable Integer id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        if (!isSuperAdmin(request)) return Result.error(403, "权限不足");
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return Result.error(400, "新密码不能为空");
        }
        try {
            adminService.resetPassword(id, newPassword);
            log.info("管理员操作: {} 重置了管理员 id={} 的密码", getAdminUsername(request), id);
            return Result.success("密码已重置");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id, HttpServletRequest request) {
        if (!isSuperAdmin(request)) return Result.error(403, "权限不足");
        try {
            adminService.deleteAdmin(id, Integer.valueOf(getAdminId(request)));
            log.info("管理员操作: {} 删除了管理员 id={}", getAdminUsername(request), id);
            return Result.success("已删除");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }
}
