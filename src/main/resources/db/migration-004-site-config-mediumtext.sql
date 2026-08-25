-- 升级 site_config.config_json 从 TEXT (64KB) 到 MEDIUMTEXT (16MB)
-- 解决「关于我们」等大配置保存时因超出 TEXT 限制而失败的问题
ALTER TABLE site_config MODIFY COLUMN config_json MEDIUMTEXT COMMENT '首页各模块的 JSON 配置';
