-- after_sale_order 表补充缺失的列
ALTER TABLE after_sale_order ADD COLUMN sale_order_no VARCHAR(64) DEFAULT NULL COMMENT '关联销售单号' AFTER order_id;
ALTER TABLE after_sale_order ADD COLUMN customer_name VARCHAR(100) DEFAULT NULL COMMENT '客户名称' AFTER customer_id;
