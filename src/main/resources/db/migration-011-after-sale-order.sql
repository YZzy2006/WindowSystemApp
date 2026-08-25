-- 售后管理表
CREATE TABLE IF NOT EXISTS after_sale_order (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) DEFAULT NULL COMMENT '售后单号',
    order_id INT DEFAULT NULL COMMENT '关联销售单ID',
    sale_order_no VARCHAR(64) DEFAULT NULL COMMENT '关联销售单号',
    customer_id INT NOT NULL COMMENT '关联客户ID',
    customer_name VARCHAR(100) DEFAULT NULL COMMENT '客户名称',
    type VARCHAR(32) NOT NULL DEFAULT 'repair' COMMENT '类型：warranty质保维修/repair付费维修/install安装问题/complaint投诉',
    source VARCHAR(32) DEFAULT 'phone' COMMENT '来源：phone电话/wechat微信/visit上门',
    description TEXT COMMENT '问题描述',
    status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态：pending待处理/assigned已派单/processing处理中/completed已完成/closed已关闭',
    assigned_to VARCHAR(64) DEFAULT NULL COMMENT '安排的师傅',
    scheduled_date DATE DEFAULT NULL COMMENT '预约日期',
    completed_date DATE DEFAULT NULL COMMENT '完成日期',
    resolution VARCHAR(32) DEFAULT NULL COMMENT '处理方式：repair维修/replace更换部件/full_replace整单更换/refund退款',
    cost DECIMAL(10,2) DEFAULT 0.00 COMMENT '费用',
    is_warranty TINYINT DEFAULT 0 COMMENT '是否质保期内 0否1是',
    remark TEXT COMMENT '备注',
    images JSON DEFAULT NULL COMMENT '现场照片',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后管理';

-- 收付款表新增售后关联字段
ALTER TABLE payment ADD COLUMN after_sale_id INT DEFAULT NULL COMMENT '关联售后单ID';
