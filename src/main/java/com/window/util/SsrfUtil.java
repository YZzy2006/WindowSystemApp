package com.window.util;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * SSRF 防护工具 — 阻止 AI Base URL 指向内网/云元数据端点
 */
public final class SsrfUtil {

    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "localhost", "127.0.0.1", "0.0.0.0", "::1",
            "169.254.169.254", "metadata.google.internal",
            "instance-data", "metadata"
    );

    private SsrfUtil() {}

    public static void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL 不能为空");
        }
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("URL 格式不合法: " + url);
        }

        String scheme = uri.getScheme();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new IllegalArgumentException("URL 必须使用 http/https 协议: " + url);
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL 缺少主机名: " + url);
        }

        // Check blocked hostnames
        String lowerHost = host.toLowerCase();
        if (BLOCKED_HOSTS.contains(lowerHost)) {
            throw new IllegalArgumentException("不允许访问内网地址: " + host);
        }

        // Check private IP ranges
        try {
            InetAddress addr = InetAddress.getByName(host);
            if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()
                    || addr.isSiteLocalAddress() || addr.isAnyLocalAddress()) {
                throw new IllegalArgumentException("不允许访问内网地址: " + host + " (" + addr.getHostAddress() + ")");
            }
            // Check 169.254.x.x (cloud metadata)
            byte[] ip = addr.getAddress();
            if (ip.length == 4 && (ip[0] & 0xFF) == 169 && (ip[1] & 0xFF) == 254) {
                throw new IllegalArgumentException("不允许访问云元数据端点: " + host);
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("无法解析主机名: " + host);
        }
    }
}
