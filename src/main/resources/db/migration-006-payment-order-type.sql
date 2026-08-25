-- 给 payment 表加 order_type 列，用于精确判断关联订单类型（替代单号前缀判断）
ALTER TABLE payment ADD COLUMN order_type VARCHAR(20) DEFAULT NULL COMMENT '关联订单类型: sale/presale/purchase/sale_return/purchase_return' AFTER remark;
