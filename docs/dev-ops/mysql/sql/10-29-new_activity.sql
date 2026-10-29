-- 1. 折扣规则
INSERT INTO group_buy_discount (discount_id, discount_name, discount_desc, discount_type, market_plan, market_expr)
VALUES ('25120301', '新人直减30', '新用户专享直减30元', 0, 'ZJ', '30');

-- 2. 活动
INSERT INTO group_buy_activity (activity_id, activity_name, discount_id, group_type, take_limit_count, target, valid_time, status, start_time, end_time)
VALUES (100456, '双十一拼团特惠', '25120301', 0, 1, 3, 15, 1, NOW(), '2026-12-31 23:59:59');

-- 3. 商品-活动关联
INSERT INTO sc_sku_activity (source, channel, activity_id, goods_id)
VALUES ('s01', 'c01', 100456, '9890002');
