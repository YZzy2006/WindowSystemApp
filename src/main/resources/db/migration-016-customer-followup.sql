-- 客户跟进记录表
CREATE TABLE IF NOT EXISTS `customer_followup` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `customer_id` INT NOT NULL COMMENT '客户ID',
  `admin_id` INT DEFAULT NULL COMMENT '操作人ID',
  `username` VARCHAR(50) DEFAULT '' COMMENT '操作人用户名',
  `content` TEXT NOT NULL COMMENT '跟进内容',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户跟进记录';
