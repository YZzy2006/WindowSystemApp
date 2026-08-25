package com.window.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface BackupService {
    String createBackup();
    List<Map<String, Object>> listBackups();
    File getBackupFile(String filename);
    void deleteBackup(String filename);
    /** 恢复数据，返回恢复前自动创建的安全备份文件名（可为 null） */
    String restoreBackup(MultipartFile file);
}
