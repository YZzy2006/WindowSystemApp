-- 商品表增加拆分成本字段
ALTER TABLE commodity
  ADD COLUMN material_cost decimal(10,2) DEFAULT NULL COMMENT '材料成本',
  ADD COLUMN labor_cost decimal(10,2) DEFAULT NULL COMMENT '人工成本',
  ADD COLUMN accessory_cost decimal(10,2) DEFAULT NULL COMMENT '配件成本';

-- 销售订单明细表增加拆分成本字段
ALTER TABLE sale_order_item
  ADD COLUMN material_cost decimal(10,2) DEFAULT NULL COMMENT '材料成本',
  ADD COLUMN labor_cost decimal(10,2) DEFAULT NULL COMMENT '人工成本',
  ADD COLUMN accessory_cost decimal(10,2) DEFAULT NULL COMMENT '配件成本';