-- 案例表新增 productId 字段，关联产品表
-- 用于案例详情页展示关联产品的款式颜色、五金、服务、性能、价格等信息
ALTER TABLE case_info ADD COLUMN product_id INT DEFAULT NULL COMMENT '关联产品ID' AFTER is_show;
