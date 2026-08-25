package com.window.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

@Component
@Profile("local")
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    public DatabaseInitializer(JdbcTemplate jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        boolean firstLaunch = false;
        try {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM admin", Integer.class);
            if (count != null && count > 0) {
                log.info("Database already initialized ({} admins), skipping h2-init.sql", count);
            } else {
                firstLaunch = true;
            }
        } catch (Exception e) {
            log.info("Admin table not found, running first-time initialization");
            firstLaunch = true;
        }

        if (firstLaunch) {
            log.info("First launch detected, executing h2-init.sql...");
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("db/h2-init.sql"));
            populator.setSeparator(";");
            populator.setCommentPrefix("--");
            populator.setSqlScriptEncoding("UTF-8"); // 必须 UTF-8，否则中文 Windows 默认 GBK 会乱码（单位/名称）
            populator.execute(dataSource);
            log.info("h2-init.sql executed successfully");
        }

        // 无论是否首次启动，都幂等补齐历史上缺失的列
        ensureColumns();
        // 公式模型迁移：商品回填 formulaId（单位默认公式 / 旧 pricingRule）
        migrateCommodityFormula();
    }

    /**
     * 幂等补齐历史上缺失的列（仅 H2 local profile）。
     * 修复 sale_return/purchase_return 缺 update_time 导致的
     * "Column update_time not found"（新建/更新退货单即 500）。
     */
    private void ensureColumns() {
        String[] statements = {
                "ALTER TABLE sale_return ADD COLUMN IF NOT EXISTS update_time DATETIME DEFAULT CURRENT_TIMESTAMP",
                "ALTER TABLE purchase_return ADD COLUMN IF NOT EXISTS update_time DATETIME DEFAULT CURRENT_TIMESTAMP",
                "ALTER TABLE commodity ADD COLUMN IF NOT EXISTS aliases TEXT",
                "ALTER TABLE sale_order_item ADD COLUMN IF NOT EXISTS manual_area TINYINT DEFAULT 0",
                "ALTER TABLE sale_order_item ADD COLUMN IF NOT EXISTS fang_pending TINYINT DEFAULT 0",
                "ALTER TABLE sale_order_item ADD COLUMN IF NOT EXISTS door_pending TINYINT DEFAULT 0",
        };
        for (String sql : statements) {
            try {
                jdbc.execute(sql);
                log.info("Ensured column: {}", sql);
            } catch (Exception e) {
                log.warn("ensure column skipped ({}): {}", e.getMessage(), sql);
            }
        }
        // 金额/数量字段精度扩容：自定义公式可能算出超大结果（如 (宽*高)+(宽+高)），
        // 超过 DECIMAL(10,2) 上限（99,999,999.99）时保存报"数据约束冲突"。
        // 扩到 DECIMAL(12,2)/(12,4)（上限 999 亿），与订单总额字段一致。幂等执行。
        String[] alterPrecisions = {
                "ALTER TABLE sale_order_item ALTER COLUMN amount DECIMAL(12, 2)",
                "ALTER TABLE sale_order_item ALTER COLUMN quantity DECIMAL(12, 4)",
                "ALTER TABLE purchase_order_item ALTER COLUMN amount DECIMAL(12, 2)",
                "ALTER TABLE purchase_order_item ALTER COLUMN quantity DECIMAL(12, 4)",
        };
        for (String sql : alterPrecisions) {
            try {
                jdbc.execute(sql);
                log.info("Ensured column precision: {}", sql);
            } catch (Exception e) {
                log.warn("ensure column precision skipped ({}): {}", e.getMessage(), sql);
            }
        }
    }

    /**
     * 公式模型迁移（幂等）：
     * 1. 补充「延米计价」「吊脚计价」等迁移目标公式；
     * 2. 为 formula_id 为空的存量商品回填公式（旧 pricingRule 优先，其次按单位默认公式）。
     */
    private void migrateCommodityFormula() {
        ensureFormula("延米计价", "米", "(宽+高)*2/1000",
                "[{\"name\": \"宽\", \"unit\": \"mm\", \"label\": \"宽(mm)\", \"default\": \"\"}, {\"name\": \"高\", \"unit\": \"mm\", \"label\": \"高(mm)\", \"default\": \"\"}]",
                "按单樘延米(周长/2)计算价格", 6);
        ensureFormula("吊脚计价", "吊脚", "高/1000",
                "[{\"name\": \"高\", \"unit\": \"mm\", \"label\": \"高(mm)\", \"default\": \"\"}]",
                "按高度(吊脚)计算价格", 7);
        migrateFormulaToPerUnitModel();

        List<Map<String, Object>> rows;
        try {
            rows = jdbc.queryForList("SELECT id, unit, pricing_rule FROM commodity WHERE formula_id IS NULL AND deleted = 0");
        } catch (Exception e) {
            log.info("commodity 表不存在或无 formula_id 列，跳过公式回填: {}", e.getMessage());
            return;
        }
        for (Map<String, Object> row : rows) {
            Object idObj = row.get("id");
            if (idObj == null) continue;
            long id = ((Number) idObj).longValue();
            String unit = row.get("unit") == null ? null : row.get("unit").toString();
            String rule = row.get("pricing_rule") == null ? null : row.get("pricing_rule").toString();
            Long formulaId = resolveFormulaId(unit, rule);
            if (formulaId == null) continue;
            try {
                jdbc.update("UPDATE commodity SET formula_id = ? WHERE id = ? AND formula_id IS NULL", formulaId, id);
                log.info("商品 id={} 回填公式 formulaId={}", id, formulaId);
            } catch (Exception e) {
                log.warn("商品公式回填失败 id={}: {}", id, e.getMessage());
            }
        }
    }

    /**
     * 公式模型迁移（幂等）：统一为"单面积模型"——公式只算每樘计量值，樘数由计算层乘。
     * 面积计价 宽*高*樘数/1000000 → 宽*高/1000000（每樘㎡）
     * 包边计价 (宽+高)*樘数/1000 → (宽+高)/1000（每樘米）
     * 樘数计价 樘数 → 1（每樘=1）
     * 数学结果等价（计算层会乘樘数），历史订单金额不变；公式内容变清晰，客户看"樘数×面积×单价"。
     */
    private void migrateFormulaToPerUnitModel() {
        // 只迁移"仍含旧模式（公式内含樘数/或裸樘数）"的同名公式——用户若在管理页自定义过
        // 这些公式（加了损耗系数等），不再被覆盖。旧模式特征：表达式含 *樘数 或 恰为 樘数。
        String[][] migrations = {
                {"面积计价", "宽*高/1000000", "%*樘数%"},
                {"面积计价(sqm)", "宽*高/1000000", "%*樘数%"},
                {"包边计价", "(宽+高)/1000", "%*樘数%"},
                {"樘数计价", "1", "樘数"},
                {"元默认计价", "宽*高/1000000", "%*樘数%"},
        };
        for (String[] m : migrations) {
            try {
                int updated = jdbc.update("UPDATE pricing_formula SET formula = ?, update_time = CURRENT_TIMESTAMP "
                        + "WHERE name = ? AND deleted = 0 AND formula LIKE ?", m[1], m[0], m[2]);
                if (updated > 0) {
                    log.info("公式[{}]已迁移为单面积模型: {}", m[0], m[1]);
                }
            } catch (Exception e) {
                log.warn("公式迁移失败 [{}]: {}", m[0], e.getMessage());
            }
        }
    }

    private void ensureFormula(String name, String unit, String formula, String params, String desc, int sort) {
        try {
            Integer exists = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM pricing_formula WHERE name = ? AND deleted = 0", Integer.class, name);
            if (exists != null && exists > 0) return;
            jdbc.update("INSERT INTO pricing_formula (name, unit, formula, parameters, description, sort, create_time, update_time, deleted) "
                            + "VALUES (?,?,?,?,?,?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)",
                    name, unit, formula, params, desc, sort);
            log.info("已补充公式: {}", name);
        } catch (Exception e) {
            log.warn("补充公式失败 {}: {}", name, e.getMessage());
        }
    }

    /** 解析商品默认公式：旧 pricingRule 优先，其次按单位默认公式（sort 最小） */
    private Long resolveFormulaId(String unit, String pricingRule) {
        if (pricingRule != null && !pricingRule.isBlank()) {
            String formulaName = switch (pricingRule.trim()) {
                case "area" -> "面积计价";
                case "count" -> "樘数计价";
                case "linear" -> "延米计价";
                case "height" -> "吊脚计价";
                default -> null;
            };
            if (formulaName != null) {
                List<Long> ids = jdbc.queryForList(
                        "SELECT id FROM pricing_formula WHERE name = ? AND deleted = 0 ORDER BY id ASC LIMIT 1",
                        Long.class, formulaName);
                if (!ids.isEmpty()) return ids.get(0);
            }
        }
        if (unit != null && !unit.isBlank()) {
            return ensureUnitDefaultFormula(unit);
        }
        return null;
    }

    /** 返回该单位的默认公式；若无干净公式（如旧库中文乱码），自动创建一个 */
    private Long ensureUnitDefaultFormula(String unit) {
        List<Long> ids = jdbc.queryForList(
                "SELECT id FROM pricing_formula WHERE unit = ? AND deleted = 0 ORDER BY sort ASC, id ASC LIMIT 1",
                Long.class, unit);
        if (!ids.isEmpty()) return ids.get(0);

        String name;
        String expr;
        switch (unit) {
            case "套" -> { name = "樘数计价"; expr = "1"; }
            case "方" -> { name = "面积计价"; expr = "宽*高/1000000"; }
            case "sqm" -> { name = "面积计价(sqm)"; expr = "宽*高/1000000"; }
            case "米" -> { name = "包边计价"; expr = "(宽+高)/1000"; }
            case "吊脚" -> { name = "吊脚计价"; expr = "高/1000"; }
            default -> { name = unit + "默认计价"; expr = "宽*高/1000000"; }
        }
        String params = "[{\"name\": \"宽\", \"unit\": \"mm\", \"label\": \"宽(mm)\", \"default\": \"\"}, "
                + "{\"name\": \"高\", \"unit\": \"mm\", \"label\": \"高(mm)\", \"default\": \"\"}, "
                + "{\"name\": \"樘数\", \"unit\": \"樘\", \"label\": \"樘数\", \"default\": \"1\"}]";
        try {
            jdbc.update("INSERT INTO pricing_formula (name, unit, formula, parameters, description, sort, create_time, update_time, deleted) "
                            + "VALUES (?,?,?,?,?,?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)",
                    name, unit, expr, params, "默认计价公式", 0);
            log.info("已为单位创建默认公式: {} ({})", name, unit);
        } catch (Exception e) {
            log.warn("创建单位默认公式失败 {}: {}", unit, e.getMessage());
            return null;
        }
        ids = jdbc.queryForList("SELECT id FROM pricing_formula WHERE unit = ? AND deleted = 0 ORDER BY id ASC LIMIT 1", Long.class, unit);
        return ids.isEmpty() ? null : ids.get(0);
    }
}
