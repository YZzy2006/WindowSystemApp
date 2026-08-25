package com.window.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量导入结果。preview 与 commit 共用：
 * preview  → created=新增，skipped=已存在(将跳过)，invalid=校验不合格
 * commit   → created=已创建，skipped=已存在已跳过，failed=失败
 */
@Data
public class ImportResult {
    private int total;
    private int created;
    private int skipped;
    private int failed;
    private int invalid;
    private List<RecordResult> results = new ArrayList<>();

    @Data
    public static class RecordResult {
        private Integer row;    // 1-based 输入序号
        private String key;     // orderNo / code / name
        private String status;  // preview: new|exists|invalid；commit: created|skipped|failed
        private String message;

        public RecordResult() {}

        public RecordResult(Integer row, String key, String status, String message) {
            this.row = row;
            this.key = key;
            this.status = status;
            this.message = message;
        }
    }
}
