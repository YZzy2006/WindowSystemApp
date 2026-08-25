package com.window.controller;

import com.window.dto.Result;
import com.window.service.BackupService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/admin/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    private boolean isSuperAdmin(HttpServletRequest request) {
        return "super_admin".equals(request.getAttribute("adminRole"));
    }

    @PostMapping("/create")
    public Result create(HttpServletRequest request) {
        if (!isSuperAdmin(request)) return Result.error(403, "仅超级管理员可操作");
        String filename = backupService.createBackup();
        return Result.success(filename);
    }

    @GetMapping("/list")
    public Result list(HttpServletRequest request) {
        if (!isSuperAdmin(request)) return Result.error(403, "仅超级管理员可操作");
        return Result.success(backupService.listBackups());
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<?> download(@PathVariable String filename, HttpServletRequest request) throws Exception {
        if (!isSuperAdmin(request)) {
            return ResponseEntity.status(403).body(Result.error(403, "仅超级管理员可操作"));
        }
        File file = backupService.getBackupFile(filename);

        String encodedName = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .body(new FileSystemResource(file));
    }

    @DeleteMapping("/delete/{filename}")
    public Result delete(@PathVariable String filename, HttpServletRequest request) {
        if (!isSuperAdmin(request)) return Result.error(403, "仅超级管理员可操作");
        backupService.deleteBackup(filename);
        return Result.success("删除成功");
    }

    @PostMapping("/restore")
    public Result restore(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (!isSuperAdmin(request)) return Result.error(403, "仅超级管理员可操作");
        try {
            String safetyBackup = backupService.restoreBackup(file);
            String msg = "数据恢复成功，建议重启服务以确保缓存刷新";
            if (safetyBackup != null) {
                msg += "，恢复前数据已自动备份为: " + safetyBackup;
            }
            return Result.success(msg);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            // 恢复失败：GlobalExceptionHandler 兜底只回"服务器内部错误"，会吞掉
            // 具体失败原因 + 安全备份位置，这里显式透传给用户以便回滚
            return Result.error(500, e.getMessage());
        }
    }
}
