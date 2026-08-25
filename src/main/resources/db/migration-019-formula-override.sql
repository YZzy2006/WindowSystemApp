-- Issue 6: 销售单行支持 formulaId 覆盖 + extraFee + formula_snapshot
-- 后端改用 formulaId 匹配公式，同 unit 不同商品可用不同公式

-- sale_order_item: 加 formula_id, extra_fee, formula_snapshot
ALTER TABLE sale_order_item ADD COLUMN IF NOT EXISTS formula_id BIGINT DEFAULT NULL COMMENT '关联公式ID（可覆盖商品默认公式）';
ALTER TABLE sale_order_item ADD COLUMN IF NOT EXISTS extra_fee DECIMAL(10,2) DEFAULT 0.00 COMMENT '额外费用（加高费、开模费等）';
ALTER TABLE sale_order_item ADD COLUMN IF NOT EXISTS formula_snapshot TEXT DEFAULT NULL COMMENT '公式快照JSON（保存时的公式表达式和参数）';