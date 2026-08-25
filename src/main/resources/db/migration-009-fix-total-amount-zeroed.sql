-- 修复 totalAmount 被 syncSaleOrderPaid 等方法意外清零的问题
-- 根因：new SaleOrder() 默认 totalAmount=0，updateById 会把 0 写入 DB

-- 1. 修复 sale_order：从明细重新计算 total_amount（外键列：order_id）
UPDATE sale_order so
SET total_amount = COALESCE(
    (SELECT SUM(amount) FROM sale_order_item WHERE order_id = so.id),
    0
)
WHERE total_amount = 0
  AND EXISTS (SELECT 1 FROM sale_order_item WHERE order_id = so.id);

-- 2. 修复 purchase_order：从明细重新计算 total_amount（外键列：purchase_order_id）
UPDATE purchase_order po
SET total_amount = COALESCE(
    (SELECT SUM(amount) FROM purchase_order_item WHERE purchase_order_id = po.id),
    0
)
WHERE total_amount = 0
  AND EXISTS (SELECT 1 FROM purchase_order_item WHERE purchase_order_id = po.id);

-- 3. 修复 sale_return：从明细重新计算 total_amount（外键列：return_id）
UPDATE sale_return sr
SET total_amount = COALESCE(
    (SELECT SUM(amount) FROM sale_return_item WHERE return_id = sr.id),
    0
)
WHERE total_amount = 0
  AND EXISTS (SELECT 1 FROM sale_return_item WHERE return_id = sr.id);

-- 4. 修复 purchase_return：从明细重新计算 total_amount（外键列：return_id）
UPDATE purchase_return pr
SET total_amount = COALESCE(
    (SELECT SUM(amount) FROM purchase_return_item WHERE return_id = pr.id),
    0
)
WHERE total_amount = 0
  AND EXISTS (SELECT 1 FROM purchase_return_item WHERE return_id = pr.id);

-- 5. 重新同步所有订单的 paid_amount（从实际收付款记录）
UPDATE sale_order so
SET paid_amount = COALESCE(
    (SELECT SUM(amount) FROM payment WHERE order_id = so.id AND type = 'receipt'),
    0
);

UPDATE purchase_order po
SET paid_amount = COALESCE(
    (SELECT SUM(amount) FROM payment WHERE order_id = po.id AND type = 'payment'),
    0
);

UPDATE sale_return sr
SET paid_amount = COALESCE(
    (SELECT SUM(amount) FROM payment WHERE order_id = sr.id AND type = 'payment'),
    0
);

UPDATE purchase_return pr
SET paid_amount = COALESCE(
    (SELECT SUM(amount) FROM payment WHERE order_id = pr.id AND type = 'receipt'),
    0
);

-- 6. 重新计算 is_cleared（未收金额 <= 1 元视为结清）
UPDATE sale_order SET is_cleared = CASE WHEN total_amount > 0 AND (total_amount - paid_amount) <= 1 THEN 1 ELSE 0 END;
UPDATE purchase_order SET is_cleared = CASE WHEN total_amount > 0 AND (total_amount - paid_amount) <= 1 THEN 1 ELSE 0 END;
UPDATE sale_return SET is_cleared = CASE WHEN total_amount > 0 AND (total_amount - paid_amount) <= 1 THEN 1 ELSE 0 END;
UPDATE purchase_return SET is_cleared = CASE WHEN total_amount > 0 AND (total_amount - paid_amount) <= 1 THEN 1 ELSE 0 END;
