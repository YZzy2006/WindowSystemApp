package com.window.controller;

import com.window.dto.Result;
import com.window.service.SysConfigService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
public class SysConfigController {
    private final SysConfigService sysConfigService;

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "aes_encryption_key"
    );

    /** 已废弃的配置 key，不再返回给前端 */
    private static final Set<String> DEPRECATED_KEYS = Set.of(
            "ai_api_key", "ai_base_url", "ai_model", "ai_max_tokens",
            "ai_temperature", "ai_api_format", "ai_auth_type", "ai_enabled",
            "oss_endpoint", "oss_bucket", "oss_access_key", "oss_secret_key", "oss_domain"
    );

    @GetMapping
    public Result getAll(HttpServletRequest request) {
        Map<String, String> all = sysConfigService.getAll();
        // 所有用户均过滤废弃配置
        all = all.entrySet().stream()
                .filter(e -> !DEPRECATED_KEYS.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        boolean isSuper = "super_admin".equals(request.getAttribute("adminRole"));
        if (!isSuper) {
            all = all.entrySet().stream()
                    .filter(e -> !SENSITIVE_KEYS.contains(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return Result.success(all);
    }

    @GetMapping("/{key}")
    public Result get(@PathVariable String key) {
        if (DEPRECATED_KEYS.contains(key)) {
            return Result.error(404, "配置项不存在");
        }
        String value = sysConfigService.get(key);
        if (value == null) return Result.error(404, "配置项不存在");
        return Result.success(value);
    }

    @PutMapping
    public Result set(@RequestBody Map<String, String> body, HttpServletRequest request) {
        boolean isSuper = "super_admin".equals(request.getAttribute("adminRole"));
        if (body.containsKey("settings")) {
            String key = body.get("key");
            String value = body.get("value");
            String remark = body.get("remark");
            if (key == null || key.isEmpty()) {
                return Result.error(400, "配置键不能为空");
            }
            if (DEPRECATED_KEYS.contains(key)) {
                return Result.error(400, "该配置已废弃");
            }
            if (!isSuper && SENSITIVE_KEYS.contains(key)) {
                return Result.error(403, "仅超级管理员可修改此配置");
            }
            sysConfigService.set(key, value, remark);
        } else {
            for (Map.Entry<String, String> entry : body.entrySet()) {
                if (DEPRECATED_KEYS.contains(entry.getKey())) {
                    continue;
                }
                if (!isSuper && SENSITIVE_KEYS.contains(entry.getKey())) {
                    return Result.error(403, "仅超级管理员可修改此配置");
                }
                sysConfigService.set(entry.getKey(), entry.getValue(), null);
            }
        }
        return Result.success("保存成功");
    }

    @PostMapping("/reset-sequences")
    public Result resetSequences() {
        sysConfigService.resetSequences();
        return Result.success("订单序号已重置");
    }
}
