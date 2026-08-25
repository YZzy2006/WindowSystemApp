-- ============================================================
-- migration-015: 数据库索引全面优化
-- 为高频查询添加针对性索引，提升列表、仪表盘、收付款等查询性能
-- ============================================================

-- ============================================================
-- 1. CRITICAL — 销售订单主表（每次打开页面都在用）
-- ============================================================

-- 列表筛选：WHERE order_type = ? AND status != 'cancelled'
CREATE INDEX idx_sale_order_type_status ON sale_order(order_type, status);

-- 未结清订单查询：WHERE is_cleared = 0 AND status != 'cancelled'
CREATE INDEX idx_sale_order_cleared ON sale_order(is_cleared, status);

-- 列表分页排序：ORDER BY order_date DESC, id DESC
CREATE INDEX idx_sale_order_date ON sale_order(order_date, id);

-- ============================================================
-- 2. CRITICAL — 订单子表（每次打开订单详情都查）
-- ============================================================

-- 销售订单明细：WHERE order_id = ?
CREATE INDEX idx_sale_order_item_order ON sale_order_item(order_id);

-- 采购订单明细：WHERE purchase_order_id = ?
CREATE INDEX idx_purchase_order_item_order ON purchase_order_item(purchase_order_id);

-- ============================================================
-- 3. HIGH — 采购订单主表
-- ============================================================

-- 列表筛选：WHERE status != 'cancelled'
CREATE INDEX idx_purchase_order_status ON purchase_order(status);

-- 列表分页排序：ORDER BY order_date DESC, id DESC
CREATE INDEX idx_purchase_order_date ON purchase_order(order_date, id);

-- 未结清查询：WHERE is_cleared = 0 AND status != 'cancelled'
CREATE INDEX idx_purchase_order_cleared ON purchase_order(is_cleared, status);

-- ============================================================
-- 4. HIGH — 退货单主表 + 子表
-- ============================================================

-- 销售退货单
CREATE INDEX idx_sale_return_status ON sale_return(status);
CREATE INDEX idx_sale_return_date ON sale_return(return_date, id);

-- 销售退货明细：WHERE return_id = ?
CREATE INDEX idx_sale_return_item_return ON sale_return_item(return_id);

-- 采购退货单
CREATE INDEX idx_purchase_return_status ON purchase_return(status);
CREATE INDEX idx_purchase_return_date ON purchase_return(return_date, id);

-- 采购退货明细：WHERE return_id = ?
CREATE INDEX idx_purchase_return_item_return ON purchase_return_item(return_id);

-- ============================================================
-- 5. HIGH — 收付款表（已有 idx_order, idx_date 等，补复合索引）
-- ============================================================

-- 收付款同步 + 恢复：WHERE order_id = ? AND type = ?
CREATE INDEX idx_payment_order_type ON payment(order_id, type);

-- ============================================================
-- 6. MEDIUM — 出入库子表
-- ============================================================

-- 入库明细：WHERE stock_in_id = ? / WHERE commodity_id = ?
CREATE INDEX idx_stock_in_item_order ON stock_in_item(stock_in_id);
CREATE INDEX idx_stock_in_item_commodity ON stock_in_item(commodity_id);

-- 出库明细：WHERE stock_out_id = ? / WHERE commodity_id = ?
CREATE INDEX idx_stock_out_item_order ON stock_out_item(stock_out_id);
CREATE INDEX idx_stock_out_item_commodity ON stock_out_item(commodity_id);

-- ============================================================
-- 7. MEDIUM — 售后单 + 询价单 + 商品
-- ============================================================

-- 售后单：WHERE status = ? / WHERE type = ?
CREATE INDEX idx_after_sale_status ON after_sale_order(status);
CREATE INDEX idx_after_sale_type ON after_sale_order(type);

-- 询价单：WHERE is_read = 0（未读计数）
CREATE INDEX idx_enquiry_read ON enquiry(is_read);

-- 商品：WHERE product_type = ?
CREATE INDEX idx_commodity_type ON commodity(product_type);

-- ============================================================
-- 8. LOW — 其他表
-- ============================================================

-- 操作日志：ORDER BY create_time DESC
CREATE INDEX idx_oplog_time ON operation_log(create_time);

-- 产品：WHERE category_id = ? AND is_show = 1
CREATE INDEX idx_product_category ON product(category_id, is_show);

-- 订单序列号：WHERE seq_type = ? FOR UPDATE
CREATE INDEX idx_order_sequence_type ON order_sequence(seq_type);
