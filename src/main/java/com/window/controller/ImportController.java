package com.window.controller;

import com.window.dto.ImportRequest;
import com.window.dto.ImportResult;
import com.window.dto.Result;
import com.window.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    /** 预览：校验 + 查重，不写库 */
    @PostMapping("/preview")
    public Result preview(@RequestBody ImportRequest req) {
        ImportResult result = importService.preview(req.getModule(), req.getRecords());
        return Result.success(result);
    }

    /** 提交：逐条独立事务，后端权威判定 created / skipped / failed */
    @PostMapping("/commit")
    public Result commit(@RequestBody ImportRequest req) {
        ImportResult result = importService.commit(req.getModule(), req.getRecords());
        return Result.success(result);
    }
}
