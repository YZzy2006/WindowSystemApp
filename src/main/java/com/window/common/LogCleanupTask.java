package com.window.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.window.entity.LoginLog;
import com.window.entity.OperationLog;
import com.window.mapper.LoginLogMapper;
import com.window.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@Lazy(false)
@RequiredArgsConstructor
public class LogCleanupTask {

    private final LoginLogMapper loginLogMapper;
    private final OperationLogMapper operationLogMapper;

    private static final int RETENTION_DAYS = 90;

    /** 每天凌晨 3 点清理 90 天前的日志 */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);

        LambdaQueryWrapper<LoginLog> loginWrapper = new LambdaQueryWrapper<>();
        loginWrapper.lt(LoginLog::getLoginTime, cutoff);
        int loginDeleted = loginLogMapper.delete(loginWrapper);

        LambdaQueryWrapper<OperationLog> opWrapper = new LambdaQueryWrapper<>();
        opWrapper.lt(OperationLog::getCreateTime, cutoff);
        int opDeleted = operationLogMapper.delete(opWrapper);

        log.info("日志清理完成：删除 {} 条登录日志, {} 条操作日志 (保留 {} 天)",
                loginDeleted, opDeleted, RETENTION_DAYS);
    }
}
