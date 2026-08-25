-- admin 表增加角色和冻结字段
ALTER TABLE `admin` ADD COLUMN `role` VARCHAR(20) NOT NULL DEFAULT 'admin' COMMENT '角色: super_admin / admin';
ALTER TABLE `admin` ADD COLUMN `is_frozen` TINYINT NOT NULL DEFAULT 0 COMMENT '是否冻结: 0=正常 1=冻结';

-- 将第一个管理员设为超级管理员
UPDATE `admin` SET `role` = 'super_admin' WHERE `id` = 1;
