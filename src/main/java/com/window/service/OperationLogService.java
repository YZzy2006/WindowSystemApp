package com.window.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.entity.OperationLog;

public interface OperationLogService {
    IPage<OperationLog> list(Page<OperationLog> page, String keyword, String module, String action, String date, String startDate, String endDate);
}
