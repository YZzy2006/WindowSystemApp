package com.window.config;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.window.entity.OperationLog;
import com.window.mapper.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static java.util.Map.entry;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");
    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;

    // 恢复操作的实体类型 → 中文标签
    private static final Map<String, String> entityLabelMap = Map.ofEntries(
            entry("category", "分类"), entry("product", "产品"), entry("case", "案例"),
            entry("enquiry", "询价"), entry("customer", "客户"), entry("supplier", "供应商"),
            entry("commodity", "商品"), entry("commodity-category", "商品分类"),
            entry("product-type", "商品细目"), entry("pricing-formula", "计算公式"),
            entry("after-sale", "售后"), entry("sale-order", "销售订单"),
            entry("purchase-order", "采购订单"), entry("sale-return", "销售退货"),
            entry("purchase-return", "采购退货"), entry("stock-in", "入库单"),
            entry("stock-out", "出库单"), entry("payment", "收付款")
    );

    // URI 路径 → 模块名（按长度降序匹配，优先匹配更长的路径）
    private static final String[][] MODULE_RULES = {
            {"/api/admin/sale-orders", "销售订单"},
            {"/api/admin/purchase-orders", "采购订单"},
            {"/api/admin/sale-returns", "销售退货"},
            {"/api/admin/purchase-returns", "采购退货"},
            {"/api/admin/returns/sale", "销售退货"},
            {"/api/admin/returns/purchase", "采购退货"},
            {"/api/admin/stock/in", "入库管理"},
            {"/api/admin/stock/out", "出库管理"},
            {"/api/admin/finance/payments", "收付款"},
            {"/api/admin/commodities/categories", "商品分类"},
            {"/api/admin/customers", "客户管理"},
            {"/api/admin/suppliers", "供应商管理"},
            {"/api/admin/products", "产品管理"},
            {"/api/admin/categories", "分类管理"},
            {"/api/admin/cases", "案例管理"},
            {"/api/admin/enquiries", "询价管理"},
            {"/api/admin/commodities", "商品库存"},
            {"/api/admin/login-logs", "登录日志"},
            {"/api/admin/site-config", "站点设置"},
            {"/api/admin/config", "系统设置"},
            {"/api/admin/accounts", "账号管理"},
            {"/api/admin/restore", "数据恢复"},
            {"/api/admin/password", "账号安全"},
            {"/api/admin/upload", "文件上传"},
            {"/api/admin/logout", "登录"},
            {"/api/admin/login", "登录"},
    };

    // URI 路径 → Mapper bean 名称（用于删除前查询实体名称）
    private static final String[][] MAPPER_RULES = {
            {"/api/admin/sale-orders", "saleOrderMapper"},
            {"/api/admin/purchase-orders", "purchaseOrderMapper"},
            {"/api/admin/returns/sale", "saleReturnMapper"},
            {"/api/admin/returns/purchase", "purchaseReturnMapper"},
            {"/api/admin/stock/in", "stockInMapper"},
            {"/api/admin/stock/out", "stockOutMapper"},
            {"/api/admin/finance/payments", "paymentMapper"},
            {"/api/admin/customers", "customerMapper"},
            {"/api/admin/suppliers", "supplierMapper"},
            {"/api/admin/products", "productMapper"},
            {"/api/admin/categories", "categoryMapper"},
            {"/api/admin/cases", "caseMapper"},
            {"/api/admin/enquiries", "enquiryMapper"},
            {"/api/admin/commodities/categories", "commodityCategoryMapper"},
            {"/api/admin/commodities", "commodityMapper"},
    };

    // 实体中用于显示的名称字段，按优先级
    private static final String[] NAME_FIELDS = {
            "name", "orderNo", "customerName", "supplierName", "partyName",
            "title", "phone", "category"
    };

    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PatchMapping)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        // --- proceed 之前：提取信息 ---
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest req = (attrs != null) ? attrs.getRequest() : null;
        String uri = (req != null) ? req.getRequestURI() : "";

        boolean isAdminApi = uri.startsWith("/api/admin/") && !uri.contains("/ping");

        String pathId = null;
        Object requestBody = null;
        if (isAdminApi) {
            pathId = extractPathParam(joinPoint);
            requestBody = extractRequestBody(joinPoint);
        }

        // 修改/删除前查出实体名称（用于日志展示）
        String entityNameFromDb = null;
        if (isAdminApi && req != null && pathId != null) {
            String method = req.getMethod();
            if ("DELETE".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
                entityNameFromDb = fetchEntityName(uri, pathId);
            }
        }

        // --- 执行业务方法 ---
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long elapsed = System.currentTimeMillis() - start;

        // --- proceed 之后：记录日志 ---
        try {
            if (!isAdminApi || req == null) return result;

            String adminId = (String) req.getAttribute("adminId");
            String adminUsername = (String) req.getAttribute("adminUsername");

            // 登录接口没有经过 TokenInterceptor，从请求体提取用户名
            if ((adminUsername == null || adminUsername.isEmpty()) && uri.contains("/login") && requestBody != null) {
                try {
                    Map<String, Object> bodyMap = objectMapper.convertValue(requestBody, Map.class);
                    Object bodyUsername = bodyMap.get("username");
                    if (bodyUsername != null) adminUsername = bodyUsername.toString();
                } catch (Exception ignored) {}
            }
            String method = req.getMethod();

            auditLog.info("adminId={} {} {} {}ms", adminId != null ? adminId : "?", method, uri, elapsed);

            String module = matchRule(MODULE_RULES, uri);
            if (module == null) return result;

            String action = parseAction(method, uri);
            String target = buildTarget(method, uri, pathId, requestBody, entityNameFromDb, result);
            // 兜底：如果 target 为空且有请求参数，从 query string 提取描述
            if ((target == null || target.isEmpty()) && req.getQueryString() != null) {
                target = buildTargetFromParams(req);
            }

            OperationLog log = new OperationLog();
            log.setAdminId(adminId != null ? Integer.valueOf(adminId) : 0);
            log.setUsername(adminUsername != null ? adminUsername : "");
            log.setModule(module);
            log.setAction(action);
            log.setTarget(target);
            log.setIp(getClientIp(req));
            log.setCreateTime(LocalDateTime.now());

            try {
                operationLogMapper.insert(log);
            } catch (Exception e) {
                auditLog.warn("操作日志写入数据库失败: {}", e.getMessage());
            }
        } catch (Exception e) {
            auditLog.warn("审计日志记录失败: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 从 URI 中匹配规则（按规则长度降序，优先匹配更精确的路径）
     */
    private String matchRule(String[][] rules, String uri) {
        for (String[] rule : rules) {
            if (uri.startsWith(rule[0])) {
                return rule[1];
            }
        }
        return null;
    }

    /**
     * 构建操作对象描述
     */
    private String buildTarget(String method, String uri, String pathId, Object requestBody,
                               String entityNameFromDb, Object result) {
        // 登录/退出：直接返回固定描述
        if (uri.contains("/logout")) return "退出登录";
        if (uri.contains("/login")) return "登录成功";

        // 恢复操作：URI 格式 /api/admin/restore/{entityType}/{id}
        if (uri.contains("/restore/")) {
            String[] segments = uri.split("/");
            String entityId = segments[segments.length - 1];
            String entityType = segments[segments.length - 2];
            String label = entityLabelMap.getOrDefault(entityType, entityType);
            return label + " #" + entityId;
        }

        StringBuilder sb = new StringBuilder();

        if ("DELETE".equalsIgnoreCase(method)) {
            // 删除：使用删除前查到的名称
            if (entityNameFromDb != null && !entityNameFromDb.isEmpty()) {
                sb.append("「").append(entityNameFromDb).append("」");
            } else if (pathId != null) {
                sb.append("#").append(pathId);
            }
        } else {
            // 新增/修改：从请求体提取名称
            String nameFromBody = extractName(requestBody);
            if (nameFromBody != null && !nameFromBody.isEmpty()) {
                sb.append("「").append(nameFromBody).append("」");
            }

            // 状态修改等小 Map 请求：使用从数据库查到的名称
            if (sb.length() == 0 && entityNameFromDb != null && !entityNameFromDb.isEmpty()) {
                sb.append("「").append(entityNameFromDb).append("」");
            }

            // 附带 ID
            if (pathId != null) {
                if (sb.length() > 0) sb.append(" ");
                sb.append("#").append(pathId);
            }

            // 兜底：从 body 提取 year/month 等非名称标识
            if (sb.length() == 0 && requestBody instanceof Map) {
                Map<?, ?> bodyMap = (Map<?, ?>) requestBody;
                Object year = bodyMap.get("year");
                Object month = bodyMap.get("month");
                if (year != null && month != null) {
                    sb.append(year).append("-").append(month);
                }
            }

            // POST 新增：MyBatis-Plus insert 后 ID 回填到实体，从 request body 提取
            if (pathId == null && requestBody != null) {
                String idFromBody = extractIdFromObject(requestBody);
                if (idFromBody != null) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append("#").append(idFromBody);
                }
            }

            // 兜底：尝试从响应提取 ID
            if (pathId == null && sb.toString().contains("#") == false && result != null) {
                String idFromResult = extractIdFromResult(result);
                if (idFromResult != null) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append("#").append(idFromResult);
                }
            }
        }

        return sb.toString();
    }

    /**
     * 判断请求体是否为小 Map（≤3 个 key）且不含名称字段
     * 用于识别状态修改、标记修改等轻量请求
     */
    private boolean isSmallMapWithoutName(Object body) {
        if (!(body instanceof Map)) return false;
        Map<?, ?> map = (Map<?, ?>) body;
        if (map.size() > 3) return false;
        for (String field : NAME_FIELDS) {
            if (map.containsKey(field) && map.get(field) != null && !map.get(field).toString().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 从请求体对象中提取 ID（MyBatis-Plus insert 后回填的 ID）
     */
    private String extractIdFromObject(Object obj) {
        try {
            Map<String, Object> map = objectMapper.convertValue(obj, Map.class);
            Object id = map.get("id");
            if (id != null && !id.toString().equals("0") && !id.toString().isEmpty()) {
                return String.valueOf(id);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String extractPathParam(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature sig = (MethodSignature) joinPoint.getSignature();
            Object[] args = joinPoint.getArgs();
            Annotation[][] paramAnnotations = sig.getMethod().getParameterAnnotations();
            for (int i = 0; i < paramAnnotations.length; i++) {
                for (Annotation a : paramAnnotations[i]) {
                    if (a instanceof PathVariable) {
                        return String.valueOf(args[i]);
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Object extractRequestBody(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature sig = (MethodSignature) joinPoint.getSignature();
            Object[] args = joinPoint.getArgs();
            Annotation[][] paramAnnotations = sig.getMethod().getParameterAnnotations();
            for (int i = 0; i < paramAnnotations.length; i++) {
                for (Annotation a : paramAnnotations[i]) {
                    if (a instanceof RequestBody) {
                        return args[i];
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String extractName(Object obj) {
        if (obj == null) return null;
        try {
            Map<String, Object> map = objectMapper.convertValue(obj, Map.class);
            for (String field : NAME_FIELDS) {
                Object val = map.get(field);
                if (val != null && !val.toString().isEmpty()) {
                    return val.toString();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 删除前根据 URI 和 ID 查询实体名称
     */
    private String fetchEntityName(String uri, String idStr) {
        try {
            String mapperName = matchMapper(uri);
            if (mapperName == null) return null;

            BaseMapper<?> mapper = applicationContext.getBean(mapperName, BaseMapper.class);
            Object entity = mapper.selectById(Integer.valueOf(idStr));
            if (entity == null) return null;

            Map<String, Object> map = objectMapper.convertValue(entity, Map.class);

            // 订单删除：订单号 + 客户/供应商名称
            if (uri.startsWith("/api/admin/sale-orders")) {
                String orderNo = map.get("orderNo") != null ? map.get("orderNo").toString() : "";
                String customerName = map.get("customerName") != null ? map.get("customerName").toString() : "";
                return (orderNo + " " + customerName).trim();
            }
            if (uri.startsWith("/api/admin/purchase-orders")) {
                String orderNo = map.get("orderNo") != null ? map.get("orderNo").toString() : "";
                String supplierName = map.get("supplierName") != null ? map.get("supplierName").toString() : "";
                return (orderNo + " " + supplierName).trim();
            }

            for (String field : NAME_FIELDS) {
                Object val = map.get(field);
                if (val != null && !val.toString().isEmpty()) {
                    return val.toString();
                }
            }
            return "#" + idStr;
        } catch (Exception e) {
            return "#" + idStr;
        }
    }

    private String matchMapper(String uri) {
        for (String[] rule : MAPPER_RULES) {
            if (uri.startsWith(rule[0])) {
                return rule[1];
            }
        }
        return null;
    }

    private String extractIdFromResult(Object result) {
        try {
            Map<String, Object> map = objectMapper.convertValue(result, Map.class);
            Object data = map.get("data");
            if (data == null) return null;
            if (data instanceof String) return null;
            Map<String, Object> dataMap = objectMapper.convertValue(data, Map.class);
            Object id = dataMap.get("id");
            if (id != null) return String.valueOf(id);
        } catch (Exception ignored) {}
        return null;
    }

    private String parseAction(String method, String uri) {
        if (uri.contains("/restore/")) return "恢复";
        return switch (method) {
            case "POST" -> "新增";
            case "PUT", "PATCH" -> "修改";
            case "DELETE" -> "删除";
            default -> method;
        };
    }

    /**
     * 从请求参数构建目标描述（用于无 @PathVariable 无 @RequestBody 的端点）
     */
    private String buildTargetFromParams(HttpServletRequest req) {
        StringBuilder sb = new StringBuilder();
        String name = req.getParameter("name");
        String year = req.getParameter("year");
        String month = req.getParameter("month");
        if (name != null && !name.isEmpty()) {
            sb.append("「").append(name).append("」");
        }
        if (year != null && month != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(year).append("-").append(month);
        }
        return sb.toString();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
