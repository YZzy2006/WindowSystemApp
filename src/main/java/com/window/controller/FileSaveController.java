package com.window.controller;

import com.window.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class FileSaveController {

    private static final String EXPORT_DIR_NAME = "导入导出";

    @PostMapping("/save-file")
    public Result saveFile(@RequestBody Map<String, String> body) {
        String base64Data = body.get("data");
        String filename = body.get("filename");
        if (base64Data == null || base64Data.isBlank() || filename == null || filename.isBlank()) {
            return Result.error(400, "参数错误");
        }

        // Sanitize filename: strip path separators to prevent traversal
        filename = filename.replaceAll("[/\\\\]", "");
        if (filename.contains("..") || filename.length() > 255) {
            return Result.error(400, "非法文件名");
        }
        // Reject oversized uploads (base64 ~33% overhead, 70MB base64 ≈ 50MB raw)
        if (base64Data.length() > 70_000_000) {
            return Result.error(400, "文件过大，不支持超过50MB的文件");
        }

        try {
            byte[] fileBytes = Base64.getDecoder().decode(base64Data);

            File exportDir = findExportDir();
            if (exportDir == null) {
                return Result.error(500, "无法创建导出目录");
            }

            // Deduplicate: file.xlsx → file(1).xlsx → file(2).xlsx ...
            File target = new File(exportDir, filename);
            if (target.exists()) {
                String name = filename;
                String ext = "";
                int dot = filename.lastIndexOf('.');
                if (dot > 0) {
                    name = filename.substring(0, dot);
                    ext = filename.substring(dot);
                }
                for (int i = 1; i < 1000; i++) {
                    target = new File(exportDir, name + "(" + i + ")" + ext);
                    if (!target.exists()) break;
                }
            }

            try (FileOutputStream fos = new FileOutputStream(target)) {
                fos.write(fileBytes);
            }

            log.info("File saved: {}", target.getAbsolutePath());
            return Result.success(target.getAbsolutePath());
        } catch (Exception e) {
            log.error("Save file error", e);
            return Result.error(500, "保存失败");
        }
    }

    /**
     * 查找导出目录：优先安装目录/导入导出/（app.install-dir 由 Rust 启动时传入）。
     * 安装目录不存在或不可写时（如 Program Files）依次兜底 %APPDATA%、user.dir。
     */
    private File findExportDir() {
        // 1. 安装目录/导入导出/
        String installDir = System.getProperty("app.install-dir");
        if (installDir != null && !installDir.isBlank()) {
            File exportDir = new File(installDir, EXPORT_DIR_NAME);
            if (isUsableDir(exportDir)) {
                log.info("Export dir (install): {}", exportDir.getAbsolutePath());
                return exportDir;
            }
            log.warn("安装目录不可写，尝试其他位置: {}", installDir);
        }

        // 2. 兜底：%APPDATA%/顺居门业/导入导出/
        String appData = System.getenv("APPDATA");
        if (appData != null) {
            File exportDir = new File(appData, "顺居门业/" + EXPORT_DIR_NAME);
            if (isUsableDir(exportDir)) {
                log.info("Export dir (AppData): {}", exportDir.getAbsolutePath());
                return exportDir;
            }
        }

        // 3. 兜底：user.dir/导入导出/
        File fallback = new File(System.getProperty("user.dir"), EXPORT_DIR_NAME);
        if (isUsableDir(fallback)) {
            log.info("Export dir (user.dir): {}", fallback.getAbsolutePath());
            return fallback;
        }

        log.error("Cannot create export directory");
        return null;
    }

    /** 目录已存在且可写，或能成功创建且可写 */
    private boolean isUsableDir(File dir) {
        if (dir.exists()) {
            return dir.isDirectory() && dir.canWrite();
        }
        return dir.mkdirs() && dir.canWrite();
    }

}
