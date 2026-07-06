CREATE TABLE IF NOT EXISTS group_buy_activity (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增',
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    activity_name VARCHAR(128) NOT NULL COMMENT '活动名称',
    discount_id VARCHAR(8) NOT NULL COMMENT '折扣ID',
    group_type TINYINT NOT NULL DEFAULT 0 COMMENT '拼团方式（0自动成团、1达成目标拼团）',
    take_limit_count INT NOT NULL DEFAULT 1 COMMENT '拼团次数限制',
    target INT NOT NULL DEFAULT 1 COMMENT '拼团目标',
    valid_time INT NOT NULL DEFAULT 15 COMMENT '拼团时长（分钟）',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '活动状态（0创建、1生效、2过期、3废弃）',
    start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '活动开始时间',
    end_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '活动结束时间',
    tag_id VARCHAR(8) DEFAULT NULL COMMENT '人群标签规则标识',
    tag_scope VARCHAR(4) DEFAULT NULL COMMENT '人群标签规则范围',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_activity_id (activity_id)
);
