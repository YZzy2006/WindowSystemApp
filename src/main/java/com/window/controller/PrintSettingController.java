package com.window.controller;

import com.window.dto.PrintSettingSaveDto;
import com.window.dto.Result;
import com.window.service.PrintSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/print-setting")
@RequiredArgsConstructor
public class PrintSettingController {

    private final PrintSettingService printSettingService;

    @GetMapping
    public Result get(@RequestParam(defaultValue = "order") String type) {
        var setting = printSettingService.getSetting(type);
        return Result.success(setting == null ? null : setting.getConfig());
    }

    @PutMapping
    public Result save(@RequestBody PrintSettingSaveDto body) {
        if (body == null || body.getConfig() == null) {
            return Result.error(400, "缺少config参数");
        }
        String type = body.getType() != null ? body.getType() : "order";
        printSettingService.saveSetting(type, body.getConfig());
        return Result.success(null);
    }
}
