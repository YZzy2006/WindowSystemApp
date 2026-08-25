-- 给 customer.name 加唯一约束，防止并发创建重复客户
-- 先清理已有的重复数据（保留 id 最小的那条）
DELETE c1 FROM customer c1
INNER JOIN customer c2 ON c1.name = c2.name AND c1.id > c2.id;

ALTER TABLE customer ADD UNIQUE KEY uk_name (name);
