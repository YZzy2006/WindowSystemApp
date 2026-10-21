CREATE TABLE IF NOT EXISTS admin (
  id int NOT NULL AUTO_INCREMENT,
  username varchar(50) DEFAULT NULL,
  password varchar(100) DEFAULT NULL,
  token_id varchar(50) DEFAULT NULL,
  role varchar(20) NOT NULL DEFAULT 'admin',
  is_frozen tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
UNIQUE KEY admin_username (username)
);

CREATE TABLE IF NOT EXISTS after_sale_order (
  id int NOT NULL AUTO_INCREMENT,
  order_no varchar(64) DEFAULT NULL,
  order_id int DEFAULT NULL,
  sale_order_no varchar(64) DEFAULT NULL,
  customer_id int NOT NULL,
  customer_name varchar(100) DEFAULT NULL,
  type varchar(32) NOT NULL DEFAULT 'repair',
  source varchar(32) DEFAULT 'phone',
  description text,
  status varchar(32) NOT NULL DEFAULT 'pending',
  assigned_to varchar(64) DEFAULT NULL,
  scheduled_date date DEFAULT NULL,
  completed_date date DEFAULT NULL,
  resolution varchar(32) DEFAULT NULL,
  cost decimal(10,2) DEFAULT '0.00',
  is_warranty tinyint DEFAULT '0',
  remark text,
  images CLOB DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT CURRENT_TIMESTAMP,
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS attendance (
  id int NOT NULL AUTO_INCREMENT,
  "year" int NOT NULL,
  "month" int NOT NULL,
  employee_name varchar(50) NOT NULL,
  period varchar(10) NOT NULL,
  day_1 varchar(10) DEFAULT NULL,
  day_2 varchar(10) DEFAULT NULL,
  day_3 varchar(10) DEFAULT NULL,
  day_4 varchar(10) DEFAULT NULL,
  day_5 varchar(10) DEFAULT NULL,
  day_6 varchar(10) DEFAULT NULL,
  day_7 varchar(10) DEFAULT NULL,
  day_8 varchar(10) DEFAULT NULL,
  day_9 varchar(10) DEFAULT NULL,
  day_10 varchar(10) DEFAULT NULL,
  day_11 varchar(10) DEFAULT NULL,
  day_12 varchar(10) DEFAULT NULL,
  day_13 varchar(10) DEFAULT NULL,
  day_14 varchar(10) DEFAULT NULL,
  day_15 varchar(10) DEFAULT NULL,
  day_16 varchar(10) DEFAULT NULL,
  day_17 varchar(10) DEFAULT NULL,
  day_18 varchar(10) DEFAULT NULL,
  day_19 varchar(10) DEFAULT NULL,
  day_20 varchar(10) DEFAULT NULL,
  day_21 varchar(10) DEFAULT NULL,
  day_22 varchar(10) DEFAULT NULL,
  day_23 varchar(10) DEFAULT NULL,
  day_24 varchar(10) DEFAULT NULL,
  day_25 varchar(10) DEFAULT NULL,
  day_26 varchar(10) DEFAULT NULL,
  day_27 varchar(10) DEFAULT NULL,
  day_28 varchar(10) DEFAULT NULL,
  day_29 varchar(10) DEFAULT NULL,
  day_30 varchar(10) DEFAULT NULL,
  day_31 varchar(10) DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
UNIQUE KEY attendance_idx_year_month_name_period ("year","month",employee_name,period)
);

CREATE TABLE IF NOT EXISTS case_info (
  id int NOT NULL AUTO_INCREMENT,
  title varchar(100) DEFAULT NULL,
  image varchar(255) DEFAULT NULL,
  images text,
  description text,
  location varchar(100) DEFAULT NULL,
  product_type varchar(50) DEFAULT NULL,
  profile_spec varchar(100) DEFAULT NULL,
  glass_config varchar(100) DEFAULT NULL,
  hardware_brand varchar(50) DEFAULT NULL,
  before_image varchar(255) DEFAULT NULL,
  before_images text,
  before_desc varchar(200) DEFAULT NULL,
  after_desc varchar(200) DEFAULT NULL,
  customer_need text,
  customer_review text,
  install_duration varchar(30) DEFAULT NULL,
  sort int DEFAULT '0',
  is_show tinyint DEFAULT '1',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  product_id varchar(500) DEFAULT NULL,
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS category (
  id int NOT NULL AUTO_INCREMENT,
  name varchar(50) NOT NULL,
  sort int DEFAULT '0',
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS commodity (
  id int NOT NULL AUTO_INCREMENT,
  category_id int DEFAULT NULL,
  code varchar(50) DEFAULT NULL,
  name varchar(200) NOT NULL,
  model varchar(100) DEFAULT NULL,
  material varchar(100) DEFAULT NULL,
  product_type varchar(50) DEFAULT NULL,
  unit varchar(20) DEFAULT NULL,
  default_price decimal(10,2) DEFAULT NULL,
  cost_price decimal(10,2) DEFAULT NULL,
  material_cost decimal(10,2) DEFAULT NULL,
  labor_cost decimal(10,2) DEFAULT NULL,
  accessory_cost decimal(10,2) DEFAULT NULL,
  pricing_rule varchar(20) DEFAULT NULL,
  warehouse_loc varchar(100) DEFAULT NULL,
  initial_qty decimal(10,4) DEFAULT '0.0000',
  initial_cost decimal(12,2) DEFAULT '0.00',
  current_qty decimal(10,4) DEFAULT '0.0000',
  remark varchar(500) DEFAULT NULL,
  alert_high decimal(10,4) DEFAULT NULL,
  alert_mid decimal(10,4) DEFAULT NULL,
  alert_low decimal(10,4) DEFAULT NULL,
  is_show tinyint DEFAULT '1',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT CURRENT_TIMESTAMP,
  formula_id bigint DEFAULT NULL,
  aliases TEXT DEFAULT NULL,
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
UNIQUE KEY commodity_idx_code (code)
);

CREATE TABLE IF NOT EXISTS commodity_category (
  id int NOT NULL AUTO_INCREMENT,
  name varchar(100) NOT NULL,
  sort int DEFAULT '0',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS customer (
  id int NOT NULL AUTO_INCREMENT,
  name varchar(100) NOT NULL,
  contact varchar(50) DEFAULT NULL,
  phone varchar(20) DEFAULT NULL,
  address varchar(500) DEFAULT NULL,
  remark varchar(500) DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT CURRENT_TIMESTAMP,
  is_starred tinyint DEFAULT '0',
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
UNIQUE KEY customer_uk_name (name)
);

CREATE TABLE IF NOT EXISTS customer_followup (
  id int NOT NULL AUTO_INCREMENT,
  customer_id int NOT NULL,
  admin_id int DEFAULT NULL,
  username varchar(50) DEFAULT '',
  content text NOT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS enquiry (
  id int NOT NULL AUTO_INCREMENT,
  name varchar(50) DEFAULT NULL,
  phone varchar(20) DEFAULT NULL,
  content text,
  is_read tinyint DEFAULT '0',
  need_measure tinyint DEFAULT '0',
  budget varchar(20) DEFAULT NULL,
  is_measured tinyint DEFAULT '0',
  is_starred tinyint DEFAULT '0',
  is_completed tinyint DEFAULT '0',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  remark varchar(500) DEFAULT NULL,
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS login_log (
  id int NOT NULL AUTO_INCREMENT,
  admin_id int NOT NULL,
  username varchar(50) DEFAULT NULL,
  login_ip varchar(50) DEFAULT NULL,
  user_agent varchar(500) DEFAULT NULL,
  status tinyint DEFAULT '1',
  fail_reason varchar(200) DEFAULT NULL,
  remark varchar(200) DEFAULT NULL,
  login_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS operation_log (
  id int NOT NULL AUTO_INCREMENT,
  admin_id int NOT NULL,
  username varchar(50) DEFAULT '',
  module varchar(50) NOT NULL,
  action varchar(20) NOT NULL,
  target varchar(200) DEFAULT '',
  ip varchar(50) DEFAULT '',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS order_lock (
  id int NOT NULL AUTO_INCREMENT,
  order_type varchar(20) NOT NULL,
  order_id int NOT NULL,
  admin_id int NOT NULL,
  username varchar(50) DEFAULT NULL,
  lock_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
UNIQUE KEY order_lock_uk_order (order_type,order_id)
);

CREATE TABLE IF NOT EXISTS order_sequence (
  id int NOT NULL AUTO_INCREMENT,
  seq_type varchar(50) NOT NULL,
  prefix varchar(20) DEFAULT NULL,
  current_seq int DEFAULT '1',
  last_date date DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
UNIQUE KEY order_sequence_idx_type (seq_type)
);

CREATE TABLE IF NOT EXISTS payment (
  id int NOT NULL AUTO_INCREMENT,
  order_id int DEFAULT NULL,
  order_no varchar(50) DEFAULT NULL,
  payment_date date NOT NULL,
  type varchar(20) NOT NULL,
  amount decimal(12,2) NOT NULL,
  party_name varchar(100) DEFAULT NULL,
  remark varchar(500) DEFAULT NULL,
  order_type varchar(20) DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  after_sale_id int DEFAULT NULL,
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS pricing_formula (
  id bigint NOT NULL AUTO_INCREMENT,
  name varchar(100) NOT NULL,
  unit varchar(20) DEFAULT NULL,
  formula text NOT NULL,
  parameters CLOB DEFAULT NULL,
  description varchar(255) DEFAULT NULL,
  sort int DEFAULT '0',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT CURRENT_TIMESTAMP,
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS print_setting (
  id int NOT NULL AUTO_INCREMENT,
  config CLOB NOT NULL,
  update_time datetime DEFAULT CURRENT_TIMESTAMP,
  type varchar(32) DEFAULT 'order',
  PRIMARY KEY (id),
UNIQUE KEY print_setting_uk_type (type)
);

CREATE TABLE IF NOT EXISTS product (
  id int NOT NULL AUTO_INCREMENT,
  category_id int DEFAULT NULL,
  name varchar(100) DEFAULT NULL,
  model varchar(50) DEFAULT NULL,
  material varchar(50) DEFAULT NULL,
  price decimal(10,2) DEFAULT NULL,
  cover_image varchar(255) DEFAULT NULL,
  description text,
  images text,
  specs text,
  price_tag varchar(100) DEFAULT NULL,
  price_breakdown text,
  pricing_rule varchar(500) DEFAULT NULL,
  reference_price varchar(200) DEFAULT NULL,
  option_prices text,
  performance text,
  is_show tinyint DEFAULT '1',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS product_type (
  id int NOT NULL AUTO_INCREMENT,
  name varchar(50) NOT NULL,
  summary text,
  sort int DEFAULT '0',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  summary_desc varchar(50) DEFAULT NULL,
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS purchase_order (
  id int NOT NULL AUTO_INCREMENT,
  order_no varchar(50) NOT NULL,
  supplier_id int DEFAULT NULL,
  supplier_name varchar(100) DEFAULT NULL,
  supplier_contact varchar(50) DEFAULT NULL,
  supplier_phone varchar(20) DEFAULT NULL,
  supplier_address varchar(500) DEFAULT NULL,
  order_date date DEFAULT NULL,
  total_amount decimal(12,2) DEFAULT '0.00',
  status varchar(20) DEFAULT 'pending',
  remark varchar(500) DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT CURRENT_TIMESTAMP,
  paid_amount decimal(12,2) DEFAULT '0.00',
  is_cleared tinyint DEFAULT '0',
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
UNIQUE KEY purchase_order_idx_order_no (order_no)
);

CREATE TABLE IF NOT EXISTS purchase_order_item (
  id int NOT NULL AUTO_INCREMENT,
  purchase_order_id int NOT NULL,
  commodity_id int DEFAULT NULL,
  product_name varchar(200) DEFAULT NULL,
  product_category varchar(100) DEFAULT NULL,
  spec varchar(200) DEFAULT NULL,
  unit varchar(20) DEFAULT NULL,
  warehouse_loc varchar(100) DEFAULT NULL,
  quantity decimal(10,4) DEFAULT NULL,
  unit_price decimal(10,2) DEFAULT NULL,
  amount decimal(10,2) DEFAULT NULL,
  remark varchar(500) DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS purchase_return (
  id int NOT NULL AUTO_INCREMENT,
  order_no varchar(50) NOT NULL,
  original_order_no varchar(50) DEFAULT NULL,
  supplier_id int DEFAULT NULL,
  supplier_name varchar(100) DEFAULT NULL,
  return_date date DEFAULT NULL,
  total_amount decimal(12,2) DEFAULT '0.00',
  status varchar(20) DEFAULT 'pending',
  remark varchar(500) DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT CURRENT_TIMESTAMP,
  paid_amount decimal(12,2) DEFAULT '0.00',
  is_cleared tinyint DEFAULT '0',
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
UNIQUE KEY purchase_return_idx_order_no (order_no)
);

CREATE TABLE IF NOT EXISTS purchase_return_item (
  id int NOT NULL AUTO_INCREMENT,
  return_id int NOT NULL,
  commodity_id int DEFAULT NULL,
  product_name varchar(200) DEFAULT NULL,
  product_category varchar(100) DEFAULT NULL,
  spec varchar(200) DEFAULT NULL,
  unit varchar(20) DEFAULT NULL,
  quantity decimal(10,4) DEFAULT NULL,
  unit_price decimal(10,2) DEFAULT NULL,
  amount decimal(10,2) DEFAULT NULL,
  remark varchar(500) DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS sale_order (
  id int NOT NULL AUTO_INCREMENT,
  order_no varchar(50) NOT NULL,
  order_type varchar(20) DEFAULT 'sale',
  customer_id int DEFAULT NULL,
  customer_name varchar(100) DEFAULT NULL,
  customer_phone varchar(20) DEFAULT NULL,
  customer_address varchar(500) DEFAULT NULL,
  order_date date DEFAULT NULL,
  total_amount decimal(12,2) DEFAULT '0.00',
  deposit decimal(12,2) DEFAULT '0.00',
  paid_amount decimal(12,2) DEFAULT '0.00',
  is_cleared tinyint DEFAULT '0',
  status varchar(20) DEFAULT 'pending',
  remark text,
  notice text,
  window_area decimal(10,2) DEFAULT '0.00',
  window_count int DEFAULT '0',
  door_area decimal(10,2) DEFAULT '0.00',
  door_count int DEFAULT '0',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT CURRENT_TIMESTAMP,
  hidden_product_types varchar(500) DEFAULT NULL,
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
UNIQUE KEY sale_order_idx_order_no (order_no)
);

CREATE TABLE IF NOT EXISTS sale_order_item (
  id int NOT NULL AUTO_INCREMENT,
  order_id int NOT NULL,
  commodity_id int DEFAULT NULL,
  product_name varchar(200) DEFAULT NULL,
  series varchar(100) DEFAULT NULL,
  color varchar(100) DEFAULT NULL,
  product_type varchar(50) DEFAULT NULL,
  unit varchar(20) DEFAULT NULL,
  width decimal(10,2) DEFAULT NULL,
  height decimal(10,2) DEFAULT NULL,
  wall_thickness decimal(10,2) DEFAULT NULL,
  glass_type varchar(100) DEFAULT NULL,
  lock_position varchar(50) DEFAULT NULL,
  door_count int DEFAULT '1',
  fang_count decimal(10,2) DEFAULT NULL,
  diaojiao int DEFAULT NULL,
  tao_count int DEFAULT NULL,
  quantity decimal(10,4) DEFAULT NULL,
  unit_price decimal(10,2) DEFAULT NULL,
  amount decimal(10,2) DEFAULT NULL,
  cost decimal(10,2) DEFAULT NULL,
  material_cost decimal(10,2) DEFAULT NULL,
  labor_cost decimal(10,2) DEFAULT NULL,
  accessory_cost decimal(10,2) DEFAULT NULL,
  formula_id bigint DEFAULT NULL,
  extra_fee decimal(10,2) DEFAULT 0.00,
  formula_snapshot text DEFAULT NULL,
  remark varchar(500) DEFAULT NULL,
  image text,
  sort int DEFAULT '0',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS sale_return (
  id int NOT NULL AUTO_INCREMENT,
  order_no varchar(50) NOT NULL,
  original_order_no varchar(50) DEFAULT NULL,
  customer_id int DEFAULT NULL,
  customer_name varchar(100) DEFAULT NULL,
  return_date date DEFAULT NULL,
  total_amount decimal(12,2) DEFAULT '0.00',
  status varchar(20) DEFAULT 'pending',
  remark varchar(500) DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT CURRENT_TIMESTAMP,
  paid_amount decimal(12,2) DEFAULT '0.00',
  is_cleared tinyint DEFAULT '0',
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
UNIQUE KEY sale_return_idx_order_no (order_no)
);

CREATE TABLE IF NOT EXISTS sale_return_item (
  id int NOT NULL AUTO_INCREMENT,
  return_id int NOT NULL,
  commodity_id int DEFAULT NULL,
  product_name varchar(200) DEFAULT NULL,
  product_category varchar(100) DEFAULT NULL,
  spec varchar(200) DEFAULT NULL,
  unit varchar(20) DEFAULT NULL,
  quantity decimal(10,4) DEFAULT NULL,
  unit_price decimal(10,2) DEFAULT NULL,
  amount decimal(10,2) DEFAULT NULL,
  remark varchar(500) DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS site_config (
  id int NOT NULL DEFAULT '1',
  config_json text,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS stock_in (
  id int NOT NULL AUTO_INCREMENT,
  order_no varchar(50) NOT NULL,
  order_date date DEFAULT NULL,
  applicant varchar(50) DEFAULT NULL,
  warehouse_keeper varchar(50) DEFAULT NULL,
  operator varchar(50) DEFAULT NULL,
  remark varchar(500) DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
UNIQUE KEY stock_in_idx_order_no (order_no)
);

CREATE TABLE IF NOT EXISTS stock_in_item (
  id int NOT NULL AUTO_INCREMENT,
  stock_in_id int NOT NULL,
  commodity_id int DEFAULT NULL,
  product_name varchar(200) DEFAULT NULL,
  product_category varchar(100) DEFAULT NULL,
  spec varchar(200) DEFAULT NULL,
  unit varchar(20) DEFAULT NULL,
  warehouse_loc varchar(100) DEFAULT NULL,
  quantity decimal(10,4) DEFAULT NULL,
  unit_price decimal(10,2) DEFAULT NULL,
  amount decimal(10,2) DEFAULT NULL,
  remark varchar(500) DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS stock_out (
  id int NOT NULL AUTO_INCREMENT,
  order_no varchar(50) NOT NULL,
  order_date date DEFAULT NULL,
  applicant varchar(50) DEFAULT NULL,
  warehouse_keeper varchar(50) DEFAULT NULL,
  operator varchar(50) DEFAULT NULL,
  remark varchar(500) DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
UNIQUE KEY stock_out_idx_order_no (order_no)
);

CREATE TABLE IF NOT EXISTS stock_out_item (
  id int NOT NULL AUTO_INCREMENT,
  stock_out_id int NOT NULL,
  commodity_id int DEFAULT NULL,
  product_name varchar(200) DEFAULT NULL,
  product_category varchar(100) DEFAULT NULL,
  spec varchar(200) DEFAULT NULL,
  unit varchar(20) DEFAULT NULL,
  warehouse_loc varchar(100) DEFAULT NULL,
  quantity decimal(10,4) DEFAULT NULL,
  unit_price decimal(10,2) DEFAULT NULL,
  amount decimal(10,2) DEFAULT NULL,
  remark varchar(500) DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS supplier (
  id int NOT NULL AUTO_INCREMENT,
  name varchar(100) NOT NULL,
  contact varchar(50) DEFAULT NULL,
  phone varchar(20) DEFAULT NULL,
  address varchar(500) DEFAULT NULL,
  remark varchar(500) DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT CURRENT_TIMESTAMP,
  is_starred tinyint DEFAULT '0',
  deleted tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS sys_config (
  id int NOT NULL AUTO_INCREMENT,
  config_key varchar(100) NOT NULL,
  config_value varchar(1000) DEFAULT NULL,
  remark varchar(200) DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
UNIQUE KEY sys_config_idx_key (config_key)
);

-- ============================================================
-- 性能索引（与 MySQL migration-015 对齐）
-- ============================================================

-- 销售订单主表
CREATE INDEX IF NOT EXISTS idx_sale_order_type_status ON sale_order(order_type, status);
CREATE INDEX IF NOT EXISTS idx_sale_order_cleared ON sale_order(is_cleared, status);
CREATE INDEX IF NOT EXISTS idx_sale_order_date ON sale_order(order_date, id);

-- 订单子表
CREATE INDEX IF NOT EXISTS idx_sale_order_item_order ON sale_order_item(order_id);
CREATE INDEX IF NOT EXISTS idx_purchase_order_item_order ON purchase_order_item(purchase_order_id);

-- 采购订单主表
CREATE INDEX IF NOT EXISTS idx_purchase_order_status ON purchase_order(status);
CREATE INDEX IF NOT EXISTS idx_purchase_order_date ON purchase_order(order_date, id);
CREATE INDEX IF NOT EXISTS idx_purchase_order_cleared ON purchase_order(is_cleared, status);

-- 退货单
CREATE INDEX IF NOT EXISTS idx_sale_return_status ON sale_return(status);
CREATE INDEX IF NOT EXISTS idx_sale_return_date ON sale_return(return_date, id);
CREATE INDEX IF NOT EXISTS idx_sale_return_item_return ON sale_return_item(return_id);
CREATE INDEX IF NOT EXISTS idx_purchase_return_status ON purchase_return(status);
CREATE INDEX IF NOT EXISTS idx_purchase_return_date ON purchase_return(return_date, id);
CREATE INDEX IF NOT EXISTS idx_purchase_return_item_return ON purchase_return_item(return_id);

-- 收付款
CREATE INDEX IF NOT EXISTS idx_payment_order_type ON payment(order_id, type);

-- 出入库子表
CREATE INDEX IF NOT EXISTS idx_stock_in_item_order ON stock_in_item(stock_in_id);
CREATE INDEX IF NOT EXISTS idx_stock_in_item_commodity ON stock_in_item(commodity_id);
CREATE INDEX IF NOT EXISTS idx_stock_out_item_order ON stock_out_item(stock_out_id);
CREATE INDEX IF NOT EXISTS idx_stock_out_item_commodity ON stock_out_item(commodity_id);

-- 售后单 + 询价单 + 商品
CREATE INDEX IF NOT EXISTS idx_after_sale_status ON after_sale_order(status);
CREATE INDEX IF NOT EXISTS idx_after_sale_type ON after_sale_order(type);
CREATE INDEX IF NOT EXISTS idx_enquiry_read ON enquiry(is_read);
CREATE INDEX IF NOT EXISTS idx_commodity_type ON commodity(product_type);

-- 其他
CREATE INDEX IF NOT EXISTS idx_oplog_time ON operation_log(create_time);
CREATE INDEX IF NOT EXISTS idx_product_category ON product(category_id, is_show);
CREATE INDEX IF NOT EXISTS idx_order_sequence_type ON order_sequence(seq_type);

-- admin：示例账号（admin / admin123456，其余为示例普通管理员）
MERGE INTO admin (`id`, `username`, `password`, `token_id`, `role`, `is_frozen`) KEY(id) VALUES (1, 'admin', '$2a$10$9kb1hSPhmJ/ymW8pRCtBceSRS./ukpjjw0SpHrP0h/VHDgqyaUviq', NULL, 'super_admin', 0);
MERGE INTO admin (`id`, `username`, `password`, `token_id`, `role`, `is_frozen`) KEY(id) VALUES (2, 'test01', '$2a$10$NnjJ4RdxhRL/ouO7WKmnEuBo.wc5eFBWGQFs70EgPS08wdk1U98Ha', NULL, 'admin', 0);
MERGE INTO admin (`id`, `username`, `password`, `token_id`, `role`, `is_frozen`) KEY(id) VALUES (3, 'test02', '$2a$10$NnjJ4RdxhRL/ouO7WKmnEuBo.wc5eFBWGQFs70EgPS08wdk1U98Ha', NULL, 'admin', 0);
MERGE INTO admin (`id`, `username`, `password`, `token_id`, `role`, `is_frozen`) KEY(id) VALUES (4, 'test03', '$2a$10$NnjJ4RdxhRL/ouO7WKmnEuBo.wc5eFBWGQFs70EgPS08wdk1U98Ha', NULL, 'admin', 0);
MERGE INTO admin (`id`, `username`, `password`, `token_id`, `role`, `is_frozen`) KEY(id) VALUES (5, 'test04', '$2a$10$NnjJ4RdxhRL/ouO7WKmnEuBo.wc5eFBWGQFs70EgPS08wdk1U98Ha', NULL, 'admin', 0);
MERGE INTO admin (`id`, `username`, `password`, `token_id`, `role`, `is_frozen`) KEY(id) VALUES (6, 'test05', '$2a$10$NnjJ4RdxhRL/ouO7WKmnEuBo.wc5eFBWGQFs70EgPS08wdk1U98Ha', NULL, 'admin', 0);

-- attendance
MERGE INTO attendance (`id`, `year`, `month`, `employee_name`, `period`, `day_1`, `day_2`, `day_3`, `day_4`, `day_5`, `day_6`, `day_7`, `day_8`, `day_9`, `day_10`, `day_11`, `day_12`, `day_13`, `day_14`, `day_15`, `day_16`, `day_17`, `day_18`, `day_19`, `day_20`, `day_21`, `day_22`, `day_23`, `day_24`, `day_25`, `day_26`, `day_27`, `day_28`, `day_29`, `day_30`, `day_31`, `create_time`, `update_time`) KEY(id) VALUES (1, 2026, 1, '示例员工A', 'am', 'Y', 'Y', 'N', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-01-01 10:00:00', '2026-01-01 10:00:00');
MERGE INTO attendance (`id`, `year`, `month`, `employee_name`, `period`, `day_1`, `day_2`, `day_3`, `day_4`, `day_5`, `day_6`, `day_7`, `day_8`, `day_9`, `day_10`, `day_11`, `day_12`, `day_13`, `day_14`, `day_15`, `day_16`, `day_17`, `day_18`, `day_19`, `day_20`, `day_21`, `day_22`, `day_23`, `day_24`, `day_25`, `day_26`, `day_27`, `day_28`, `day_29`, `day_30`, `day_31`, `create_time`, `update_time`) KEY(id) VALUES (2, 2026, 1, '示例员工B', 'am', 'Y', 'N', 'Y', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-01-01 10:00:00', '2026-01-01 10:00:00');
MERGE INTO attendance (`id`, `year`, `month`, `employee_name`, `period`, `day_1`, `day_2`, `day_3`, `day_4`, `day_5`, `day_6`, `day_7`, `day_8`, `day_9`, `day_10`, `day_11`, `day_12`, `day_13`, `day_14`, `day_15`, `day_16`, `day_17`, `day_18`, `day_19`, `day_20`, `day_21`, `day_22`, `day_23`, `day_24`, `day_25`, `day_26`, `day_27`, `day_28`, `day_29`, `day_30`, `day_31`, `create_time`, `update_time`) KEY(id) VALUES (3, 2026, 1, '示例员工C', 'am', 'N', 'Y', 'Y', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-01-01 10:00:00', '2026-01-01 10:00:00');

-- case_info
MERGE INTO case_info (`id`, `title`, `image`, `images`, `description`, `location`, `product_type`, `profile_spec`, `glass_config`, `hardware_brand`, `before_image`, `before_images`, `before_desc`, `after_desc`, `customer_need`, `customer_review`, `install_duration`, `sort`, `is_show`, `create_time`, `product_id`, `deleted`) KEY(id) VALUES (1, '示例案例一：客厅阳台封窗', NULL, NULL, '示例说明：本案例为演示数据，用于展示案例详情排版效果。', '示例小区', '断桥铝平开窗', '70系列 1.8mm', '5+20A+5 中空钢化', '示例五金', NULL, NULL, '示例改造前：阳台与客厅之间无隔断，冬季冷风明显。', '示例改造后：整体封窗后空间连成一体，隔音保温明显提升。', '示例需求：希望封窗后不影响采光，同时可开启通风。', '示例评价：施工规范，成品与效果图一致。', '2天', 0, 1, '2026-01-01 10:00:00', '100,101', 0);
MERGE INTO case_info (`id`, `title`, `image`, `images`, `description`, `location`, `product_type`, `profile_spec`, `glass_config`, `hardware_brand`, `before_image`, `before_images`, `before_desc`, `after_desc`, `customer_need`, `customer_review`, `install_duration`, `sort`, `is_show`, `create_time`, `product_id`, `deleted`) KEY(id) VALUES (2, '示例案例二：卧室旧窗改造', NULL, NULL, '示例说明：本案例为演示数据，用于展示案例详情排版效果。', '示例小区', '断桥铝推拉窗', '90系列 2.0mm', '6+12A+6 中空钢化', '示例五金', NULL, NULL, '示例改造前：旧窗密封老化，雨天渗水。', '示例改造后：更换新窗并重做防水密封，渗水问题解决。', '示例需求：解决渗水与隔音问题，工期尽量短。', '示例评价：一天完工，清理干净。', '2天', 0, 1, '2026-01-01 10:00:00', '100,101', 0);
MERGE INTO case_info (`id`, `title`, `image`, `images`, `description`, `location`, `product_type`, `profile_spec`, `glass_config`, `hardware_brand`, `before_image`, `before_images`, `before_desc`, `after_desc`, `customer_need`, `customer_review`, `install_duration`, `sort`, `is_show`, `create_time`, `product_id`, `deleted`) KEY(id) VALUES (3, '示例案例三：入户门更换', NULL, NULL, '示例说明：本案例为演示数据，用于展示案例详情排版效果。', '示例小区', '不锈钢防盗门', '甲级 9cm 门扇', '夹胶钢化玻璃', '示例五金', NULL, NULL, '示例改造前：旧门变形，锁具卡顿。', '示例改造后：更换甲级防盗门，配 C 级锁芯。', '示例需求：希望提升防盗等级并保留原有门洞尺寸。', '示例评价：安装牢固，开关顺畅。', '2天', 0, 1, '2026-01-01 10:00:00', '100,101', 0);

-- category
MERGE INTO category (`id`, `name`, `sort`, `deleted`) KEY(id) VALUES (1,'断桥铝平开窗',1,0);
MERGE INTO category (`id`, `name`, `sort`, `deleted`) KEY(id) VALUES (2,'断桥铝推拉窗',2,0);
MERGE INTO category (`id`, `name`, `sort`, `deleted`) KEY(id) VALUES (3,'断桥铝平开门',3,0);
MERGE INTO category (`id`, `name`, `sort`, `deleted`) KEY(id) VALUES (4,'断桥铝推拉门',4,0);
MERGE INTO category (`id`, `name`, `sort`, `deleted`) KEY(id) VALUES (5,'封阳台 / 封窗改造',5,0);
MERGE INTO category (`id`, `name`, `sort`, `deleted`) KEY(id) VALUES (6,'不锈钢防盗门',6,0);
MERGE INTO category (`id`, `name`, `sort`, `deleted`) KEY(id) VALUES (7,'不锈钢防盗窗',7,0);
MERGE INTO category (`id`, `name`, `sort`, `deleted`) KEY(id) VALUES (8,'不锈钢商铺门',8,0);
MERGE INTO category (`id`, `name`, `sort`, `deleted`) KEY(id) VALUES (9,'实木复合门',9,0);
MERGE INTO category (`id`, `name`, `sort`, `deleted`) KEY(id) VALUES (10,'模压门',10,0);
MERGE INTO category (`id`, `name`, `sort`, `deleted`) KEY(id) VALUES (11,'定制室内木门',11,0);
MERGE INTO category (`id`, `name`, `sort`, `deleted`) KEY(id) VALUES (12,'密封胶条（耐候防老化）',12,0);
MERGE INTO category (`id`, `name`, `sort`, `deleted`) KEY(id) VALUES (13,'五金锁具 / 滑轨铰链',13,0);
MERGE INTO category (`id`, `name`, `sort`, `deleted`) KEY(id) VALUES (14,'中空玻璃',14,0);
MERGE INTO category (`id`, `name`, `sort`, `deleted`) KEY(id) VALUES (15,'防护栏杆 / 简易护栏',15,0);
MERGE INTO category (`id`, `name`, `sort`, `deleted`) KEY(id) VALUES (16,'旧窗改造拆换（含墙体修补）',16,0);
MERGE INTO category (`id`, `name`, `sort`, `deleted`) KEY(id) VALUES (17,'自建房门窗定制方案',17,0);
MERGE INTO category (`id`, `name`, `sort`, `deleted`) KEY(id) VALUES (18,'维修耗材包（密封胶/润滑剂）',18,0);

-- commodity
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (1,1,'GL001','tempered glass','',NULL,NULL,'sqm',NULL,NULL,NULL,NULL,0.0000,0.00,100.0000,NULL,NULL,NULL,NULL,1,'2026-05-16 21:07:16','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (2,3,'SP00002','优惠价格','',NULL,NULL,'元',-10.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (3,4,'SP00003','新10号平开门','',NULL,'入户门','套',100.00,NULL,'套',NULL,0.0000,0.00,0.0000,'',NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-20 22:52:20',2,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (4,5,'SP00004','新12平开门','',NULL,NULL,'套',101.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (5,6,'SP00005','35*48中窄平开门','',NULL,NULL,'套',102.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (6,7,'SP00006','35*65中窄平开门','',NULL,NULL,'套',103.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (7,8,'SP00007','35*16极窄平开门','',NULL,NULL,'套',104.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (8,9,'SP00008','至尊平开门','',NULL,NULL,'套',105.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (9,10,'SP00009','40*65中窄平开门','',NULL,NULL,'套',106.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (10,11,'SP00010','新10号双包套','',NULL,NULL,'套',107.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (11,12,'SP00011','新12双包套','',NULL,NULL,'套',108.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (12,13,'SP00012','35*48中窄双包套','',NULL,NULL,'套',109.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (13,14,'SP00013','35*65中窄双包套','',NULL,NULL,'套',110.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (14,15,'SP00014','35*16极窄双包套','',NULL,NULL,'套',111.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (15,16,'SP00015','至尊双包套','',NULL,NULL,'套',112.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (16,17,'SP00016','40*65中窄双包套','',NULL,NULL,'套',113.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (17,18,'SP00017','新10号对开门','',NULL,NULL,'套',114.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (18,19,'SP00018','新12对开门','',NULL,NULL,'套',115.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (19,20,'SP00019','35*48中窄对开门','',NULL,NULL,'套',116.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (20,21,'SP00020','35*65中窄对开门','',NULL,NULL,'套',117.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (21,22,'SP00021','35*16极窄对开门','',NULL,NULL,'套',118.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (22,23,'SP00022','至尊对开门','',NULL,NULL,'套',119.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (23,24,'SP00023','40*65中窄对开门','',NULL,NULL,'套',120.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (24,25,'SP00024','新10号子母门','',NULL,NULL,'套',121.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (25,26,'SP00025','新12子母门','',NULL,NULL,'套',122.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (26,27,'SP00026','35*48中窄子母门','',NULL,NULL,'套',123.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (27,28,'SP00027','35*65中窄子母门','',NULL,NULL,'套',124.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (28,29,'SP00028','35*16极窄子母门','',NULL,NULL,'套',125.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (29,30,'SP00029','至尊子母门','',NULL,NULL,'套',126.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (30,31,'SP00030','40*65中窄子母门','',NULL,NULL,'套',127.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (31,32,'SP00031','新10号两轨2扇','',NULL,'','方',128.00,99.00,'',NULL,0.0000,0.00,0.0000,'',NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-19 13:57:03',4,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (32,33,'SP00032','新10号两轨4扇','',NULL,NULL,'方',129.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (33,34,'SP00033','新10号三轨3扇','',NULL,NULL,'方',130.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (34,35,'SP00034','新10号三轨6扇','',NULL,NULL,'方',131.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (35,36,'SP00035','新10号单轨推拉','',NULL,NULL,'方',132.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (36,37,'SP00036','新10号单轨吊趟','',NULL,NULL,'方',133.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (37,38,'SP00037','新10号两轨吊趟','',NULL,NULL,'方',134.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (38,39,'SP00038','新12号两轨2扇','',NULL,NULL,'方',135.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (39,40,'SP00039','新12号两轨4扇','',NULL,NULL,'方',136.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (40,41,'SP00040','新12号三轨3扇','',NULL,NULL,'方',137.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (41,42,'SP00041','新12号三轨6扇','',NULL,NULL,'方',138.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (42,43,'SP00042','新12号单轨推拉','',NULL,NULL,'方',139.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (43,44,'SP00043','新12号单轨吊趟','',NULL,NULL,'方',140.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (44,45,'SP00044','新12号两轨吊趟','',NULL,NULL,'方',141.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (45,46,'SP00045','35*48两轨2扇','',NULL,'推拉门','方',142.00,NULL,'area',NULL,0.0000,0.00,0.0000,'',NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-22 21:33:20',4,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (46,47,'SP00046','35*48两轨4扇','',NULL,NULL,'方',143.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (47,48,'SP00047','35*48三轨3扇','',NULL,NULL,'方',144.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (48,49,'SP00048','35*48三轨6扇','',NULL,NULL,'方',145.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (49,50,'SP00049','35*48单轨推拉','',NULL,NULL,'方',146.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (50,51,'SP00050','35*48单轨吊趟','',NULL,NULL,'方',147.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (51,52,'SP00051','35*48两轨吊趟','',NULL,NULL,'方',148.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (52,53,'SP00052','35*48三联动','',NULL,NULL,'方',149.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (53,54,'SP00053','35*65两轨2扇','',NULL,NULL,'方',380.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (54,55,'SP00054','35*65两轨4扇','',NULL,NULL,'方',380.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (55,56,'SP00055','35*65三轨3扇','',NULL,NULL,'方',152.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (56,57,'SP00056','35*65三轨6扇','',NULL,NULL,'方',153.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (57,58,'SP00057','35*65单轨推拉','',NULL,NULL,'方',154.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (58,59,'SP00058','35*65单轨吊趟','',NULL,NULL,'方',155.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (59,60,'SP00059','35*65两轨吊趟','',NULL,NULL,'方',156.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (60,61,'SP00060','35*65三联动','',NULL,NULL,'方',157.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (61,62,'SP00061','40*65两轨2扇','',NULL,NULL,'方',158.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (62,63,'SP00062','40*65两轨4扇','',NULL,NULL,'方',159.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (63,64,'SP00063','40*65三轨3扇','',NULL,NULL,'方',160.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (64,65,'SP00064','40*65三轨6扇','',NULL,NULL,'方',161.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (65,66,'SP00065','40*65单轨推拉','',NULL,NULL,'方',162.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (66,67,'SP00066','40*65单轨吊趟','',NULL,NULL,'方',163.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (67,68,'SP00067','40*65两轨吊趟','',NULL,NULL,'方',164.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (68,69,'SP00068','40*65三联动','',NULL,NULL,'方',165.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (69,70,'SP00069','至尊两轨2扇','',NULL,NULL,'方',166.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (70,71,'SP00070','至尊两轨4扇','',NULL,NULL,'方',167.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (71,72,'SP00071','至尊三轨3扇','',NULL,NULL,'方',168.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (72,73,'SP00072','至尊三轨6扇','',NULL,NULL,'方',169.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (73,74,'SP00073','至尊单轨推拉','',NULL,NULL,'方',170.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (74,75,'SP00074','至尊单轨吊趟','',NULL,NULL,'方',171.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (75,76,'SP00075','至尊两轨吊趟','',NULL,NULL,'方',172.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (76,77,'SP00076','35*16极窄两轨2扇','',NULL,NULL,'方',173.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (77,78,'SP00077','35*16极窄两轨4扇','',NULL,NULL,'方',174.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (78,79,'SP00078','35*16极窄单轨推拉','',NULL,NULL,'方',175.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (79,80,'SP00079','35*16极窄单轨吊趟','',NULL,NULL,'方',176.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (80,81,'SP00080','35*16极窄两轨吊趟','',NULL,NULL,'方',177.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (81,82,'SP00081','35*16极窄三联动','',NULL,NULL,'方',178.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (82,83,'SP00082','35*16极窄吊轨三联动','',NULL,NULL,'方',178.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (83,84,'SP00083','110断桥平开窗','',NULL,NULL,'方',550.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (84,85,'SP00084','110断桥推拉窗','',NULL,NULL,'方',600.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (85,86,'SP00085','130六轨推拉窗','',NULL,NULL,'方',700.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (86,87,'SP00086','包边','',NULL,'单包','米',40.00,NULL,'linear',NULL,0.0000,0.00,0.0000,'',NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-21 09:51:37',3,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (87,87,'SP00087','包边','',NULL,'双包','米',178.00,NULL,'linear',NULL,0.0000,0.00,0.0000,'',NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-21 09:51:45',3,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (88,88,'SP00088','固定','',NULL,NULL,'方',178.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (89,89,'SP00089','开启','',NULL,NULL,'套',650.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (90,90,'SP00090','转通','',NULL,NULL,'米',178.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (91,91,'SP00091','方通','',NULL,NULL,'米',178.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (92,92,'SP00092','碳晶橡木门','',NULL,NULL,'套',178.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (93,93,'SP00093','铝合金房门','',NULL,NULL,'套',178.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (94,94,'SP00094','锌合金房门','',NULL,NULL,'套',178.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (95,95,'SP00095','不锈钢门','',NULL,NULL,'套',178.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (96,96,'SP00096','65门套','',NULL,NULL,'米',178.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (97,97,'SP00097','48门套','',NULL,NULL,'米',178.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (98,98,'SP00098','83门套','',NULL,NULL,'米',178.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (99,102,'SP00099','35*60现货','',NULL,NULL,'套',350.00,NULL,NULL,NULL,0.0000,0.00,0.0000,NULL,NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',NULL,0);
MERGE INTO commodity (`id`, `category_id`, `code`, `name`, `model`, `material`, `product_type`, `unit`, `default_price`, `cost_price`, `pricing_rule`, `warehouse_loc`, `initial_qty`, `initial_cost`, `current_qty`, `remark`, `alert_high`, `alert_mid`, `alert_low`, `is_show`, `create_time`, `update_time`, `formula_id`, `deleted`) KEY(id) VALUES (100,103,'SP00100','35*70现货','',NULL,'','套',NULL,5.00,'count',NULL,0.0000,0.00,0.0000,'',NULL,NULL,NULL,1,'2026-05-17 00:34:47','2026-05-17 23:28:59',2,0);

-- commodity_category
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (1,'category1',0,'2026-05-16 20:52:52',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (2,'glass',0,'2026-05-16 21:07:16',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (3,'优惠价格',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (4,'新10号平开门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (5,'新12平开门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (6,'35*48中窄平开门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (7,'35*65中窄平开门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (8,'35*16极窄平开门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (9,'至尊平开门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (10,'40*65中窄平开门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (11,'新10号双包套',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (12,'新12双包套',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (13,'35*48中窄双包套',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (14,'35*65中窄双包套',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (15,'35*16极窄双包套',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (16,'至尊双包套',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (17,'40*65中窄双包套',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (18,'新10号对开门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (19,'新12对开门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (20,'35*48中窄对开门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (21,'35*65中窄对开门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (22,'35*16极窄对开门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (23,'至尊对开门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (24,'40*65中窄对开门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (25,'新10号子母门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (26,'新12子母门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (27,'35*48中窄子母门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (28,'35*65中窄子母门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (29,'35*16极窄子母门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (30,'至尊子母门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (31,'40*65中窄子母门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (32,'新10号两轨2扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (33,'新10号两轨4扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (34,'新10号三轨3扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (35,'新10号三轨6扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (36,'新10号单轨推拉',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (37,'新10号单轨吊趟',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (38,'新10号两轨吊趟',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (39,'新12号两轨2扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (40,'新12号两轨4扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (41,'新12号三轨3扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (42,'新12号三轨6扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (43,'新12号单轨推拉',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (44,'新12号单轨吊趟',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (45,'新12号两轨吊趟',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (46,'35*48两轨2扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (47,'35*48两轨4扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (48,'35*48三轨3扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (49,'35*48三轨6扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (50,'35*48单轨推拉',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (51,'35*48单轨吊趟',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (52,'35*48两轨吊趟',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (53,'35*48三联动',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (54,'35*65两轨2扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (55,'35*65两轨4扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (56,'35*65三轨3扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (57,'35*65三轨6扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (58,'35*65单轨推拉',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (59,'35*65单轨吊趟',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (60,'35*65两轨吊趟',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (61,'35*65三联动',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (62,'40*65两轨2扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (63,'40*65两轨4扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (64,'40*65三轨3扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (65,'40*65三轨6扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (66,'40*65单轨推拉',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (67,'40*65单轨吊趟',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (68,'40*65两轨吊趟',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (69,'40*65三联动',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (70,'至尊两轨2扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (71,'至尊两轨4扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (72,'至尊三轨3扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (73,'至尊三轨6扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (74,'至尊单轨推拉',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (75,'至尊单轨吊趟',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (76,'至尊两轨吊趟',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (77,'35*16极窄两轨2扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (78,'35*16极窄两轨4扇',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (79,'35*16极窄单轨推拉',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (80,'35*16极窄单轨吊趟',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (81,'35*16极窄两轨吊趟',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (82,'35*16极窄三联动',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (83,'35*16极窄吊轨三联动',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (84,'110断桥平开窗',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (85,'110断桥推拉窗',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (86,'130六轨推拉窗',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (87,'包边',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (88,'固定',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (89,'开启',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (90,'转通',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (91,'方通',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (92,'碳晶橡木门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (93,'铝合金房门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (94,'锌合金房门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (95,'不锈钢门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (96,'65门套',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (97,'48门套',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (98,'83门套',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (99,'欠款',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (100,'纱网',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (101,'人工',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (102,'35*60现货',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (103,'35*70现货',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (104,'辅材',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (105,'普通推拉窗',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (106,'运费',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (107,'卫浴门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (108,'不锈钢防盗网',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (109,'推拉纱网',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (110,'修玻璃',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (111,'单推',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (112,'单推上下轨',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (113,'玻璃加厚',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (114,'楼顶门',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (115,'50平开窗',0,'2026-05-17 00:34:47',0);
MERGE INTO commodity_category (`id`, `name`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (116,'拉爆螺丝',0,'2026-05-17 00:34:47',0);

-- customer
MERGE INTO customer (`id`, `name`, `contact`, `phone`, `address`, `remark`, `create_time`, `update_time`, `is_starred`, `deleted`) KEY(id) VALUES (1, '示例客户01', '张三', '13800000001', '示例省示例市示例区示例路1号', '示例客户', '2026-01-01 10:00:00', '2026-01-01 10:00:00', 0, 0);
MERGE INTO customer (`id`, `name`, `contact`, `phone`, `address`, `remark`, `create_time`, `update_time`, `is_starred`, `deleted`) KEY(id) VALUES (2, '示例客户02', '李四', '13800000002', '示例省示例市示例区示例路2号', '示例客户', '2026-01-01 10:01:00', '2026-01-01 10:01:00', 1, 0);
MERGE INTO customer (`id`, `name`, `contact`, `phone`, `address`, `remark`, `create_time`, `update_time`, `is_starred`, `deleted`) KEY(id) VALUES (3, '示例客户03', '王五', '13800000003', '示例省示例市示例区示例路3号', '示例客户', '2026-01-01 10:02:00', '2026-01-01 10:02:00', 0, 0);
MERGE INTO customer (`id`, `name`, `contact`, `phone`, `address`, `remark`, `create_time`, `update_time`, `is_starred`, `deleted`) KEY(id) VALUES (4, '示例客户04', '赵六', '13800000004', '示例省示例市示例区示例路4号', '示例客户', '2026-01-01 10:03:00', '2026-01-01 10:03:00', 1, 0);
MERGE INTO customer (`id`, `name`, `contact`, `phone`, `address`, `remark`, `create_time`, `update_time`, `is_starred`, `deleted`) KEY(id) VALUES (5, '示例客户05', '钱七', '13800000005', '示例省示例市示例区示例路5号', '示例客户', '2026-01-01 10:04:00', '2026-01-01 10:04:00', 0, 0);
MERGE INTO customer (`id`, `name`, `contact`, `phone`, `address`, `remark`, `create_time`, `update_time`, `is_starred`, `deleted`) KEY(id) VALUES (6, '示例客户06', '孙八', '13800000006', '示例省示例市示例区示例路6号', '示例客户', '2026-01-01 10:05:00', '2026-01-01 10:05:00', 1, 0);
MERGE INTO customer (`id`, `name`, `contact`, `phone`, `address`, `remark`, `create_time`, `update_time`, `is_starred`, `deleted`) KEY(id) VALUES (7, '示例客户07', '周九', '13800000007', '示例省示例市示例区示例路7号', '示例客户', '2026-01-01 10:06:00', '2026-01-01 10:06:00', 0, 0);
MERGE INTO customer (`id`, `name`, `contact`, `phone`, `address`, `remark`, `create_time`, `update_time`, `is_starred`, `deleted`) KEY(id) VALUES (8, '示例客户08', '吴十', '13800000008', '示例省示例市示例区示例路8号', '示例客户', '2026-01-01 10:07:00', '2026-01-01 10:07:00', 1, 0);

-- enquiry
MERGE INTO enquiry (`id`, `name`, `phone`, `content`, `is_read`, `need_measure`, `budget`, `is_measured`, `is_starred`, `is_completed`, `create_time`, `remark`, `deleted`) KEY(id) VALUES (1, '示例咨询客户A', '13700000001', '房屋类型: 示例小区新房\n窗户数量: 4樘\n门窗材质: 断桥铝\n期望风格: 简约\n补充说明: 示例需求', 0, 1, '1万以下', 0, 0, 0, '2026-01-02 09:00:00', NULL, 0);
MERGE INTO enquiry (`id`, `name`, `phone`, `content`, `is_read`, `need_measure`, `budget`, `is_measured`, `is_starred`, `is_completed`, `create_time`, `remark`, `deleted`) KEY(id) VALUES (2, '示例咨询客户B', '13700000002', '房屋类型: 示例小区新房\n窗户数量: 4樘\n门窗材质: 断桥铝\n期望风格: 简约\n补充说明: 示例需求', 1, 1, '1-2万', 0, 0, 0, '2026-01-03 09:00:00', NULL, 0);
MERGE INTO enquiry (`id`, `name`, `phone`, `content`, `is_read`, `need_measure`, `budget`, `is_measured`, `is_starred`, `is_completed`, `create_time`, `remark`, `deleted`) KEY(id) VALUES (3, '示例咨询客户C', '13700000003', '房屋类型: 示例小区新房\n窗户数量: 4樘\n门窗材质: 断桥铝\n期望风格: 简约\n补充说明: 示例需求', 1, 1, '2-5万', 0, 0, 0, '2026-01-04 09:00:00', NULL, 0);

-- order_sequence
MERGE INTO order_sequence (`id`, `seq_type`, `prefix`, `current_seq`, `last_date`, `create_time`, `update_time`) KEY(id) VALUES (1, 'sale', 'XS', 8, '2026-01-08', '2026-01-01 10:00:00', '2026-01-01 10:00:00');
MERGE INTO order_sequence (`id`, `seq_type`, `prefix`, `current_seq`, `last_date`, `create_time`, `update_time`) KEY(id) VALUES (2, 'purchase', 'CG', 1, '2026-01-05', '2026-01-01 10:00:00', '2026-01-01 10:00:00');
MERGE INTO order_sequence (`id`, `seq_type`, `prefix`, `current_seq`, `last_date`, `create_time`, `update_time`) KEY(id) VALUES (3, 'stock_in', 'RK', 1, '2026-01-05', '2026-01-01 10:00:00', '2026-01-01 10:00:00');
MERGE INTO order_sequence (`id`, `seq_type`, `prefix`, `current_seq`, `last_date`, `create_time`, `update_time`) KEY(id) VALUES (4, 'stock_out', 'CK', 1, '2026-01-06', '2026-01-01 10:00:00', '2026-01-01 10:00:00');
MERGE INTO order_sequence (`id`, `seq_type`, `prefix`, `current_seq`, `last_date`, `create_time`, `update_time`) KEY(id) VALUES (5, 'sale_return', 'XT', 1, '2026-01-10', '2026-01-01 10:00:00', '2026-01-01 10:00:00');
MERGE INTO order_sequence (`id`, `seq_type`, `prefix`, `current_seq`, `last_date`, `create_time`, `update_time`) KEY(id) VALUES (6, 'purchase_return', 'CT', 1, '2026-01-12', '2026-01-01 10:00:00', '2026-01-01 10:00:00');
MERGE INTO order_sequence (`id`, `seq_type`, `prefix`, `current_seq`, `last_date`, `create_time`, `update_time`) KEY(id) VALUES (14, 'presale', 'YS', 0, '2026-01-08', '2026-01-01 10:00:00', '2026-01-01 10:00:00');

-- sale_order
MERGE INTO sale_order (`id`, `order_no`, `order_type`, `customer_id`, `customer_name`, `customer_phone`, `customer_address`, `order_date`, `total_amount`, `deposit`, `paid_amount`, `is_cleared`, `status`, `remark`, `notice`, `window_area`, `window_count`, `door_area`, `door_count`, `create_time`, `update_time`, `hidden_product_types`, `deleted`) KEY(id) VALUES (1, 'XS20260101001', 'sale', 1, '示例客户01', '13800000001', '示例省示例市示例区示例路1号', '2026-01-01', 3800.00, 1140.00, 0.00, 0, 'pending', '示例订单', NULL, 13.50, 5, 0.00, 0, '2026-01-01 10:00:00', '2026-01-01 10:00:00', NULL, 0);
MERGE INTO sale_order (`id`, `order_no`, `order_type`, `customer_id`, `customer_name`, `customer_phone`, `customer_address`, `order_date`, `total_amount`, `deposit`, `paid_amount`, `is_cleared`, `status`, `remark`, `notice`, `window_area`, `window_count`, `door_area`, `door_count`, `create_time`, `update_time`, `hidden_product_types`, `deleted`) KEY(id) VALUES (2, 'XS20260102001', 'sale', 2, '示例客户02', '13800000002', '示例省示例市示例区示例路2号', '2026-01-02', 5600.00, 1680.00, 1680.00, 0, 'pending', '示例订单', NULL, 25.20, 7, 0.00, 0, '2026-01-02 10:00:00', '2026-01-02 10:00:00', NULL, 0);
MERGE INTO sale_order (`id`, `order_no`, `order_type`, `customer_id`, `customer_name`, `customer_phone`, `customer_address`, `order_date`, `total_amount`, `deposit`, `paid_amount`, `is_cleared`, `status`, `remark`, `notice`, `window_area`, `window_count`, `door_area`, `door_count`, `create_time`, `update_time`, `hidden_product_types`, `deleted`) KEY(id) VALUES (3, 'XS20260103001', 'sale', 3, '示例客户03', '13800000003', '示例省示例市示例区示例路3号', '2026-01-03', 7200.00, 2160.00, 3600.00, 0, 'pending', '示例订单', NULL, 57.60, 8, 0.00, 0, '2026-01-03 10:00:00', '2026-01-03 10:00:00', NULL, 0);
MERGE INTO sale_order (`id`, `order_no`, `order_type`, `customer_id`, `customer_name`, `customer_phone`, `customer_address`, `order_date`, `total_amount`, `deposit`, `paid_amount`, `is_cleared`, `status`, `remark`, `notice`, `window_area`, `window_count`, `door_area`, `door_count`, `create_time`, `update_time`, `hidden_product_types`, `deleted`) KEY(id) VALUES (4, 'XS20260104001', 'sale', 4, '示例客户04', '13800000004', '示例省示例市示例区示例路4号', '2026-01-04', 2600.00, 780.00, 2600.00, 1, 'completed', '示例订单', NULL, 0.00, 0, 4.20, 1, '2026-01-04 10:00:00', '2026-01-04 10:00:00', NULL, 0);
MERGE INTO sale_order (`id`, `order_no`, `order_type`, `customer_id`, `customer_name`, `customer_phone`, `customer_address`, `order_date`, `total_amount`, `deposit`, `paid_amount`, `is_cleared`, `status`, `remark`, `notice`, `window_area`, `window_count`, `door_area`, `door_count`, `create_time`, `update_time`, `hidden_product_types`, `deleted`) KEY(id) VALUES (5, 'XS20260105001', 'sale', 5, '示例客户05', '13800000005', '示例省示例市示例区示例路5号', '2026-01-05', 9800.00, 2940.00, 2940.00, 0, 'pending', '示例订单', NULL, 25.20, 14, 0.00, 0, '2026-01-05 10:00:00', '2026-01-05 10:00:00', NULL, 0);
MERGE INTO sale_order (`id`, `order_no`, `order_type`, `customer_id`, `customer_name`, `customer_phone`, `customer_address`, `order_date`, `total_amount`, `deposit`, `paid_amount`, `is_cleared`, `status`, `remark`, `notice`, `window_area`, `window_count`, `door_area`, `door_count`, `create_time`, `update_time`, `hidden_product_types`, `deleted`) KEY(id) VALUES (6, 'XS20260106001', 'sale', 6, '示例客户06', '13800000006', '示例省示例市示例区示例路6号', '2026-01-06', 4500.00, 1350.00, 2250.00, 0, 'pending', '示例订单', NULL, 0.00, 0, 5.67, 1, '2026-01-06 10:00:00', '2026-01-06 10:00:00', NULL, 0);
MERGE INTO sale_order (`id`, `order_no`, `order_type`, `customer_id`, `customer_name`, `customer_phone`, `customer_address`, `order_date`, `total_amount`, `deposit`, `paid_amount`, `is_cleared`, `status`, `remark`, `notice`, `window_area`, `window_count`, `door_area`, `door_count`, `create_time`, `update_time`, `hidden_product_types`, `deleted`) KEY(id) VALUES (7, 'XS20260107001', 'sale', 7, '示例客户07', '13800000007', '示例省示例市示例区示例路7号', '2026-01-07', 6300.00, 1890.00, 0.00, 0, 'pending', '示例订单', NULL, 24.30, 9, 0.00, 0, '2026-01-07 10:00:00', '2026-01-07 10:00:00', NULL, 0);
MERGE INTO sale_order (`id`, `order_no`, `order_type`, `customer_id`, `customer_name`, `customer_phone`, `customer_address`, `order_date`, `total_amount`, `deposit`, `paid_amount`, `is_cleared`, `status`, `remark`, `notice`, `window_area`, `window_count`, `door_area`, `door_count`, `create_time`, `update_time`, `hidden_product_types`, `deleted`) KEY(id) VALUES (8, 'XS20260108001', 'sale', 8, '示例客户08', '13800000008', '示例省示例市示例区示例路8号', '2026-01-08', 3100.00, 930.00, 3100.00, 1, 'completed', '示例订单', NULL, 0.00, 0, 2.42, 1, '2026-01-08 10:00:00', '2026-01-08 10:00:00', NULL, 0);

-- sale_order_item
MERGE INTO sale_order_item (`id`, `order_id`, `commodity_id`, `product_name`, `series`, `color`, `product_type`, `unit`, `width`, `height`, `wall_thickness`, `glass_type`, `lock_position`, `door_count`, `fang_count`, `diaojiao`, `tao_count`, `quantity`, `unit_price`, `amount`, `cost`, `material_cost`, `labor_cost`, `accessory_cost`, `formula_id`, `extra_fee`, `remark`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (1, 1, NULL, '断桥铝平开窗', '70系列', '示例灰', '断桥铝平开窗', '樘', 1500.00, 1800.00, 1.80, '5+20A+5 中空钢化', '居中', 1, 2.00, 0, 1, 5, 760.00, 3800.00, 532.00, 340.48, 117.04, 74.48, NULL, 0.00, '示例明细', 1, '2026-01-01 10:00:00', 0);
MERGE INTO sale_order_item (`id`, `order_id`, `commodity_id`, `product_name`, `series`, `color`, `product_type`, `unit`, `width`, `height`, `wall_thickness`, `glass_type`, `lock_position`, `door_count`, `fang_count`, `diaojiao`, `tao_count`, `quantity`, `unit_price`, `amount`, `cost`, `material_cost`, `labor_cost`, `accessory_cost`, `formula_id`, `extra_fee`, `remark`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (2, 2, NULL, '断桥铝推拉窗', '90系列', '示例灰', '断桥铝推拉窗', '樘', 1800.00, 2000.00, 1.80, '5+20A+5 中空钢化', '居中', 1, 2.00, 0, 1, 7, 800.00, 5600.00, 560.00, 358.40, 123.20, 78.40, NULL, 0.00, '示例明细', 1, '2026-01-02 10:00:00', 0);
MERGE INTO sale_order_item (`id`, `order_id`, `commodity_id`, `product_name`, `series`, `color`, `product_type`, `unit`, `width`, `height`, `wall_thickness`, `glass_type`, `lock_position`, `door_count`, `fang_count`, `diaojiao`, `tao_count`, `quantity`, `unit_price`, `amount`, `cost`, `material_cost`, `labor_cost`, `accessory_cost`, `formula_id`, `extra_fee`, `remark`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (3, 3, NULL, '阳台封窗', '100系列', '示例灰', '阳台封窗', '平方米', 3000.00, 2400.00, 1.80, '5+20A+5 中空钢化', '居中', 1, 2.00, 0, 1, 8, 900.00, 7200.00, 630.00, 403.20, 138.60, 88.20, NULL, 0.00, '示例明细', 1, '2026-01-03 10:00:00', 0);
MERGE INTO sale_order_item (`id`, `order_id`, `commodity_id`, `product_name`, `series`, `color`, `product_type`, `unit`, `width`, `height`, `wall_thickness`, `glass_type`, `lock_position`, `door_count`, `fang_count`, `diaojiao`, `tao_count`, `quantity`, `unit_price`, `amount`, `cost`, `material_cost`, `labor_cost`, `accessory_cost`, `formula_id`, `extra_fee`, `remark`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (4, 4, NULL, '不锈钢防盗门', '甲级', '示例灰', '不锈钢防盗门', '樘', 1000.00, 2100.00, 1.80, '5+20A+5 中空钢化', '居中', 1, 1.00, 0, 1, 2, 1300.00, 2600.00, 910.00, 582.40, 200.20, 127.40, NULL, 0.00, '示例明细', 1, '2026-01-04 10:00:00', 0);
MERGE INTO sale_order_item (`id`, `order_id`, `commodity_id`, `product_name`, `series`, `color`, `product_type`, `unit`, `width`, `height`, `wall_thickness`, `glass_type`, `lock_position`, `door_count`, `fang_count`, `diaojiao`, `tao_count`, `quantity`, `unit_price`, `amount`, `cost`, `material_cost`, `labor_cost`, `accessory_cost`, `formula_id`, `extra_fee`, `remark`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (5, 5, NULL, '断桥铝平开窗', '70系列', '示例灰', '断桥铝平开窗', '樘', 1200.00, 1500.00, 1.80, '5+20A+5 中空钢化', '居中', 1, 2.00, 0, 1, 14, 700.00, 9800.00, 490.00, 313.60, 107.80, 68.60, NULL, 0.00, '示例明细', 1, '2026-01-05 10:00:00', 0);
MERGE INTO sale_order_item (`id`, `order_id`, `commodity_id`, `product_name`, `series`, `color`, `product_type`, `unit`, `width`, `height`, `wall_thickness`, `glass_type`, `lock_position`, `door_count`, `fang_count`, `diaojiao`, `tao_count`, `quantity`, `unit_price`, `amount`, `cost`, `material_cost`, `labor_cost`, `accessory_cost`, `formula_id`, `extra_fee`, `remark`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (6, 6, NULL, '实木复合门', '标准', '示例灰', '实木复合门', '樘', 900.00, 2100.00, 1.80, '5+20A+5 中空钢化', '居中', 1, 1.00, 0, 1, 3, 1500.00, 4500.00, 1050.00, 672.00, 231.00, 147.00, NULL, 0.00, '示例明细', 1, '2026-01-06 10:00:00', 0);
MERGE INTO sale_order_item (`id`, `order_id`, `commodity_id`, `product_name`, `series`, `color`, `product_type`, `unit`, `width`, `height`, `wall_thickness`, `glass_type`, `lock_position`, `door_count`, `fang_count`, `diaojiao`, `tao_count`, `quantity`, `unit_price`, `amount`, `cost`, `material_cost`, `labor_cost`, `accessory_cost`, `formula_id`, `extra_fee`, `remark`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (7, 7, NULL, '断桥铝推拉窗', '90系列', '示例灰', '断桥铝推拉窗', '樘', 1500.00, 1800.00, 1.80, '5+20A+5 中空钢化', '居中', 1, 2.00, 0, 1, 9, 700.00, 6300.00, 490.00, 313.60, 107.80, 68.60, NULL, 0.00, '示例明细', 1, '2026-01-07 10:00:00', 0);
MERGE INTO sale_order_item (`id`, `order_id`, `commodity_id`, `product_name`, `series`, `color`, `product_type`, `unit`, `width`, `height`, `wall_thickness`, `glass_type`, `lock_position`, `door_count`, `fang_count`, `diaojiao`, `tao_count`, `quantity`, `unit_price`, `amount`, `cost`, `material_cost`, `labor_cost`, `accessory_cost`, `formula_id`, `extra_fee`, `remark`, `sort`, `create_time`, `deleted`) KEY(id) VALUES (8, 8, NULL, '不锈钢防盗门', '甲级', '示例灰', '不锈钢防盗门', '樘', 1100.00, 2200.00, 1.80, '5+20A+5 中空钢化', '居中', 1, 1.00, 0, 1, 1, 3100.00, 3100.00, 2170.00, 1388.80, 477.40, 303.80, NULL, 0.00, '示例明细', 1, '2026-01-08 10:00:00', 0);

-- payment
MERGE INTO payment (`id`, `order_id`, `order_no`, `payment_date`, `type`, `amount`, `party_name`, `remark`, `order_type`, `create_time`, `after_sale_id`, `deleted`) KEY(id) VALUES (1, 1, 'XS20260101001', '2026-01-02', 'receipt', 0.00, '示例客户01', NULL, 'sale', '2026-01-02 11:00:00', NULL, 0);
MERGE INTO payment (`id`, `order_id`, `order_no`, `payment_date`, `type`, `amount`, `party_name`, `remark`, `order_type`, `create_time`, `after_sale_id`, `deleted`) KEY(id) VALUES (2, 2, 'XS20260102001', '2026-01-03', 'receipt', 1680.00, '示例客户02', NULL, 'sale', '2026-01-03 11:00:00', NULL, 0);
MERGE INTO payment (`id`, `order_id`, `order_no`, `payment_date`, `type`, `amount`, `party_name`, `remark`, `order_type`, `create_time`, `after_sale_id`, `deleted`) KEY(id) VALUES (3, 3, 'XS20260103001', '2026-01-04', 'receipt', 3600.00, '示例客户03', NULL, 'sale', '2026-01-04 11:00:00', NULL, 0);
MERGE INTO payment (`id`, `order_id`, `order_no`, `payment_date`, `type`, `amount`, `party_name`, `remark`, `order_type`, `create_time`, `after_sale_id`, `deleted`) KEY(id) VALUES (4, 4, 'XS20260104001', '2026-01-05', 'receipt', 2600.00, '示例客户04', NULL, 'sale', '2026-01-05 11:00:00', NULL, 0);
MERGE INTO payment (`id`, `order_id`, `order_no`, `payment_date`, `type`, `amount`, `party_name`, `remark`, `order_type`, `create_time`, `after_sale_id`, `deleted`) KEY(id) VALUES (5, 5, 'XS20260105001', '2026-01-06', 'receipt', 2940.00, '示例客户05', NULL, 'sale', '2026-01-06 11:00:00', NULL, 0);
MERGE INTO payment (`id`, `order_id`, `order_no`, `payment_date`, `type`, `amount`, `party_name`, `remark`, `order_type`, `create_time`, `after_sale_id`, `deleted`) KEY(id) VALUES (6, 6, 'XS20260106001', '2026-01-07', 'receipt', 2250.00, '示例客户06', NULL, 'sale', '2026-01-07 11:00:00', NULL, 0);
MERGE INTO payment (`id`, `order_id`, `order_no`, `payment_date`, `type`, `amount`, `party_name`, `remark`, `order_type`, `create_time`, `after_sale_id`, `deleted`) KEY(id) VALUES (7, 7, 'XS20260107001', '2026-01-08', 'receipt', 0.00, '示例客户07', NULL, 'sale', '2026-01-08 11:00:00', NULL, 0);
MERGE INTO payment (`id`, `order_id`, `order_no`, `payment_date`, `type`, `amount`, `party_name`, `remark`, `order_type`, `create_time`, `after_sale_id`, `deleted`) KEY(id) VALUES (8, 8, 'XS20260108001', '2026-01-09', 'receipt', 3100.00, '示例客户08', NULL, 'sale', '2026-01-09 11:00:00', NULL, 0);
MERGE INTO payment (`id`, `order_id`, `order_no`, `payment_date`, `type`, `amount`, `party_name`, `remark`, `order_type`, `create_time`, `after_sale_id`, `deleted`) KEY(id) VALUES (9, NULL, NULL, '2026-01-05', 'payment', 2000.00, '示例供应商01', '示例采购付款', 'purchase', '2026-01-05 15:00:00', NULL, 0);

-- pricing_formula
MERGE INTO pricing_formula (`id`, `name`, `unit`, `formula`, `parameters`, `description`, `sort`, `create_time`, `update_time`, `deleted`) KEY(id) VALUES (2,'樘数计价','套','樘数','[{\"name\": \"樘数\", \"unit\": \"樘\", \"label\": \"樘数\", \"default\": \"1\"}]','按樘数计算价格',2,'2026-05-18 00:30:53','2026-05-18 00:59:59',0);
MERGE INTO pricing_formula (`id`, `name`, `unit`, `formula`, `parameters`, `description`, `sort`, `create_time`, `update_time`, `deleted`) KEY(id) VALUES (3,'包边计价','米','(宽+高)*樘数/1000','[{\"name\": \"宽\", \"unit\": \"mm\", \"label\": \"宽(mm)\", \"default\": \"\"}, {\"name\": \"高\", \"unit\": \"mm\", \"label\": \"高(mm)\", \"default\": \"\"}, {\"name\": \"樘数\", \"unit\": \"樘\", \"label\": \"樘数\", \"default\": \"1\"}]','按延米计算价格',3,'2026-05-18 00:30:53','2026-05-23 12:45:30',0);
MERGE INTO pricing_formula (`id`, `name`, `unit`, `formula`, `parameters`, `description`, `sort`, `create_time`, `update_time`, `deleted`) KEY(id) VALUES (4,'面积计价','方','宽*高*樘数/1000000','[{\"name\": \"宽\", \"unit\": \"mm\", \"label\": \"宽(mm)\", \"default\": \"1000\"}, {\"name\": \"高\", \"unit\": \"mm\", \"label\": \"高(mm)\", \"default\": \"1000\"}, {\"name\": \"樘数\", \"unit\": \"樘\", \"label\": \"樘数\", \"default\": \"1\"}]','按面积计算价格',4,'2026-05-18 00:30:53','2026-05-19 17:22:55',0);
MERGE INTO pricing_formula (`id`, `name`, `unit`, `formula`, `parameters`, `description`, `sort`, `create_time`, `update_time`, `deleted`) KEY(id) VALUES (5,'面积计价(sqm)','sqm','宽*高*樘数/1000000','[{\"name\": \"宽\", \"unit\": \"mm\", \"label\": \"宽(mm)\", \"default\": \"1000\"}, {\"name\": \"高\", \"unit\": \"mm\", \"label\": \"高(mm)\", \"default\": \"1000\"}, {\"name\": \"樘数\", \"unit\": \"樘\", \"label\": \"樘数\", \"default\": \"1\"}]','按面积计算价格',5,'2026-05-18 00:30:53','2026-05-19 17:22:55',0);
MERGE INTO pricing_formula (`id`, `name`, `unit`, `formula`, `parameters`, `description`, `sort`, `create_time`, `update_time`, `deleted`) KEY(id) VALUES (6,'延米计价','米','(宽+高)*2/1000','[{\"name\": \"宽\", \"unit\": \"mm\", \"label\": \"宽(mm)\", \"default\": \"\"}, {\"name\": \"高\", \"unit\": \"mm\", \"label\": \"高(mm)\", \"default\": \"\"}]','按单樘延米(周长/2)计算价格',6,'2026-08-01 00:00:00','2026-08-01 00:00:00',0);
MERGE INTO pricing_formula (`id`, `name`, `unit`, `formula`, `parameters`, `description`, `sort`, `create_time`, `update_time`, `deleted`) KEY(id) VALUES (7,'吊脚计价','吊脚','高/1000','[{\"name\": \"高\", \"unit\": \"mm\", \"label\": \"高(mm)\", \"default\": \"\"}]','按高度(吊脚)计算价格',7,'2026-08-01 00:00:00','2026-08-01 00:00:00',0);

-- print_setting
MERGE INTO print_setting (`id`, `config`, `update_time`, `type`) KEY(id) VALUES (1,'{\"columns\": [{\"key\": \"productName\", \"label\": \"商品名称\", \"visible\": true}, {\"key\": \"color\", \"label\": \"颜色\", \"visible\": true}, {\"key\": \"width\", \"label\": \"宽(mm)\", \"visible\": true}, {\"key\": \"height\", \"label\": \"高(mm)\", \"visible\": true}, {\"key\": \"wallThickness\", \"label\": \"墙厚(mm)\", \"visible\": true}, {\"key\": \"glassType\", \"label\": \"玻璃款式\", \"visible\": true}, {\"key\": \"lockPosition\", \"label\": \"锁位\", \"visible\": true}, {\"key\": \"diaojiao\", \"label\": \"吊脚\", \"visible\": true}, {\"key\": \"doorCount\", \"label\": \"樘数\", \"visible\": true}, {\"key\": \"fangCount\", \"label\": \"面积(㎡)\", \"visible\": true}, {\"key\": \"productType\", \"label\": \"细目\", \"visible\": true}, {\"key\": \"unitPrice\", \"label\": \"单价\", \"visible\": true}, {\"key\": \"remark\", \"label\": \"备注\", \"visible\": true}], \"company\": {\"logo\": \"\", \"name\": \"示例门窗\", \"phone\": \"\", \"address\": \"\"}, \"margins\": {\"top\": 15, \"left\": 12, \"right\": 12, \"bottom\": 15}, \"fontSize\": 11, \"marginTop\": 10, \"footerText\": \"\", \"marginLeft\": 8, \"showFooter\": true, \"companyName\": \"\", \"creatorName\": \"示例制单人\", \"marginRight\": 8, \"orientation\": \"portrait\", \"rowsPerPage\": 20, \"showSummary\": true, \"columnWidths\": {\"color\": 50, \"index\": 36, \"width\": 40, \"amount\": 60, \"height\": 40, \"remark\": 70, \"series\": 60, \"diaojiao\": 40, \"doorCount\": 40, \"fangCount\": 48, \"glassType\": 50, \"unitPrice\": 56, \"productName\": 100, \"productType\": 60, \"lockPosition\": 36, \"wallThickness\": 40}, \"creatorPhone\": \"13800000000\", \"marginBottom\": 10, \"showTotalRow\": true, \"customPresets\": [{\"name\": \"全-横向\", \"config\": {\"columns\": [{\"key\": \"index\", \"label\": \"序号\", \"visible\": true}, {\"key\": \"productName\", \"label\": \"商品名称\", \"visible\": true}, {\"key\": \"series\", \"label\": \"系列\", \"visible\": true}, {\"key\": \"color\", \"label\": \"颜色\", \"visible\": true}, {\"key\": \"width\", \"label\": \"宽(mm)\", \"visible\": true}, {\"key\": \"height\", \"label\": \"高(mm)\", \"visible\": true}, {\"key\": \"wallThickness\", \"label\": \"墙厚(mm)\", \"visible\": true}, {\"key\": \"lockPosition\", \"label\": \"锁位\", \"visible\": true}, {\"key\": \"diaojiao\", \"label\": \"吊脚\", \"visible\": true}, {\"key\": \"doorCount\", \"label\": \"樘数\", \"visible\": true}, {\"key\": \"fangCount\", \"label\": \"面积(㎡)\", \"visible\": true}, {\"key\": \"productType\", \"label\": \"细目\", \"visible\": true}, {\"key\": \"unitPrice\", \"label\": \"单价\", \"visible\": true}, {\"key\": \"amount\", \"label\": \"金额\", \"visible\": true}, {\"key\": \"remark\", \"label\": \"备注\", \"visible\": true}], \"fontSize\": \"medium\", \"marginTop\": 10, \"footerText\": \"\", \"marginLeft\": 8, \"showFooter\": true, \"marginRight\": 8, \"orientation\": \"landscape\", \"showSummary\": true, \"columnWidths\": {\"color\": 50, \"index\": 36, \"width\": 40, \"amount\": 60, \"height\": 40, \"remark\": 70, \"series\": 74, \"diaojiao\": 40, \"doorCount\": 40, \"fangCount\": 48, \"unitPrice\": 56, \"productName\": 71, \"productType\": 60, \"lockPosition\": 36, \"wallThickness\": 40}, \"marginBottom\": 10, \"showTotalRow\": true, \"sectionStyles\": {\"info\": {\"bold\": false, \"sizeOffset\": 0}, \"terms\": {\"bold\": false, \"sizeOffset\": 0}, \"footer\": {\"bold\": false, \"sizeOffset\": 0}, \"header\": {\"bold\": true, \"sizeOffset\": 7}, \"summary\": {\"bold\": false, \"sizeOffset\": 0}, \"tableBody\": {\"bold\": false, \"sizeOffset\": 0}, \"tableHead\": {\"bold\": true, \"sizeOffset\": 0}}, \"paymentTermsText\": \"客户订货需支付总金额百分之30，安装完成后需付尾款百分之70，剩余货款需在一个月内付清。\", \"showPaymentTerms\": true}, \"builtIn\": false}, {\"name\": \"竖-字体放大\", \"config\": {\"columns\": [{\"key\": \"productName\", \"label\": \"商品名称\", \"visible\": true}, {\"key\": \"series\", \"label\": \"系列\", \"visible\": true}, {\"key\": \"color\", \"label\": \"颜色\", \"visible\": true}, {\"key\": \"width\", \"label\": \"宽(mm)\", \"visible\": true}, {\"key\": \"height\", \"label\": \"高(mm)\", \"visible\": true}, {\"key\": \"wallThickness\", \"label\": \"墙厚(mm)\", \"visible\": true}, {\"key\": \"lockPosition\", \"label\": \"锁位\", \"visible\": true}, {\"key\": \"diaojiao\", \"label\": \"吊脚\", \"visible\": true}, {\"key\": \"doorCount\", \"label\": \"樘数\", \"visible\": true}, {\"key\": \"fangCount\", \"label\": \"面积(㎡)\", \"visible\": true}, {\"key\": \"productType\", \"label\": \"细目\", \"visible\": true}, {\"key\": \"unitPrice\", \"label\": \"单价\", \"visible\": true}, {\"key\": \"remark\", \"label\": \"备注\", \"visible\": true}], \"fontSize\": 11, \"marginTop\": 10, \"footerText\": \"\", \"marginLeft\": 8, \"showFooter\": true, \"marginRight\": 8, \"orientation\": \"portrait\", \"showSummary\": true, \"columnWidths\": {\"color\": 54, \"index\": 36, \"width\": 40, \"amount\": 60, \"height\": 40, \"remark\": 70, \"series\": 60, \"diaojiao\": 40, \"doorCount\": 40, \"fangCount\": 48, \"unitPrice\": 56, \"productName\": 100, \"productType\": 60, \"lockPosition\": 36, \"wallThickness\": 40}, \"marginBottom\": 10, \"showTotalRow\": true, \"sectionStyles\": {\"info\": {\"bold\": true, \"align\": \"left\", \"sizeOffset\": 2}, \"terms\": {\"bold\": true, \"sizeOffset\": 3}, \"footer\": {\"bold\": true, \"sizeOffset\": 4}, \"header\": {\"bold\": true, \"sizeOffset\": 7}, \"summary\": {\"bold\": true, \"sizeOffset\": 2}, \"tableBody\": {\"bold\": false, \"sizeOffset\": 2}, \"tableHead\": {\"bold\": true, \"sizeOffset\": 1}}, \"paymentTermsText\": \"客户订货需支付总金额百分之30，安装完成后需付尾款百分之70，剩余货款需在一个月内付清。\", \"showPaymentTerms\": true}, \"builtIn\": false}], \"sectionStyles\": {\"info\": {\"bold\": false, \"align\": \"left\", \"sizeOffset\": 0}, \"terms\": {\"bold\": false, \"sizeOffset\": 0}, \"footer\": {\"bold\": false, \"sizeOffset\": 0}, \"header\": {\"bold\": false, \"sizeOffset\": 0}, \"summary\": {\"bold\": false, \"sizeOffset\": 0}, \"tableBody\": {\"bold\": false, \"sizeOffset\": 0}, \"tableHead\": {\"bold\": true, \"sizeOffset\": 0}}, \"creatorAddress\": \"示例省示例市示例区示例路1号\", \"paymentTermsText\": \"客户订货需支付总金额百分之30，安装完成后需付尾款百分之70，剩余货款需在一个月内付清。\", \"showPaymentTerms\": true}','2026-05-19 19:50:32','order');

-- product
MERGE INTO product (`id`, `category_id`, `name`, `model`, `material`, `price`, `cover_image`, `description`, `images`, `specs`, `price_tag`, `price_breakdown`, `pricing_rule`, `reference_price`, `option_prices`, `performance`, `is_show`, `create_time`, `deleted`) KEY(id) VALUES (100,1,'70系列断桥铝内开内倒窗','BR70-NKD','6063-T5原生铝 + PA66GF25隔热条',780.00,'','70系列系统窗，德系内开内倒设计，通风不直吹、下雨不进水。多腔体结构搭配三道密封，专为山区高层住宅量身，抗风压性能达国标9级。',',','{\"型材:系列宽度\":\"70mm\",\"型材:壁厚\":\"1.8mm（新国标）\",\"型材:型材材质\":\"6063-T5原生铝\",\"型材:隔热条\":\"PA66GF25 尼龙，宽30mm\",\"型材:腔体结构\":\"5腔体 + 3道密封\",\"玻璃:标配配置\":\"5+20A+5 黑色折弯中空钢化\",\"玻璃:可选配置\":\"5+24A+5 黑色折弯, 5+20A+5 Low-E, 6+18A+6 大板加强\",\"玻璃:3C认证\":\"是（钢化激光印）\",\"玻璃:暖边处理\":\"铝间隔条 + 分子筛\",\"五金:执手品牌\":\"德国好博 HOPPE 锌合金\",\"五金:铰链/合页\":\"304不锈钢 进口轴承\",\"五金:锁点数量\":\"3锁点\",\"五金:承重\":\"80kg/扇\",\"花色:可选颜色\":\"金属灰, 咖啡棕, 黑晶石, 白橡木纹, 金丝楠木纹\",\"花色:表面工艺\":\"老虎粉TIGER 粉末涂层\",\"花色:内框颜色\":\"支持内外双色（如外灰内白）\",\"工艺:组角工艺\":\"销钉注胶工艺\",\"工艺:排水方式\":\"隐藏式排水\",\"工艺:密封胶\":\"三元乙丙EPDM 汽车级胶条\",\"服务:质保\":\"型材10年 / 五金5年 / 玻璃3年\",\"服务:计价\":\"按㎡（含五金安装，无增项）\"}','起','基础型材框架:¥580/㎡\n开启扇五金(进口好博):¥480/扇\n金刚网纱窗:¥280/扇\n三元乙丙密封+防水:¥180/窗\n拆旧清运:¥300/窗','按洞口面积计算,不足1㎡按1㎡计,异形窗/超大尺寸另计,以上为不含税参考价','1.5m×1.5m单开启扇窗户,总价约 ¥1,950','玻璃:Low-E镀膜:80\n玻璃:夹胶PVB:120\n花色:氟碳金:50\n花色:黑晶石:30','{\"抗风压\":\"9级\",\"气密性\":\"8级\",\"水密性\":\"6级\",\"隔音\":\"35dB\",\"保温K值\":\"1.8\"}',1,'2026-05-10 20:03:58',0);
MERGE INTO product (`id`, `category_id`, `name`, `model`, `material`, `price`, `cover_image`, `description`, `images`, `specs`, `price_tag`, `price_breakdown`, `pricing_rule`, `reference_price`, `option_prices`, `performance`, `is_show`, `create_time`, `deleted`) KEY(id) VALUES (101,1,'90系列断桥铝外开窗','BR90-WK','6063-T5原生铝 + PA66GF25隔热条',850.00,'','90系列加宽外开窗，不占室内空间，配儿童安全锁。加厚2.0mm壁厚设计，适配示例市乡镇自建房及沿海台风区域，水密性6级。',',','{\"型材:系列宽度\":\"90mm\",\"型材:壁厚\":\"2.0mm（加强）\",\"型材:型材材质\":\"6063-T5原生铝\",\"型材:隔热条\":\"PA66GF25 34mm宽\",\"型材:腔体结构\":\"6腔体 + 3道密封\",\"玻璃:标配配置\":\"5+24A+5 双层中空钢化\",\"玻璃:可选配置\":\"6+20A+6 黑色折弯, 5+24A+5 Low-E, 6+18A+6 大板加强\",\"玻璃:3C认证\":\"是\",\"玻璃:暖边处理\":\"铝间隔条\",\"五金:执手品牌\":\"德国丝吉利娅 SIEGENIA\",\"五金:铰链/合页\":\"304不锈钢 进口轴承\",\"五金:锁点数量\":\"4锁点\",\"五金:安全锁\":\"儿童安全锁\",\"花色:可选颜色\":\"金属灰, 深咖, 白色, 香槟金\",\"花色:表面工艺\":\"氟碳喷涂（不褪色）\",\"花色:内框颜色\":\"支持双色定制\",\"工艺:组角工艺\":\"销钉注胶\",\"工艺:排水方式\":\"隐藏式排水 + 防水盖\",\"工艺:密封胶\":\"三元乙丙EPDM\",\"服务:质保\":\"型材10年 / 五金5年 / 玻璃3年\",\"服务:计价\":\"按㎡（含安装五金）\"}','含安装五金','','','','','{\"抗风压\":\"8级\",\"气密性\":\"8级\"}',1,'2026-05-10 20:03:58',0);
MERGE INTO product (`id`, `category_id`, `name`, `model`, `material`, `price`, `cover_image`, `description`, `images`, `specs`, `price_tag`, `price_breakdown`, `pricing_rule`, `reference_price`, `option_prices`, `performance`, `is_show`, `create_time`, `deleted`) KEY(id) VALUES (102,2,'100系列断桥铝重型推拉窗','BT100-TL','6063-T5原生铝 + PA66隔热条',650.00,'','100系列重型推拉，加厚不锈钢静音滑轮，推拉顺畅无声。通风面积大、视野开阔，示例市阳台封窗改造首选。自带304金刚网防蚊纱窗。',',','{\"型材:系列宽度\":\"100mm\",\"型材:壁厚\":\"2.0mm\",\"型材:型材材质\":\"6063-T5原生铝\",\"型材:隔热条\":\"PA66 宽24mm\",\"型材:滑轮\":\"304不锈钢静音轴承\",\"玻璃:标配配置\":\"5+20A+5 黑色折弯中空钢化\",\"玻璃:可选配置\":\"5+24A+5 黑色折弯, 5+20A+5 Low-E, 6+18A+6 大板加强\",\"玻璃:3C认证\":\"是\",\"五金:品牌\":\"国产坚朗\",\"五金:滑轨\":\"不锈钢加强轨\",\"五金:锁具\":\"月牙锁\",\"花色:可选颜色\":\"金属灰, 深咖, 白色\",\"花色:表面工艺\":\"粉末涂层\",\"花色:纱窗\":\"304金刚网（自带）\",\"工艺:排水方式\":\"高轨防水设计\",\"工艺:密封胶\":\"三元乙丙EPDM\",\"服务:质保\":\"型材8年 / 五金3年 / 玻璃3年\",\"服务:计价\":\"按㎡\"}','含安装五金',NULL,NULL,NULL,NULL,NULL,1,'2026-05-10 20:03:58',0);
MERGE INTO product (`id`, `category_id`, `name`, `model`, `material`, `price`, `cover_image`, `description`, `images`, `specs`, `price_tag`, `price_breakdown`, `pricing_rule`, `reference_price`, `option_prices`, `performance`, `is_show`, `create_time`, `deleted`) KEY(id) VALUES (103,3,'85系列断桥铝阳台平开门','BP85-YT','6063-T5原生铝 + PA66GF25隔热条',980.00,'','85系列阳台平开门，德国进口诺托铰链，单扇承重120kg。加宽门槛防水防尘设计，配自动升降密封条，门扇闭合后自动下降密封。',',','{\"型材:系列宽度\":\"85mm\",\"型材:壁厚\":\"2.2mm\",\"型材:型材材质\":\"6063-T5原生铝\",\"型材:隔热条\":\"PA66GF25 宽36mm\",\"型材:腔体结构\":\"5腔体\",\"玻璃:标配配置\":\"5+24A+5 黑色折弯中空钢化\",\"玻璃:可选配置\":\"6+20A+6, 5+24A+5 Low-E, 夹胶玻璃\",\"玻璃:3C认证\":\"是\",\"五金:铰链\":\"德国诺托 ROTO 三维可调\",\"五金:执手\":\"德国好博 HOPPE\",\"五金:承重\":\"120kg/扇\",\"五金:锁点\":\"4锁点\",\"五金:门吸\":\"304不锈钢地吸\",\"花色:可选颜色\":\"金属灰, 咖啡棕, 黑晶石, 白色, 金丝楠木纹\",\"花色:表面工艺\":\"氟碳喷涂\",\"花色:门槛\":\"不锈钢防水门槛\",\"工艺:排水方式\":\"隐藏式排水\",\"工艺:密封\":\"三元乙丙EPDM + 自动升降密封条\",\"服务:质保\":\"型材10年 / 五金5年 / 玻璃3年\",\"服务:计价\":\"按扇（含铰链执手安装）\"}','含安装五金',NULL,NULL,NULL,NULL,NULL,1,'2026-05-10 20:03:58',0);
MERGE INTO product (`id`, `category_id`, `name`, `model`, `material`, `price`, `cover_image`, `description`, `images`, `specs`, `price_tag`, `price_breakdown`, `pricing_rule`, `reference_price`, `option_prices`, `performance`, `is_show`, `create_time`, `deleted`) KEY(id) VALUES (104,4,'120系列断桥铝提升推拉门','BT120-TS','6063-T5原生铝 + PA66GF25隔热条',1380.00,'','120系列提升推拉门，德国GU提升系统，转动执手门扇整体抬起，推拉轻如鸿毛。锁闭后密封条紧密贴合，隔音保温不输平开门。高轨不锈钢下轨，暴雨不进水。',',','{\"型材:系列宽度\":\"120mm\",\"型材:壁厚\":\"2.5mm\",\"型材:型材材质\":\"6063-T5原生铝\",\"型材:隔热条\":\"PA66GF25 宽42mm\",\"型材:腔体结构\":\"6腔体 + 3道密封\",\"玻璃:标配配置\":\"6+20A+6 黑色折弯中空钢化\",\"玻璃:可选配置\":\"6+24A+6 黑色折弯, 6+20A+6 Low-E, 6+18A+6 大板加强\",\"玻璃:3C认证\":\"是\",\"五金:提升系统\":\"德国格屋 G-U 提升机构\",\"五金:执手\":\"德国好博 HOPPE\",\"五金:滑轨\":\"304不锈钢高轨\",\"五金:锁点\":\"多点锁闭\",\"花色:可选颜色\":\"金属灰, 咖啡棕, 黑晶石, 白橡木纹\",\"花色:表面工艺\":\"氟碳喷涂\",\"花色:门槛\":\"不锈钢高轨门槛\",\"工艺:排水方式\":\"高轨 + 地漏式排水\",\"工艺:密封\":\"三元乙丙EPDM 四道密封\",\"服务:质保\":\"型材10年 / 五金5年 / 玻璃3年\",\"服务:计价\":\"按扇（含提升系统安装）\"}','含安装五金',NULL,NULL,NULL,NULL,NULL,1,'2026-05-10 20:03:58',0);
MERGE INTO product (`id`, `category_id`, `name`, `model`, `material`, `price`, `cover_image`, `description`, `images`, `specs`, `price_tag`, `price_breakdown`, `pricing_rule`, `reference_price`, `option_prices`, `performance`, `is_show`, `create_time`, `deleted`) KEY(id) VALUES (105,5,'全景落地封阳台系统','FB-QJ01','6063-T5断桥铝 + 大板中空钢化玻璃',1580.00,'','全景封阳台方案，大板玻璃+窄边框断桥铝框架，视野无遮挡。针对示例市老城区阳台特点，含拆旧、墙体加固、防水密封一体化服务。开窗位置可灵活定制。',',','{\"型材:框架材质\":\"6063-T5断桥铝\",\"型材:壁厚\":\"2.0mm\",\"型材:边框宽度\":\"极窄边框设计\",\"玻璃:标配配置\":\"6+20A+6 大板中空钢化\",\"玻璃:最大单玻\":\"2400×1800mm\",\"玻璃:3C认证\":\"是\",\"玻璃:可选配置\":\"Low-E镀膜, 夹胶玻璃, 6+18A+6 大板加强\",\"五金:执手\":\"德国好博 HOPPE\",\"五金:铰链\":\"304不锈钢\",\"花色:框架颜色\":\"金属灰, 深咖, 白色\",\"花色:表面工艺\":\"氟碳喷涂\",\"工艺:墙体加固\":\"聚氨酯注浆\",\"工艺:防水处理\":\"聚氨酯防水涂料\",\"工艺:密封\":\"三元乙丙EPDM\",\"服务:包含服务\":\"拆旧 / 加固 / 防水 / 密封 / 安装\",\"服务:计价\":\"面议（免费上门测量）\"}','含安装五金',NULL,NULL,NULL,NULL,NULL,1,'2026-05-10 20:03:58',0);
MERGE INTO product (`id`, `category_id`, `name`, `model`, `material`, `price`, `cover_image`, `description`, `images`, `specs`, `price_tag`, `price_breakdown`, `pricing_rule`, `reference_price`, `option_prices`, `performance`, `is_show`, `create_time`, `deleted`) KEY(id) VALUES (106,6,'304不锈钢防盗门（甲级）','SB-A01','304不锈钢板 + C级锁芯',2680.00,'','甲级防盗门，门扇厚度9cm，304不锈钢面板+内置钢骨架+防火隔音填充。配C级锁芯，防撬防钻防技术开锁。多用于示例市临街商铺大门及自建房底层入口。',',','{\"型材:门扇厚度\":\"9cm\",\"型材:面板材质\":\"304不锈钢\",\"型材:内部结构\":\"钢骨架 + 防火填充\",\"五金:锁芯等级\":\"C级（防技术开锁）\",\"五金:锁体\":\"304不锈钢\",\"五金:铰链\":\"重型轴承铰链\",\"五金:猫眼\":\"电子猫眼（可选）\",\"花色:面板颜色\":\"不锈钢原色, 金属灰, 深咖\",\"花色:表面处理\":\"拉丝防指纹\",\"花色:门框\":\"不锈钢门框\",\"工艺:防盗等级\":\"甲级\",\"工艺:密封\":\"EPDM密封条\",\"服务:质保\":\"10年\",\"服务:计价\":\"按扇（含安装）\"}','含安装五金',NULL,NULL,NULL,NULL,NULL,1,'2026-05-10 20:03:58',0);
MERGE INTO product (`id`, `category_id`, `name`, `model`, `material`, `price`, `cover_image`, `description`, `images`, `specs`, `price_tag`, `price_breakdown`, `pricing_rule`, `reference_price`, `option_prices`, `performance`, `is_show`, `create_time`, `deleted`) KEY(id) VALUES (107,7,'可开启式不锈钢防盗窗','SC-K01','304不锈钢方管 + 逃生装置',420.00,'','可开启式防盗窗，紧急情况从内部一键开启逃生。304不锈钢方管，网格加密防攀爬，表面拉丝防锈。盐雾测试500小时不锈，适配高湿山区气候。',',','{\"型材:材质\":\"304不锈钢方管\",\"型材:壁厚\":\"1.2mm\",\"型材:网格间距\":\"110×110mm\",\"型材:竖杆\":\"Φ18mm\",\"五金:锁具\":\"内置插销锁\",\"五金:合页\":\"304不锈钢\",\"五金:逃生装置\":\"一键开启\",\"花色:表面处理\":\"拉丝防锈\",\"花色:颜色\":\"不锈钢原色, 白色, 灰色\",\"工艺:焊接\":\"满焊抛光\",\"工艺:盐雾测试\":\"500小时\",\"服务:质保\":\"5年\",\"服务:计价\":\"按㎡（含安装）\"}','含安装五金',NULL,NULL,NULL,NULL,NULL,1,'2026-05-10 20:03:58',0);
MERGE INTO product (`id`, `category_id`, `name`, `model`, `material`, `price`, `cover_image`, `description`, `images`, `specs`, `price_tag`, `price_breakdown`, `pricing_rule`, `reference_price`, `option_prices`, `performance`, `is_show`, `create_time`, `deleted`) KEY(id) VALUES (108,8,'电动304不锈钢商铺卷帘门','SJ-D01','304不锈钢门片 + 电动遥控系统',3800.00,'','电动不锈钢卷帘门，适配示例市城沿街商铺。支持遥控/手动双模式，停电可手摇启闭。加厚门片+防撬底梁，隔音防盗。最大宽度6米。',',','{\"型材:门片材质\":\"304不锈钢加厚\",\"型材:门片厚度\":\"0.8mm\",\"型材:最大宽度\":\"6m\",\"五金:驱动\":\"电动 + 手动双模\",\"五金:电机\":\"杜亚DOOYA 静音管状电机\",\"五金:遥控\":\"433MHz 滚码遥控（2把）\",\"五金:底梁\":\"防撬加厚底梁\",\"花色:门片颜色\":\"不锈钢原色, 白色, 灰色\",\"工艺:传动\":\"链条传动\",\"工艺:安全\":\"遇阻反弹\",\"服务:质保\":\"电机2年 / 门体5年\",\"服务:计价\":\"按门洞面积（含安装调试）\"}','含安装五金',NULL,NULL,NULL,NULL,NULL,1,'2026-05-10 20:03:58',0);
MERGE INTO product (`id`, `category_id`, `name`, `model`, `material`, `price`, `cover_image`, `description`, `images`, `specs`, `price_tag`, `price_breakdown`, `pricing_rule`, `reference_price`, `option_prices`, `performance`, `is_show`, `create_time`, `deleted`) KEY(id) VALUES (109,9,'实木复合烤漆门','SM-KQ01','实木框架 + E1级密度板 + 钢琴烤漆',1280.00,'','实木复合烤漆门，内填蜂窝隔音结构，表面钢琴烤漆处理。多色可选，支持定制尺寸。含门套、静音磁吸锁、不锈钢合页，包安装。适配示例市本地住宅卧室、书房。',',','{\"型材:门扇厚度\":\"45mm\",\"型材:框架\":\"实木指接框架\",\"型材:面板\":\"E1级环保密度板\",\"型材:填充\":\"蜂窝隔音结构\",\"五金:锁具\":\"静音磁吸锁\",\"五金:合页\":\"304不锈钢 3只\",\"五金:门吸\":\"304不锈钢\",\"花色:可选颜色\":\"纯白烤漆, 浅灰烤漆, 奶咖烤漆, 深灰烤漆\",\"花色:可选款式\":\"平板款, 线条款, 镶嵌款\",\"花色:表面\":\"钢琴烤漆（耐黄变）\",\"工艺:封边\":\"PVC封边\",\"工艺:门套\":\"实木复合门套\",\"服务:质保\":\"5年\",\"服务:计价\":\"按扇（含门套五金安装）\"}','含安装五金',NULL,NULL,NULL,NULL,NULL,1,'2026-05-10 20:03:58',0);
MERGE INTO product (`id`, `category_id`, `name`, `model`, `material`, `price`, `cover_image`, `description`, `images`, `specs`, `price_tag`, `price_breakdown`, `pricing_rule`, `reference_price`, `option_prices`, `performance`, `is_show`, `create_time`, `deleted`) KEY(id) VALUES (110,10,'免漆模压门（经济款）','MM-J01','实木骨架 + 模压面板 + PVC覆膜',680.00,'','经济型免漆模压门，PVC覆膜免漆处理，防水防潮。价格实惠，适合装修预算有限的家庭、出租房。提供多种木纹花色选择，含门套及基础五金。',',','{\"型材:门扇厚度\":\"40mm\",\"型材:框架\":\"实木骨架\",\"型材:面板\":\"模压面板\",\"型材:填充\":\"蜂窝纸\",\"五金:锁具\":\"普通球形锁\",\"五金:合页\":\"铁镀锌 3只\",\"五金:门吸\":\"普通门吸\",\"花色:可选花色\":\"白枫木纹, 橡木纹, 柚木纹, 胡桃木纹, 浅灰\",\"花色:表面\":\"PVC覆膜（免漆）\",\"工艺:封边\":\"PVC封边\",\"工艺:门套\":\"密度板门套\",\"服务:质保\":\"3年\",\"服务:计价\":\"按扇（含门套五金安装）\"}','含安装五金',NULL,NULL,NULL,NULL,NULL,1,'2026-05-10 20:03:58',0);
MERGE INTO product (`id`, `category_id`, `name`, `model`, `material`, `price`, `cover_image`, `description`, `images`, `specs`, `price_tag`, `price_breakdown`, `pricing_rule`, `reference_price`, `option_prices`, `performance`, `is_show`, `create_time`, `deleted`) KEY(id) VALUES (111,11,'新中式实木工艺门','SM-X01','实木指接板 + 水性环保漆',1980.00,'','新中式风格实木工艺门，指接实木芯材，稳定性好不变形。水性环保漆喷涂，无甲醛异味。可定制雕刻传统回纹/如意纹图案，适配示例市乡村自建房中式装修。',',','{\"型材:门扇厚度\":\"50mm\",\"型材:芯材\":\"实木指接板（全实木）\",\"型材:面板\":\"实木面板\",\"五金:锁具\":\"中式铜锁（可选）\",\"五金:合页\":\"304不锈钢 4只\",\"五金:门吸\":\"304不锈钢\",\"花色:可选颜色\":\"胡桃木色, 樱桃木色, 原木色, 浅胡桃\",\"花色:可选雕刻\":\"回纹, 如意纹, 祥云纹, 素面\",\"花色:表面\":\"水性环保漆（哑光/半哑）\",\"工艺:结构\":\"指接工艺（不易变形）\",\"工艺:门套\":\"实木门套\",\"服务:质保\":\"8年\",\"服务:计价\":\"按扇（含门套五金安装）\"}','含安装五金',NULL,NULL,NULL,NULL,NULL,1,'2026-05-10 20:03:58',0);
MERGE INTO product (`id`, `category_id`, `name`, `model`, `material`, `price`, `cover_image`, `description`, `images`, `specs`, `price_tag`, `price_breakdown`, `pricing_rule`, `reference_price`, `option_prices`, `performance`, `is_show`, `create_time`, `deleted`) KEY(id) VALUES (112,12,'三元乙丙密封胶条（耐候型）','JT-EPDM','三元乙丙橡胶（EPDM）',15.00,'','三元乙丙汽车级密封胶条，耐候抗老化性能为普通橡胶3倍。专为山区高湿气候优化，-40℃~120℃不变形不脆化，使用寿命10年以上。按米计价。',',','{\"型材:材质\":\"EPDM 三元乙丙橡胶\",\"型材:耐温范围\":\"-40℃~120℃\",\"型材:适用槽口\":\"欧标/国标\",\"五金:品牌\":\"硅宝\",\"花色:可选颜色\":\"黑色, 灰色\",\"工艺:寿命\":\"10年以上\",\"工艺:特点\":\"耐臭氧/耐UV/弹性好\",\"服务:质保\":\"3年\",\"服务:计价\":\"按米\"}','含安装五金',NULL,NULL,NULL,NULL,NULL,1,'2026-05-10 20:03:58',0);
MERGE INTO product (`id`, `category_id`, `name`, `model`, `material`, `price`, `cover_image`, `description`, `images`, `specs`, `price_tag`, `price_breakdown`, `pricing_rule`, `reference_price`, `option_prices`, `performance`, `is_show`, `create_time`, `deleted`) KEY(id) VALUES (113,13,'304不锈钢轴承铰链（防锈型）','HJ-304','304不锈钢 + 进口密封轴承',48.00,'','304不锈钢轴承铰链，进口密封轴承，盐雾测试500小时不锈。专为高湿山区环境设计，单只承重80kg，三维可调，开合顺畅无异响。',',','{\"型材:材质\":\"304不锈钢\",\"型材:轴承\":\"进口密封轴承\",\"型材:厚度\":\"3.0mm\",\"五金:承重\":\"80kg/只\",\"五金:调节\":\"三维可调（上下/左右/前后）\",\"五金:盐雾测试\":\"500小时\",\"花色:颜色\":\"不锈钢原色\",\"工艺:表面\":\"拉丝处理\",\"服务:质保\":\"5年\",\"服务:计价\":\"按只\"}','含安装五金',NULL,NULL,NULL,NULL,NULL,1,'2026-05-10 20:03:58',0);
MERGE INTO product (`id`, `category_id`, `name`, `model`, `material`, `price`, `cover_image`, `description`, `images`, `specs`, `price_tag`, `price_breakdown`, `pricing_rule`, `reference_price`, `option_prices`, `performance`, `is_show`, `create_time`, `deleted`) KEY(id) VALUES (114,14,'双层中空钢化玻璃（5+20A+5）','BL-ZK05','浮法玻璃 + 钢化处理 + 铝间隔条',128.00,'','5+20A+5双层中空钢化玻璃，CCC认证激光刻印。铝条内填分子筛干燥剂，长效不起雾。可按需定制Low-E镀膜、磨砂、夹胶等附加工艺。',',','{\"型材:规格\":\"5+20A+5mm\",\"型材:原片品牌\":\"信义/南玻\",\"型材:认证\":\"CCC钢化\",\"玻璃:可选配置\":\"Low-E镀膜, 磨砂/玉砂, 夹胶PVB\",\"玻璃:中空处理\":\"铝间隔条 + 分子筛干燥剂\",\"玻璃:可选规格\":\"5+24A+5, 6+20A+6\",\"工艺:折弯\":\"一体折弯（防漏气）\",\"花色:可选颜色\":\"透明, 超白, 灰玻, 茶玻\",\"服务:质保\":\"3年\",\"服务:计价\":\"按㎡\"}','含安装五金',NULL,NULL,NULL,NULL,NULL,1,'2026-05-10 20:03:58',0);
MERGE INTO product (`id`, `category_id`, `name`, `model`, `material`, `price`, `cover_image`, `description`, `images`, `specs`, `price_tag`, `price_breakdown`, `pricing_rule`, `reference_price`, `option_prices`, `performance`, `is_show`, `create_time`, `deleted`) KEY(id) VALUES (115,15,'304不锈钢防护栏杆','LG-304','304不锈钢',298.00,'A001.jpg','304不锈钢阳台防护栏杆，竖杆间距≤11cm符合国标安全规范，法兰焊接安装稳固不晃动。适用于阳台、楼梯、走廊等。按米计价。','A001.jpg,A001-1.jpg','{\"型材:材质\":\"304不锈钢\",\"型材:主管\":\"Φ50×1.5mm\",\"型材:竖杆\":\"Φ19×1.0mm\",\"型材:竖杆间距\":\"≤11cm（国标安全）\",\"五金:安装\":\"法兰膨胀螺栓\",\"花色:颜色\":\"不锈钢原色, 黑色, 白色\",\"工艺:表面\":\"拉丝防锈\",\"工艺:焊接\":\"氩弧焊满焊\",\"服务:质保\":\"5年\",\"服务:计价\":\"按米（含安装）\"}','含安装五金',NULL,NULL,NULL,NULL,NULL,1,'2026-05-10 20:03:58',0);
MERGE INTO product (`id`, `category_id`, `name`, `model`, `material`, `price`, `cover_image`, `description`, `images`, `specs`, `price_tag`, `price_breakdown`, `pricing_rule`, `reference_price`, `option_prices`, `performance`, `is_show`, `create_time`, `deleted`) KEY(id) VALUES (116,16,'老旧门窗拆换一体化服务','FW-CH01','全包（拆旧 → 修补 → 防水 → 安装）',0.00,'','针对示例市老城区20年以上房龄房屋，提供老旧门窗拆换一体化服务。包含：①旧门窗拆除 ②墙体缺口专业修补 ③聚氨酯防水密封 ④新门窗安装调试。拒绝野蛮拆除导致的渗水开裂。免费上门评估，价格面议。',',','{\"型材:适用对象\":\"20年以上老城区房屋\",\"型材:改造范围\":\"窗户/门/阳台封窗\",\"服务:①拆除\":\"专业工具无损拆旧\",\"服务:②修补\":\"墙体缺口砂浆修补\",\"服务:③防水\":\"聚氨酯防水打底\",\"服务:④安装\":\"新门窗标准安装调试\",\"服务:工期\":\"1-3天（视数量）\",\"服务:质保\":\"整体2年\",\"服务:计价\":\"面议（免费上门评估出方案）\"}','含安装五金',NULL,NULL,NULL,NULL,NULL,1,'2026-05-10 20:03:58',0);
MERGE INTO product (`id`, `category_id`, `name`, `model`, `material`, `price`, `cover_image`, `description`, `images`, `specs`, `price_tag`, `price_breakdown`, `pricing_rule`, `reference_price`, `option_prices`, `performance`, `is_show`, `create_time`, `deleted`) KEY(id) VALUES (117,17,'乡村自建房门窗整体方案','FW-ZJ01','定制（测量 → 设计 → 生产 → 安装）',0.00,'','为示例市各镇乡村自建房量身定制门窗整体方案。综合考虑山区风压、房屋朝向、采光通风等因素，强化连接件稳固性。提供断桥铝/不锈钢/木门组合方案，一站式配齐。免费上门测量出图。',',','{\"型材:适用对象\":\"乡村自建房（2-4层）\",\"型材:设计要素\":\"风压/朝向/采光/通风\",\"服务:①测量\":\"免费上门精准测量\",\"服务:②设计\":\"CAD方案图 + 3D效果可选\",\"服务:③产品\":\"断桥铝/不锈钢/木门组合\",\"服务:④安装\":\"自有安装团队施工\",\"服务:工期\":\"7-15天（视面积）\",\"服务:质保\":\"型材10年/五金5年/玻璃3年\",\"服务:计价\":\"面议（免费上门测量出方案）\"}','含安装五金',NULL,NULL,NULL,NULL,NULL,1,'2026-05-10 20:03:58',0);
MERGE INTO product (`id`, `category_id`, `name`, `model`, `material`, `price`, `cover_image`, `description`, `images`, `specs`, `price_tag`, `price_breakdown`, `pricing_rule`, `reference_price`, `option_prices`, `performance`, `is_show`, `create_time`, `deleted`) KEY(id) VALUES (118,18,'门窗维修养护耗材包','HC-B01','密封胶 + 轨道润滑剂 + 五金清洁防锈喷雾',68.00,'','专为山区高湿气候搭配的门窗日常养护三件套：①专用耐候密封胶 ②轨道滑轮润滑剂 ③五金清洁防锈喷雾。简单操作即可延长门窗使用寿命3-5年。',',','{\"型材:套装内容\":\"密封胶×1 + 润滑剂×1 + 防锈喷雾×1\",\"型材:密封胶\":\"硅宝耐候密封胶 300ml\",\"型材:润滑剂\":\"轨道专用润滑脂 50ml\",\"型材:防锈喷雾\":\"五金防锈清洁剂 200ml\",\"服务:适用\":\"断桥铝/铝合金/不锈钢门窗\",\"服务:效果\":\"延长寿命3-5年\",\"服务:使用频率\":\"建议每6个月养护1次\",\"服务:质保\":\"耗材类无质保\",\"服务:计价\":\"按套\"}','含安装五金',NULL,NULL,NULL,NULL,NULL,1,'2026-05-10 20:03:58',0);

-- product_type
MERGE INTO product_type (`id`, `name`, `summary`, `sort`, `create_time`, `summary_desc`, `deleted`) KEY(id) VALUES (1,'入户门',NULL,1,'2026-05-18 17:14:54','',0);
MERGE INTO product_type (`id`, `name`, `summary`, `sort`, `create_time`, `summary_desc`, `deleted`) KEY(id) VALUES (2,'房间门',NULL,2,'2026-05-18 17:14:54',NULL,0);
MERGE INTO product_type (`id`, `name`, `summary`, `sort`, `create_time`, `summary_desc`, `deleted`) KEY(id) VALUES (3,'卫生间门',NULL,3,'2026-05-18 17:14:54',NULL,0);
MERGE INTO product_type (`id`, `name`, `summary`, `sort`, `create_time`, `summary_desc`, `deleted`) KEY(id) VALUES (4,'厨房门',NULL,4,'2026-05-18 17:14:54',NULL,0);
MERGE INTO product_type (`id`, `name`, `summary`, `sort`, `create_time`, `summary_desc`, `deleted`) KEY(id) VALUES (5,'推拉门',NULL,5,'2026-05-18 17:14:54',NULL,0);
MERGE INTO product_type (`id`, `name`, `summary`, `sort`, `create_time`, `summary_desc`, `deleted`) KEY(id) VALUES (6,'折叠门',NULL,6,'2026-05-18 17:14:54',NULL,0);
MERGE INTO product_type (`id`, `name`, `summary`, `sort`, `create_time`, `summary_desc`, `deleted`) KEY(id) VALUES (7,'平开窗',NULL,7,'2026-05-18 17:14:54',NULL,0);
MERGE INTO product_type (`id`, `name`, `summary`, `sort`, `create_time`, `summary_desc`, `deleted`) KEY(id) VALUES (8,'推拉窗',NULL,8,'2026-05-18 17:14:54',NULL,0);
MERGE INTO product_type (`id`, `name`, `summary`, `sort`, `create_time`, `summary_desc`, `deleted`) KEY(id) VALUES (9,'固定窗',NULL,9,'2026-05-18 17:14:54',NULL,0);
MERGE INTO product_type (`id`, `name`, `summary`, `sort`, `create_time`, `summary_desc`, `deleted`) KEY(id) VALUES (10,'百叶窗',NULL,10,'2026-05-18 17:14:54',NULL,0);
MERGE INTO product_type (`id`, `name`, `summary`, `sort`, `create_time`, `summary_desc`, `deleted`) KEY(id) VALUES (11,'纱窗',NULL,11,'2026-05-18 17:14:54',NULL,0);
MERGE INTO product_type (`id`, `name`, `summary`, `sort`, `create_time`, `summary_desc`, `deleted`) KEY(id) VALUES (12,'防盗窗',NULL,12,'2026-05-18 17:14:54',NULL,0);
MERGE INTO product_type (`id`, `name`, `summary`, `sort`, `create_time`, `summary_desc`, `deleted`) KEY(id) VALUES (13,'阳光房',NULL,13,'2026-05-18 17:14:54',NULL,0);
MERGE INTO product_type (`id`, `name`, `summary`, `sort`, `create_time`, `summary_desc`, `deleted`) KEY(id) VALUES (14,'隔断',NULL,14,'2026-05-18 17:14:54',NULL,0);
MERGE INTO product_type (`id`, `name`, `summary`, `sort`, `create_time`, `summary_desc`, `deleted`) KEY(id) VALUES (15,'包边',NULL,15,'2026-05-18 17:14:54',NULL,0);
MERGE INTO product_type (`id`, `name`, `summary`, `sort`, `create_time`, `summary_desc`, `deleted`) KEY(id) VALUES (16,'门套',NULL,16,'2026-05-18 17:14:54',NULL,0);
MERGE INTO product_type (`id`, `name`, `summary`, `sort`, `create_time`, `summary_desc`, `deleted`) KEY(id) VALUES (17,'窗套',NULL,17,'2026-05-18 17:14:54',NULL,0);
MERGE INTO product_type (`id`, `name`, `summary`, `sort`, `create_time`, `summary_desc`, `deleted`) KEY(id) VALUES (18,'单包',NULL,0,'2026-05-21 09:47:42','',0);
MERGE INTO product_type (`id`, `name`, `summary`, `sort`, `create_time`, `summary_desc`, `deleted`) KEY(id) VALUES (19,'双包',NULL,0,'2026-05-21 09:47:49','',0);

-- purchase_order
MERGE INTO purchase_order (`id`, `order_no`, `supplier_id`, `supplier_name`, `supplier_contact`, `supplier_phone`, `supplier_address`, `order_date`, `total_amount`, `status`, `remark`, `create_time`, `update_time`, `paid_amount`, `is_cleared`, `deleted`) KEY(id) VALUES (1, 'CG20260105001', 1, '示例供应商01', '示例联系人A', '13900000001', '示例省示例市示例工业园1号', '2026-01-05', 2000.00, 'completed', '示例采购', '2026-01-05 09:00:00', '2026-01-05 09:00:00', 2000.00, 1, 0);

-- purchase_order_item
MERGE INTO purchase_order_item (`id`, `purchase_order_id`, `commodity_id`, `product_name`, `product_category`, `spec`, `unit`, `warehouse_loc`, `quantity`, `unit_price`, `amount`, `remark`, `create_time`, `deleted`) KEY(id) VALUES (1, 1, NULL, '铝合金型材', '型材', '示例规格', '米', '示例仓', 100.0000, 20.00, 2000.00, '示例采购明细', '2026-01-05 09:00:00', 0);

-- purchase_return
MERGE INTO purchase_return (`id`, `order_no`, `original_order_no`, `supplier_id`, `supplier_name`, `return_date`, `total_amount`, `status`, `remark`, `create_time`, `update_time`, `paid_amount`, `is_cleared`, `deleted`) KEY(id) VALUES (1, 'CT20260112001', 'CG20260105001', 1, '示例供应商01', '2026-01-12', 200.00, 'completed', '示例采购退货', '2026-01-12 09:00:00', '2026-01-12 09:00:00', 200.00, 1, 0);

-- purchase_return_item
MERGE INTO purchase_return_item (`id`, `return_id`, `commodity_id`, `product_name`, `product_category`, `spec`, `unit`, `quantity`, `unit_price`, `amount`, `remark`, `create_time`, `deleted`) KEY(id) VALUES (1, 1, NULL, '铝合金型材', '型材', '示例规格', '米', 10.0000, 20.00, 200.00, '示例退货明细', '2026-01-12 09:00:00', 0);

-- sale_return
MERGE INTO sale_return (`id`, `order_no`, `original_order_no`, `customer_id`, `customer_name`, `return_date`, `total_amount`, `status`, `remark`, `create_time`, `update_time`, `paid_amount`, `is_cleared`, `deleted`) KEY(id) VALUES (1, 'XT20260110001', 'XS20260101001', 1, '示例客户01', '2026-01-10', 380.00, 'completed', '示例退货', '2026-01-10 09:00:00', '2026-01-10 09:00:00', 380.00, 1, 0);

-- sale_return_item
MERGE INTO sale_return_item (`id`, `return_id`, `commodity_id`, `product_name`, `product_category`, `spec`, `unit`, `quantity`, `unit_price`, `amount`, `remark`, `create_time`, `deleted`) KEY(id) VALUES (1, 1, NULL, '断桥铝平开窗', '门窗', '示例规格', '樘', 1.0000, 380.00, 380.00, '示例退货明细', '2026-01-10 09:00:00', 0);

-- stock_in
MERGE INTO stock_in (`id`, `order_no`, `order_date`, `applicant`, `warehouse_keeper`, `operator`, `remark`, `create_time`, `deleted`) KEY(id) VALUES (1, 'RK20260105001', '2026-01-05', '示例仓管', '示例仓管', '示例操作员', '示例入库', '2026-01-05 08:00:00', 0);

-- stock_in_item
MERGE INTO stock_in_item (`id`, `stock_in_id`, `commodity_id`, `product_name`, `product_category`, `spec`, `unit`, `warehouse_loc`, `quantity`, `unit_price`, `amount`, `remark`, `create_time`, `deleted`) KEY(id) VALUES (1, 1, NULL, '中空玻璃', '玻璃', '示例规格', '片', '示例仓', 20.0000, 50.00, 1000.00, '示例入库明细', '2026-01-05 08:00:00', 0);

-- stock_out
MERGE INTO stock_out (`id`, `order_no`, `order_date`, `applicant`, `warehouse_keeper`, `operator`, `remark`, `create_time`, `deleted`) KEY(id) VALUES (1, 'CK20260106001', '2026-01-06', '示例仓管', '示例仓管', '示例操作员', '示例出库', '2026-01-06 08:00:00', 0);

-- stock_out_item
MERGE INTO stock_out_item (`id`, `stock_out_id`, `commodity_id`, `product_name`, `product_category`, `spec`, `unit`, `warehouse_loc`, `quantity`, `unit_price`, `amount`, `remark`, `create_time`, `deleted`) KEY(id) VALUES (1, 1, NULL, '中空玻璃', '玻璃', '示例规格', '片', '示例仓', 5.0000, 50.00, 250.00, '示例出库明细', '2026-01-06 08:00:00', 0);

-- supplier
MERGE INTO supplier (`id`, `name`, `contact`, `phone`, `address`, `remark`, `create_time`, `update_time`, `is_starred`, `deleted`) KEY(id) VALUES (1, '示例供应商01', '示例联系人A', '13900000001', '示例省示例市示例工业园1号', '示例供应商', '2026-01-01 10:00:00', '2026-01-01 10:00:00', 0, 0);
MERGE INTO supplier (`id`, `name`, `contact`, `phone`, `address`, `remark`, `create_time`, `update_time`, `is_starred`, `deleted`) KEY(id) VALUES (2, '示例供应商02', '示例联系人B', '13900000002', '示例省示例市示例工业园2号', '示例供应商', '2026-01-01 10:01:00', '2026-01-01 10:01:00', 0, 0);
MERGE INTO supplier (`id`, `name`, `contact`, `phone`, `address`, `remark`, `create_time`, `update_time`, `is_starred`, `deleted`) KEY(id) VALUES (3, '示例供应商03', '示例联系人C', '13900000003', '示例省示例市示例工业园3号', '示例供应商', '2026-01-01 10:02:00', '2026-01-01 10:02:00', 0, 0);

-- sys_config
MERGE INTO sys_config (`id`, `config_key`, `config_value`, `remark`, `create_time`, `update_time`) KEY(id) VALUES (1, 'deposit_rate', '0.3', '默认定金比例（30%）', '2026-05-16 17:28:41', '2026-05-16 17:28:41');
MERGE INTO sys_config (`id`, `config_key`, `config_value`, `remark`, `create_time`, `update_time`) KEY(id) VALUES (2, 'clear_threshold', '0.01', '清账阈值（差额比例<1%视为已清账）', '2026-05-16 17:28:41', '2026-05-16 17:28:41');
MERGE INTO sys_config (`id`, `config_key`, `config_value`, `remark`, `create_time`, `update_time`) KEY(id) VALUES (3, 'order_date_reset', '1', '单号是否按日重置：1=是 0=否', '2026-05-16 17:28:41', '2026-05-16 17:28:41');
MERGE INTO sys_config (`id`, `config_key`, `config_value`, `remark`, `create_time`, `update_time`) KEY(id) VALUES (4, 'company_name', '示例门窗', '公司名称', '2026-05-16 17:28:41', '2026-05-16 17:28:41');
MERGE INTO sys_config (`id`, `config_key`, `config_value`, `remark`, `create_time`, `update_time`) KEY(id) VALUES (10, 'sale_order_prefix', 'XS', NULL, '2026-05-21 21:55:43', '2026-05-21 21:55:43');
MERGE INTO sys_config (`id`, `config_key`, `config_value`, `remark`, `create_time`, `update_time`) KEY(id) VALUES (11, 'presale_order_prefix', 'YS', NULL, '2026-05-21 21:55:43', '2026-05-21 21:55:43');
MERGE INTO sys_config (`id`, `config_key`, `config_value`, `remark`, `create_time`, `update_time`) KEY(id) VALUES (12, 'purchase_order_prefix', 'CG', NULL, '2026-05-21 21:55:43', '2026-05-21 21:55:43');
MERGE INTO sys_config (`id`, `config_key`, `config_value`, `remark`, `create_time`, `update_time`) KEY(id) VALUES (13, 'sale_return_prefix', 'XT', NULL, '2026-05-21 21:55:43', '2026-05-21 21:55:43');
MERGE INTO sys_config (`id`, `config_key`, `config_value`, `remark`, `create_time`, `update_time`) KEY(id) VALUES (14, 'purchase_return_prefix', 'CT', NULL, '2026-05-21 21:55:43', '2026-05-21 21:55:43');
MERGE INTO sys_config (`id`, `config_key`, `config_value`, `remark`, `create_time`, `update_time`) KEY(id) VALUES (15, 'stock_in_prefix', 'RK', NULL, '2026-05-21 21:55:43', '2026-05-21 21:55:43');
MERGE INTO sys_config (`id`, `config_key`, `config_value`, `remark`, `create_time`, `update_time`) KEY(id) VALUES (16, 'stock_out_prefix', 'CK', NULL, '2026-05-21 21:55:43', '2026-05-21 21:55:43');
MERGE INTO sys_config (`id`, `config_key`, `config_value`, `remark`, `create_time`, `update_time`) KEY(id) VALUES (17, 'company_phone', '13800000000', NULL, '2026-05-21 21:55:43', '2026-05-21 21:55:43');
MERGE INTO sys_config (`id`, `config_key`, `config_value`, `remark`, `create_time`, `update_time`) KEY(id) VALUES (18, 'company_address', '示例省示例市示例区示例路1号', NULL, '2026-05-21 21:55:43', '2026-05-21 21:55:43');
MERGE INTO sys_config (`id`, `config_key`, `config_value`, `remark`, `create_time`, `update_time`) KEY(id) VALUES (19, 'print_title_sale', '示例门窗客户销售单', NULL, '2026-05-21 21:55:43', '2026-05-21 21:55:43');
MERGE INTO sys_config (`id`, `config_key`, `config_value`, `remark`, `create_time`, `update_time`) KEY(id) VALUES (20, 'print_title_presale', '示例门窗客户预算单', NULL, '2026-05-21 21:55:43', '2026-05-21 21:55:43');
MERGE INTO sys_config (`id`, `config_key`, `config_value`, `remark`, `create_time`, `update_time`) KEY(id) VALUES (21, 'installation_terms', '', NULL, '2026-07-23 23:30:07', '2026-07-23 23:30:07');
MERGE INTO sys_config (`id`, `config_key`, `config_value`, `remark`, `create_time`, `update_time`) KEY(id) VALUES (22, 'warranty_terms', '', NULL, '2026-07-23 23:30:07', '2026-07-23 23:30:07');

-- after_sale_order
MERGE INTO after_sale_order (`id`, `order_no`, `order_id`, `sale_order_no`, `customer_id`, `customer_name`, `type`, `source`, `description`, `status`, `assigned_to`, `scheduled_date`, `completed_date`, `resolution`, `cost`, `is_warranty`, `remark`, `images`, `create_time`, `update_time`, `deleted`) KEY(id) VALUES (1, 'SH20260115001', 1, 'XS20260101001', 1, '示例客户01', 'repair', 'phone', '示例售后：门窗把手松动', 'completed', '示例师傅', '2026-01-16', '2026-01-16', 'fixed', 0.00, 1, '示例售后单', NULL, '2026-01-15 10:00:00', '2026-01-15 10:00:00', 0);

-- 清理废弃的 AI 和 OSS 配置（兼容旧数据库升级）
DELETE FROM sys_config WHERE config_key IN ('ai_api_key', 'ai_base_url', 'ai_model', 'ai_max_tokens', 'ai_temperature', 'ai_api_format', 'ai_auth_type', 'ai_enabled', 'oss_endpoint', 'oss_bucket', 'oss_access_key', 'oss_secret_key', 'oss_domain');
DROP TABLE IF EXISTS ai_config;
