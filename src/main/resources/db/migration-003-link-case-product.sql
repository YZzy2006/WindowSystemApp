-- 给已导入的 6 个测试案例关联产品（product_id 填对应的真实产品 ID）
-- 需先执行 migration-002-case-product.sql 添加列

USE window_db;

-- 案例1 示例小区断桥铝平开窗 → 产品1 (70系列断桥铝内开内倒窗)
UPDATE case_info SET product_id = 1 WHERE title LIKE '%示例小区%';

-- 案例2 示例镇A自建房 → 产品2 (90系列断桥铝外开窗)
UPDATE case_info SET product_id = 2 WHERE title LIKE '%示例镇A%';

-- 案例3 老城区推拉窗封阳台 → 产品3 (100系列推拉窗)
UPDATE case_info SET product_id = 3 WHERE title LIKE '%阳台%';

-- 案例4 商铺不锈钢防盗门 → 产品7 (304不锈钢防盗门)
UPDATE case_info SET product_id = 7 WHERE title LIKE '%早餐店%';

-- 案例5 示例镇B室内木门 → 产品9 (实木复合烤漆门)
UPDATE case_info SET product_id = 9 WHERE title LIKE '%示例镇B%';

-- 案例6 示例镇D旧窗拆换 → 产品1 (70系列断桥铝)
UPDATE case_info SET product_id = 1 WHERE title LIKE '%示例镇D%';
