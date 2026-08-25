package com.window.service;

import com.window.dto.ImportResult;

import java.util.List;
import java.util.Map;

public interface ImportService {

    /**
     * 只读预览：校验 + 查重，不写库。结果状态：new / exists / invalid
     */
    ImportResult preview(String module, List<Map<String, Object>> records);

    /**
     * 提交导入：逐条独立事务，后端权威判定。结果状态：created / skipped / failed
     */
    ImportResult commit(String module, List<Map<String, Object>> records);
}
