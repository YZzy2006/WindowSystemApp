// com/window/config/GlobalExceptionHandler.java
package com.window.config;

import com.window.dto.Result;
import com.window.exception.OrderNoExistsException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 静态资源 404 — 直接返回 404，不尝试 JSON 转换 */
    @ExceptionHandler(NoResourceFoundException.class)
    public void handleNoResource(NoResourceFoundException e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.NOT_FOUND.value());
    }

    /** 参数校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        return Result.error(400, "参数校验失败：" + msg);
    }

    /** 业务逻辑参数错误 */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result handleIllegalArgument(IllegalArgumentException e) {
        return Result.error(400, e.getMessage());
    }

    /** 安全异常（公式非法字符等） */
    @ExceptionHandler(SecurityException.class)
    public Result handleSecurity(SecurityException e) {
        return Result.error(400, e.getMessage());
    }

    /** JSON 解析失败 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result handleJsonError(HttpMessageNotReadableException e) {
        return Result.error(400, "请求数据格式错误");
    }

    /** 业务逻辑错误 */
    @ExceptionHandler(IllegalStateException.class)
    public Result handleIllegalState(IllegalStateException e) {
        return Result.error(400, e.getMessage());
    }

    /** 订单号已存在（唯一索引预检），导入时前端归类为"跳过" */
    @ExceptionHandler(OrderNoExistsException.class)
    public Result handleOrderNoExists(OrderNoExistsException e) {
        return Result.error(400, e.getMessage());
    }

    /** 数据库约束冲突（外键引用、唯一键冲突等）。兼容 MySQL 与 H2 两种报错文案 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result handleDataIntegrity(DataIntegrityViolationException e) {
        String msg = e.getMessage();
        if (msg != null && (msg.contains("foreign key") || msg.contains("Referential integrity constraint violation"))) {
            return Result.error(400, "数据被其他记录引用，无法删除");
        }
        if (msg != null && (msg.contains("Duplicate")
                || msg.contains("Unique index or primary key violation")
                || msg.contains("already exists"))) {
            return Result.error(400, "数据重复，请检查后重试");
        }
        return Result.error(400, "数据约束冲突，操作无法完成");
    }

    /** 兜底异常 — 排除已被处理的 NoResourceFoundException */
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e, HttpServletResponse response) {
        if (e instanceof NoResourceFoundException) {
            return null;
        }
        // SSE 响应已提交（Content-Type 为 text/event-stream），无法写入 JSON，跳过
        if (response.isCommitted() || "text/event-stream".equals(response.getContentType())) {
            log.debug("SSE 响应已提交，跳过全局异常处理: {}", e.getMessage());
            return null;
        }
        log.error("服务器内部错误", e);
        return Result.error(500, "服务器内部错误");
    }

}
