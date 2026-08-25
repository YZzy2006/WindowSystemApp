package com.window.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.dto.Result;
import com.window.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String module,
                       @RequestParam(required = false) String action,
                       @RequestParam(required = false) String date,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate) {
        return Result.success(operationLogService.list(new Page<>(page, size), keyword, module, action, date, startDate, endDate));
    }
}
