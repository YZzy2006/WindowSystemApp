package com.window.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 批量导入请求体。records 为原始 JSON 记录（订单含 items），
 * 由后端按 module 反序列化为对应 DTO。
 */
@Data
public class ImportRequest {
    private String module;
    private List<Map<String, Object>> records;
}
