-- 订单编辑锁表：防止多人同时编辑同一订单
CREATE TABLE IF NOT EXISTS order_lock (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_type VARCHAR(20) NOT NULL COMMENT '订单类型: sale/purchase/sale_return/purchase_return',
    order_id INT NOT NULL COMMENT '订单ID',
    admin_id INT NOT NULL COMMENT '加锁用户ID',
    username VARCHAR(50) NOT NULL COMMENT '加锁用户名',
    lock_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加锁时间',
    UNIQUE KEY uk_order (order_type, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
