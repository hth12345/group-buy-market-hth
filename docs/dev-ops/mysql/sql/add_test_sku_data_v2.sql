-- 添加测试商品数据到 sku 表
-- 执行时间: 2026-11-03
-- 说明: 为拼团活动添加测试商品信息
-- 使用方法: 在 IDEA 的数据库控制台中选择 group_buy_market 数据库，然后执行此脚本

USE group_buy_market;

-- 方式1: 添加多个测试商品（推荐）
INSERT INTO `sku` (`source`, `channel`, `goods_id`, `goods_name`, `original_price`, `create_time`, `update_time`)
VALUES
    ('s01', 'c01', '9890002', '《Java并发编程实战》', 89.00, NOW(), NOW()),
    ('s01', 'c01', '9890003', '《深入理解Java虚拟机》', 129.00, NOW(), NOW()),
    ('s01', 'c01', '9890004', '《Spring Boot实战》', 79.00, NOW(), NOW()),
    ('s01', 'c01', '9890005', '《MySQL必知必会》', 59.00, NOW(), NOW()),
    ('s01', 'c01', '9890006', '《Redis设计与实现》', 99.00, NOW(), NOW()),
    ('s01', 'c01', '9890007', '《Docker技术入门与实战》', 69.00, NOW(), NOW()),
    ('s01', 'c01', '9890008', '《微服务架构设计模式》', 149.00, NOW(), NOW()),
    ('s01', 'c01', '9890009', '《消息队列RabbitMQ实战》', 89.00, NOW(), NOW()),
    ('s01', 'c01', '9890010', '《Elasticsearch权威指南》', 119.00, NOW(), NOW());

-- 验证数据插入成功
SELECT * FROM sku WHERE goods_id >= '9890002';

-- 如果需要添加与活动的关联，执行以下 SQL
-- INSERT INTO `sc_sku_activity` (`source`, `channel`, `activity_id`, `goods_id`, `create_time`, `update_time`)
-- VALUES
--     ('s01', 'c01', 100123, '9890002', NOW(), NOW()),
--     ('s01', 'c01', 100123, '9890003', NOW(), NOW()),
--     ('s01', 'c01', 100123, '9890004', NOW(), NOW());
