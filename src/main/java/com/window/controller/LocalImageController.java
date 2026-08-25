package com.window.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class LocalImageController {

    @Value("${user.dir}")
    private String userDir;

    @GetMapping("/api/images/**")
    public void serveImage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String uri = request.getRequestURI();
        String relativePath = uri.substring("/api/images/".length());

        Path filePath = Paths.get(userDir, "data", "images", relativePath).normalize();
        Path imagesRoot = Paths.get(userDir, "data", "images").normalize();

        // 安全检查：防止路径穿越
        if (!filePath.startsWith(imagesRoot)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String contentType = Files.probeContentType(filePath);
        if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

        response.setContentType(contentType);
        response.setHeader("Cache-Control", "max-age=31536000, public");
        Files.copy(filePath, response.getOutputStream());
        response.getOutputStream().flush();
    }
}
