-- 门窗产品展示系统 数据库初始化脚本
-- 数据库名：window_db（需在MySQL中先创建）

CREATE DATABASE IF NOT EXISTS window_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE window_db;

-- 分类表
CREATE TABLE IF NOT EXISTS category (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    sort INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 产品表
CREATE TABLE IF NOT EXISTS product (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category_id INT,
    name VARCHAR(100),
    model VARCHAR(50) COMMENT '型号',
    material VARCHAR(50) COMMENT '材质',
    price DECIMAL(10,2) COMMENT '价格',
    cover_image VARCHAR(255) COMMENT '封面图',
    description TEXT,
    images TEXT COMMENT '逗号分隔路径',
    specs TEXT COMMENT 'JSON',
    price_tag VARCHAR(100) COMMENT '价格标签，如"含安装五金"',
    is_show TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 案例表
CREATE TABLE IF NOT EXISTS case_info (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100),
    image VARCHAR(255),
    images TEXT COMMENT '逗号分隔的多张图片URL',
    description TEXT,
    location VARCHAR(100) COMMENT '小区/位置',
    product_type VARCHAR(50) COMMENT '产品类型',
    profile_spec VARCHAR(100) COMMENT '型材规格',
    glass_config VARCHAR(100) COMMENT '玻璃配置',
    hardware_brand VARCHAR(50) COMMENT '五金品牌',
    before_image VARCHAR(255) COMMENT '改造前图片',
    customer_need TEXT COMMENT '客户需求',
    customer_review TEXT COMMENT '客户评价',
    install_duration VARCHAR(30) COMMENT '安装时长',
    sort INT DEFAULT 0 COMMENT '排序值，越小越靠前',
    is_show TINYINT DEFAULT 1 COMMENT '0隐藏 1展示',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 询价表
CREATE TABLE IF NOT EXISTS enquiry (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50),
    phone VARCHAR(20),
    content TEXT,
    remark VARCHAR(500) COMMENT '管理员备注',
    is_read TINYINT DEFAULT 0,
    need_measure TINYINT DEFAULT 0 COMMENT '是否需要上门量尺 0否 1是',
    budget VARCHAR(20) COMMENT '预算区间',
    is_measured TINYINT DEFAULT 0 COMMENT '是否已量尺 0否 1是',
    is_starred TINYINT DEFAULT 0 COMMENT '是否星标 0否 1是',
    is_completed TINYINT DEFAULT 0 COMMENT '是否完单 0否 1是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 管理员表
CREATE TABLE IF NOT EXISTS admin (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE,
    password VARCHAR(100),
    token_id VARCHAR(50) COMMENT '当前有效会话标识'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 登录日志表
CREATE TABLE IF NOT EXISTS login_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    admin_id INT NOT NULL,
    username VARCHAR(50),
    login_ip VARCHAR(50),
    user_agent VARCHAR(500),
    status TINYINT DEFAULT 1 COMMENT '1成功 0失败',
    fail_reason VARCHAR(200),
    remark VARCHAR(200) COMMENT '管理员备注',
    login_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    admin_id INT NOT NULL,
    username VARCHAR(50),
    module VARCHAR(50) COMMENT '模块',
    action VARCHAR(50) COMMENT '操作',
    target VARCHAR(200) COMMENT '目标',
    ip VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 站点配置表（单行，id=1）
CREATE TABLE IF NOT EXISTS site_config (
    id INT PRIMARY KEY DEFAULT 1,
    config_json MEDIUMTEXT COMMENT '首页各模块的 JSON 配置'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入默认管理员（首次登录时自动升级为 BCrypt 加密）
INSERT IGNORE INTO admin (username, password) VALUES ('admin', 'admin123');

-- 客户表（如果不存在则创建）
CREATE TABLE IF NOT EXISTS customer (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '客户名称',
    contact VARCHAR(50) COMMENT '联系人',
    phone VARCHAR(20) COMMENT '电话',
    address VARCHAR(200) COMMENT '地址',
    remark VARCHAR(500) COMMENT '备注',
    is_starred TINYINT DEFAULT 0 COMMENT '是否星标 0否 1是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 供应商表（如果不存在则创建）
CREATE TABLE IF NOT EXISTS supplier (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '供应商名称',
    contact VARCHAR(50) COMMENT '联系人',
    phone VARCHAR(20) COMMENT '电话',
    address VARCHAR(200) COMMENT '地址',
    remark VARCHAR(500) COMMENT '备注',
    is_starred TINYINT DEFAULT 0 COMMENT '是否星标 0否 1是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 系统配置表
CREATE TABLE IF NOT EXISTS sys_config (
    id INT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) UNIQUE NOT NULL COMMENT '配置键',
    config_value VARCHAR(500) COMMENT '配置值',
    remark VARCHAR(200) COMMENT '备注说明',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 收付款记录表
CREATE TABLE IF NOT EXISTS payment (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT DEFAULT NULL COMMENT '订单ID（可为空）',
    order_no VARCHAR(50) DEFAULT NULL COMMENT '订单编号',
    payment_date DATE NOT NULL COMMENT '日期',
    type VARCHAR(20) NOT NULL COMMENT '类型：receipt=收款 payment=付款',
    amount DECIMAL(12,2) NOT NULL COMMENT '金额',
    party_name VARCHAR(100) DEFAULT NULL COMMENT '对方名称',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    order_type VARCHAR(20) DEFAULT NULL COMMENT '关联订单类型: sale/presale/purchase/sale_return/purchase_return',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_order (order_id),
    KEY idx_order_no (order_no),
    KEY idx_date (payment_date),
    KEY idx_type (type),
    KEY idx_party (party_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单编辑锁表
CREATE TABLE IF NOT EXISTS order_lock (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_type VARCHAR(20) NOT NULL COMMENT '订单类型: sale/presale/purchase/sale_return/purchase_return',
    order_id INT NOT NULL COMMENT '订单ID',
    admin_id INT NOT NULL COMMENT '锁定人ID',
    username VARCHAR(50) COMMENT '锁定人用户名',
    lock_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '锁定时间',
    UNIQUE KEY uk_order (order_type, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入默认系统配置
INSERT IGNORE INTO sys_config (config_key, config_value, remark) VALUES
('sale_order_prefix', 'XS', '销售单号前缀'),
('purchase_order_prefix', 'CG', '采购单号前缀'),
('sale_return_prefix', 'XT', '销售退货单号前缀'),
('purchase_return_prefix', 'CT', '采购退货单号前缀'),
('stock_in_prefix', 'RK', '入库单号前缀'),
('stock_out_prefix', 'CK', '出库单号前缀'),
('company_name', '顺居门业', '公司名称'),
('company_phone', '18948846839', '公司电话'),
('company_address', '乳源县二九一物流仓库', '公司地址'),
('print_title_sale', '顺居门业客户销售单', '销售单打印标题'),
('print_title_presale', '顺居门窗客户预算单', '预算单打印标题'),
('presale_order_prefix', 'YS', '预算单号前缀');
