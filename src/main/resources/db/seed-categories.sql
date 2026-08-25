-- 顺居门业 产品分类（基于实际主营业务）
-- 乳源瑶族自治县 · 门窗定制专家

USE window_db;

DELETE FROM category;

-- ==================== 一、金属门窗系列 ====================
-- 1.1 断桥隔热铝合金门窗（主营）
INSERT INTO category (id, name, sort) VALUES (1,  '断桥铝平开窗',           1);
INSERT INTO category (id, name, sort) VALUES (2,  '断桥铝推拉窗',           2);
INSERT INTO category (id, name, sort) VALUES (3,  '断桥铝平开门',           3);
INSERT INTO category (id, name, sort) VALUES (4,  '断桥铝推拉门',           4);
INSERT INTO category (id, name, sort) VALUES (5,  '封阳台 / 封窗改造',     5);

-- 1.2 不锈钢防盗门窗
INSERT INTO category (id, name, sort) VALUES (6,  '不锈钢防盗门',           6);
INSERT INTO category (id, name, sort) VALUES (7,  '不锈钢防盗窗',           7);
INSERT INTO category (id, name, sort) VALUES (8,  '不锈钢商铺门',           8);

-- ==================== 二、木质门系列 ====================
INSERT INTO category (id, name, sort) VALUES (9,  '实木复合门',             9);
INSERT INTO category (id, name, sort) VALUES (10, '模压门',                10);
INSERT INTO category (id, name, sort) VALUES (11, '定制室内木门',          11);

-- ==================== 三、门窗配件及辅助产品 ====================
INSERT INTO category (id, name, sort) VALUES (12, '密封胶条（耐候防老化）', 12);
INSERT INTO category (id, name, sort) VALUES (13, '五金锁具 / 滑轨铰链',   13);
INSERT INTO category (id, name, sort) VALUES (14, '中空玻璃',              14);
INSERT INTO category (id, name, sort) VALUES (15, '防护栏杆 / 简易护栏',   15);

-- ==================== 四、服务延伸 ====================
INSERT INTO category (id, name, sort) VALUES (16, '旧窗改造拆换（含墙体修补）', 16);
INSERT INTO category (id, name, sort) VALUES (17, '自建房门窗定制方案',         17);
INSERT INTO category (id, name, sort) VALUES (18, '维修耗材包（密封胶/润滑剂）', 18);
