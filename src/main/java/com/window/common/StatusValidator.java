package com.window.common;

import java.util.Map;
import java.util.Set;

public final class StatusValidator {

    private static final Set<String> VALID_ORDER_STATUSES = Set.of("pending", "processing", "completed", "cancelled");
    private static final Set<String> VALID_RETURN_STATUSES = Set.of("pending", "processing", "completed", "cancelled");

    // 内部系统：任意状态可互转（已完成/已取消等终态不再锁定）
    private static final Map<String, Set<String>> ORDER_RULES = Map.of(
            "pending", Set.of("pending", "processing", "completed", "cancelled"),
            "processing", Set.of("pending", "processing", "completed", "cancelled"),
            "completed", Set.of("pending", "processing", "completed", "cancelled"),
            "cancelled", Set.of("pending", "processing", "completed", "cancelled")
    );

    private static final Map<String, Set<String>> RETURN_RULES = Map.of(
            "pending", Set.of("processing", "completed", "cancelled"),
            "processing", Set.of("pending", "completed", "cancelled"),
            "completed", Set.of("cancelled"),
            "cancelled", Set.of()
    );

    public static void validateOrder(String current, String next) {
        validate(ORDER_RULES, current, next);
    }

    public static void validateReturn(String current, String next) {
        validate(RETURN_RULES, current, next);
    }

    private static void validate(Map<String, Set<String>> rules, String current, String next) {
        if (current == null || next == null) {
            throw new IllegalArgumentException("状态不能为空");
        }
        Set<String> allowed = rules.get(current);
        if (allowed == null || !allowed.contains(next)) {
            throw new IllegalArgumentException("非法状态变更: " + current + " → " + next);
        }
    }
}
