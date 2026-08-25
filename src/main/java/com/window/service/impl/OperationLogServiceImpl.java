package com.window.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.entity.OperationLog;
import com.window.mapper.OperationLogMapper;
import com.window.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.window.common.KeywordUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public IPage<OperationLog> list(Page<OperationLog> page, String keyword, String module, String action, String date, String startDate, String endDate) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            final String escaped = KeywordUtil.escapeLike(keyword);
            wrapper.and(w -> w.like(OperationLog::getUsername, escaped)
                    .or().like(OperationLog::getTarget, escaped)
                    .or().like(OperationLog::getModule, escaped));
        }
        if (StringUtils.hasText(module)) {
            wrapper.eq(OperationLog::getModule, module);
        }
        if (StringUtils.hasText(action)) {
            wrapper.eq(OperationLog::getAction, action);
        }
        if (StringUtils.hasText(date)) {
            LocalDate ld = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            wrapper.ge(OperationLog::getCreateTime, ld.atStartOfDay());
            wrapper.le(OperationLog::getCreateTime, ld.atTime(LocalTime.MAX));
        }
        if (StringUtils.hasText(startDate)) {
            LocalDate sd = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE);
            wrapper.ge(OperationLog::getCreateTime, sd.atStartOfDay());
        }
        if (StringUtils.hasText(endDate)) {
            LocalDate ed = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE);
            wrapper.le(OperationLog::getCreateTime, ed.atTime(LocalTime.MAX));
        }
        wrapper.orderByDesc(OperationLog::getCreateTime);
        return operationLogMapper.selectPage(page, wrapper);
    }
}
