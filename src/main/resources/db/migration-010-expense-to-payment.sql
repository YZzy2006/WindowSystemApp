-- 迁移：将 expense 表数据合并到 payment 表
-- expense.type（办公/交通/餐饮等）映射到 payment.party_name
-- 所有 expense 记录作为 type='payment'（付款），不绑订单

INSERT INTO payment (payment_date, type, amount, party_name, remark, create_time)
SELECT expense_date, 'payment', amount, type, remark, create_time FROM expense;

-- 迁移完成后可手动删除 expense 表：
-- DROP TABLE IF EXISTS expense;
