-- print_setting 表新增 type 列，区分订单/售后单打印配置
ALTER TABLE print_setting ADD COLUMN type VARCHAR(32) DEFAULT 'order' COMMENT '配置类型：order订单/after_sale售后单';
UPDATE print_setting SET type = 'order' WHERE type IS NULL;
ALTER TABLE print_setting ADD UNIQUE INDEX uk_type (type);
