package com.window.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface OssService {

    /**
     * 上传文件
     * @param file 上传的文件
     * @return 文件访问 URL
     */
    String upload(MultipartFile file) throws IOException;

    /**
     * 删除文件
     * @param objectName 文件路径（如 2026/05/abc.jpg）
     */
    void delete(String objectName);

}
