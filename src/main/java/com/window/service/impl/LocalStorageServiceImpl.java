package com.window.service.impl;

import com.window.service.OssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class LocalStorageServiceImpl implements OssService {

    @Value("${user.dir}")
    private String userDir;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final Set<String> ALLOWED_MIME = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    @Override
    public String upload(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }

        String suffix = extractSuffix(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(suffix)) {
            throw new IllegalArgumentException("不支持的文件类型，仅允许 jpg/png/gif/webp");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME.contains(contentType)) {
            throw new IllegalArgumentException("文件内容类型不合法");
        }

        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;
        String relativePath = dir + "/" + fileName;

        Path imagesDir = Paths.get(userDir, "data", "images", dir);
        Files.createDirectories(imagesDir);

        Path target = imagesDir.resolve(fileName);
        file.transferTo(target.toFile());
        log.info("图片保存成功: {}", target);

        return "/api/images/" + relativePath;
    }

    @Override
    public void delete(String objectName) {
        // objectName 可以是完整 URL 或相对路径
        String path = objectName;
        if (path.startsWith("/api/images/")) {
            path = path.substring("/api/images/".length());
        }
        Path file = Paths.get(userDir, "data", "images", path);
        try {
            Files.deleteIfExists(file);
            log.info("图片删除成功: {}", file);
        } catch (IOException e) {
            log.error("图片删除失败: {}", e.getMessage());
        }
    }

    private String extractSuffix(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }
}
