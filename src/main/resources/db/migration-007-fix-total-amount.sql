-- 修复 sale_order.total_amount 与 sale_order_item 明细合计不一致的问题
-- 以明细为准，将 total_amount 更新为 SUM(amount)

UPDATE sale_order so
SET total_amount = COALESCE(
    (SELECT SUM(amount) FROM sale_order_item WHERE order_id = so.id),
    0
)
WHERE total_amount != COALESCE(
    (SELECT SUM(amount) FROM sale_order_item WHERE order_id = so.id),
    0
);

-- 重新同步所有订单的 paidAmount 和 isCleared
-- paidAmount = SUM(receipt payments)
UPDATE sale_order so
SET paid_amount = COALESCE(
    (SELECT SUM(amount) FROM payment WHERE order_id = so.id AND type = 'receipt'),
    0
);

-- isCleared: 未收金额占比 < 1% 视为结清
UPDATE sale_order
SET is_cleared = CASE
    WHEN total_amount > 0 AND (total_amount - paid_amount) / total_amount < 0.01 THEN 1
    ELSE 0
END;
