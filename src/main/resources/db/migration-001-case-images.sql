-- 案例表新增字段
ALTER TABLE case_info ADD COLUMN images TEXT COMMENT '逗号分隔的多张图片URL' AFTER `image`;
ALTER TABLE case_info ADD COLUMN sort INT DEFAULT 0 COMMENT '排序值，越小越靠前' AFTER `description`;
ALTER TABLE case_info ADD COLUMN is_show TINYINT DEFAULT 1 COMMENT '0隐藏 1展示' AFTER `sort`;
