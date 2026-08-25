package com.window.service.impl;

import com.window.service.BackupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class BackupServiceImpl implements BackupService {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${app.backup.dir:backups}")
    private String backupDir;

    @Value("${app.backup.max-count:10}")
    private int maxBackupCount;

    private static final DateTimeFormatter FILE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final long MAX_BACKUP_SIZE = 50L * 1024 * 1024; // 50MB
    private final AtomicBoolean operationInProgress = new AtomicBoolean(false);

    /**
     * Build H2 JDBC URL from the configured datasource URL.
     * Strips any query params that don't work with direct JDBC connections.
     */
    private String getH2JdbcUrl() {
        // jdbc:h2:file:./data/window_db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE
        // For SCRIPT/RUNSCRIPT, use the same URL but may need AUTO_SERVER=FALSE if locked
        return datasourceUrl;
    }

    @Override
    public String createBackup() {
        if (!operationInProgress.compareAndSet(false, true)) {
            throw new RuntimeException("备份/恢复操作正在进行中，请稍后再试");
        }
        try {
            return doCreateBackup();
        } finally {
            operationInProgress.set(false);
        }
    }

    /** 实际执行 H2 SCRIPT 导出（不校验互斥锁，供 createBackup / restoreBackup 内部复用） */
    private String doCreateBackup() {
        try {
            Path backupPath = Path.of(backupDir);
            Files.createDirectories(backupPath);

            String timestamp = LocalDateTime.now().format(FILE_FMT);
            String filename = "window_db_backup_" + timestamp + ".sql";
            Path outputFile = backupPath.resolve(filename);

            // Use H2's SCRIPT command to export database as SQL
            try (Connection conn = DriverManager.getConnection(getH2JdbcUrl(), dbUsername, dbPassword);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SCRIPT")) {

                try (OutputStream os = Files.newOutputStream(outputFile);
                     PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"))) {
                    while (rs.next()) {
                        writer.println(rs.getString(1));
                    }
                }
            }

            // 自动清理超出数量限制的旧备份
            cleanupOldBackups(backupPath);

            log.info("数据库备份成功: {}", filename);
            return filename;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("备份失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Map<String, Object>> listBackups() {
        Path backupPath = Path.of(backupDir);
        if (!Files.exists(backupPath)) return Collections.emptyList();

        try (Stream<Path> files = Files.list(backupPath)) {
            return files
                    .filter(p -> p.toString().endsWith(".sql"))
                    .sorted(Comparator.comparing(p -> {
                        try { return Files.getLastModifiedTime(p); }
                        catch (IOException e) { return null; }
                    }, Comparator.reverseOrder()))
                    .map(p -> {
                        Map<String, Object> info = new LinkedHashMap<>();
                        info.put("filename", p.getFileName().toString());
                        try {
                            info.put("size", Files.size(p));
                            info.put("lastModified", Files.getLastModifiedTime(p).toMillis());
                        } catch (IOException e) {
                            info.put("size", 0);
                            info.put("lastModified", 0);
                        }
                        return info;
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("列出备份文件失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public File getBackupFile(String filename) {
        validateFilename(filename);
        File file = Path.of(backupDir, filename).toFile();
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("备份文件不存在");
        }
        return file;
    }

    @Override
    public void deleteBackup(String filename) {
        validateFilename(filename);
        try {
            Path file = Path.of(backupDir, filename);
            Files.deleteIfExists(file);
            log.info("已删除备份文件: {}", filename);
        } catch (IOException e) {
            throw new RuntimeException("删除备份文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String restoreBackup(MultipartFile file) {
        if (!operationInProgress.compareAndSet(false, true)) {
            throw new RuntimeException("备份/恢复操作正在进行中，请稍后再试");
        }
        try {
            return doRestore(file);
        } finally {
            operationInProgress.set(false);
        }
    }

    /**
     * 恢复数据。先自动备份当前库（安全备份，恢复失败/中断时可回滚），再 DROP ALL OBJECTS + RUNSCRIPT。
     * @return 恢复前自动创建的安全备份文件名（创建失败时为 null）
     */
    private String doRestore(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件为空");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.endsWith(".sql")) {
            throw new IllegalArgumentException("只支持 .sql 文件");
        }
        if (file.getSize() > MAX_BACKUP_SIZE) {
            throw new IllegalArgumentException("文件大小超过 50MB 限制");
        }

        // ── 数据安全：恢复前先自动备份当前库 ──
        // DROP ALL OBJECTS + RUNSCRIPT 一旦中途失败/中断，当前数据会部分丢失且无法回滚。
        // 先存一份安全备份，保证数据"可替换性"——恢复出问题时可从安全备份找回。
        // 安全备份创建失败时【中止恢复】：绝不能带着"无回滚点"去做破坏性 DROP。
        String safetyBackup;
        try {
            safetyBackup = doCreateBackup();
            log.info("恢复前安全备份已创建: {}", safetyBackup);
        } catch (Exception e) {
            log.error("恢复前创建安全备份失败，已中止恢复（避免无回滚点的破坏性操作）: {}", e.getMessage());
            throw new RuntimeException("恢复已中止：恢复前自动备份创建失败（" + e.getMessage()
                    + "）。请检查磁盘空间/权限后重新操作。");
        }

        try {
            // Save uploaded file to temp location for RUNSCRIPT
            Path tempFile = Files.createTempFile("restore_", ".sql");
            try {
                file.transferTo(tempFile.toFile());

                // Use H2's RUNSCRIPT command to restore from SQL file
                // First drop all existing objects, then restore from backup
                try (Connection conn = DriverManager.getConnection(getH2JdbcUrl(), dbUsername, dbPassword);
                     Statement stmt = conn.createStatement()) {
                    stmt.execute("DROP ALL OBJECTS");
                    stmt.execute("RUNSCRIPT FROM '" + tempFile.toAbsolutePath().toString().replace("\\", "/") + "'");
                }

                log.info("数据库恢复成功，文件: {}", originalName);
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (RuntimeException e) {
            log.error("恢复失败，当前数据可能已部分变更。恢复前安全备份: {}", safetyBackup);
            throw e;
        } catch (Exception e) {
            // 把失败原因 + 安全备份位置一起抛给用户（GlobalExceptionHandler 兜底只回"服务器内部错误"，会吞掉这些信息）
            String msg = "恢复失败: " + e.getMessage();
            if (safetyBackup != null) {
                msg += "。当前数据可能已部分变更，恢复前安全备份为: " + safetyBackup;
            }
            log.error("恢复失败，当前数据可能已部分变更。恢复前安全备份: {}", safetyBackup);
            throw new RuntimeException(msg, e);
        }
        return safetyBackup;
    }

    private void cleanupOldBackups(Path backupPath) {
        try (Stream<Path> files = Files.list(backupPath)) {
            List<Path> sorted = files
                    .filter(p -> p.toString().endsWith(".sql"))
                    .sorted(Comparator.comparing(p -> {
                        try { return Files.getLastModifiedTime(p); }
                        catch (IOException e) { return null; }
                    }))
                    .collect(Collectors.toList());
            if (sorted.size() > maxBackupCount) {
                for (int i = 0; i < sorted.size() - maxBackupCount; i++) {
                    try {
                        Files.deleteIfExists(sorted.get(i));
                        log.info("自动清理旧备份: {}", sorted.get(i).getFileName());
                    } catch (IOException e) {
                        log.warn("清理旧备份失败: {}", sorted.get(i).getFileName(), e);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("清理旧备份时出错", e);
        }
    }

    private void validateFilename(String filename) {
        if (filename == null || !filename.matches("^[a-zA-Z0-9_.\\-]+$")) {
            throw new IllegalArgumentException("文件名不合法");
        }
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("文件名包含非法路径字符");
        }
    }

}
