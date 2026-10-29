# 拼团营销市场系统（Group Buy Market）

基于 DDD（领域驱动设计）分层架构实现的拼团营销交易系统，支持开团/参团、锁单、支付结算、回调通知等核心业务流程。

## 技术栈

| 类别 | 技术 |
|------|------|
| 开发语言 | Java 8 |
| 核心框架 | Spring Boot 2.7.12 |
| 持久层 | MyBatis + MySQL 8.x |
| 缓存 | Redis（Redisson 3.26.0） |
| 消息队列 | RabbitMQ |
| 鉴权 | JWT（java-jwt 4.4.0） |
| 日志 | Logback + Logstash（ELK 上报） |
| 监控 | Prometheus + Grafana |
| 部署 | Docker Compose |

## 项目结构

```
group-buy-market-hth
├── group-buy-market-hth-api            # 接口层 - 对外暴露的 RPC/HTTP 接口定义
├── group-buy-market-hth-app            # 应用层 - 业务流程编排、配置管理
├── group-buy-market-hth-domain         # 领域层 - 核心业务逻辑（活动/交易/标签）
├── group-buy-market-hth-infrastructure # 基础设施层 - 数据库、Redis、MQ、网关
├── group-buy-market-hth-trigger        # 触发器层 - HTTP Controller、定时任务、MQ 监听
├── group-buy-market-hth-types          # 公共类型 - 枚举、异常、通用对象
└── docs/dev-ops                        # 运维部署 - Docker Compose、SQL、配置
```

## 核心功能

### 拼团交易

- **锁单**：用户开团/参团时预锁定订单，扣减 Redis 库存，创建拼团记录
- **结算**：支付成功后触发结算流程，更新订单状态，通过责任链规则过滤
- **回调通知**：拼团结果通过 HTTP/MQ 双通道回调，配合定时任务补偿确保可靠送达

### 营销活动

- **活动管理**：配置拼团活动（目标人数、有效时长、参与次数限制）
- **折扣试算**：基于决策树架构实现多级营销折扣计算（直减、满减、N元购）
- **人群标签**：基于用户行为数据打标，支持按人群限制活动可见性和参与资格

### 并发控制

- Redis `setNx` 原子操作 + 数据库唯一索引，实现拼团库存的并发安全
- 令牌桶限流策略保护核心接口
- 幂等设计（outTradeNo + biz_id 双重防重）

### 基础设施

- **Redis 分布式锁**：保障关键业务的并发安全
- **Redis 缓存**：热点数据缓存加速
- **RabbitMQ 消息队列**：异步解耦、削峰填谷
- **ELK 日志收集**：全链路日志上报与检索
- **Prometheus + Grafana 监控**：系统运行状态可视化

## 快速启动

### 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 8.x
- Redis 6.x+
- RabbitMQ 3.x

### 1. 初始化数据库

```bash
mysql -u root -p < docs/dev-ops/mysql/sql/2-29-group_buy_market.sql
```

### 2. 修改配置

编辑 `group-buy-market-hth-app/src/main/resources/application.yml`，配置数据库、Redis、MQ 连接信息。

### 3. 启动后端

```bash
mvn clean install
cd group-buy-market-hth-app
mvn spring-boot:run
```

### 4. Docker 部署

```bash
cd docs/dev-ops
# 本地环境
docker-compose -f docker-compose-environment.yml up -d
# 阿里云环境
docker-compose -f docker-compose-environment-aliyun.yml up -d
```

## 数据库设计

| 表名 | 说明 |
|------|------|
| `group_buy_activity` | 拼团活动配置 |
| `group_buy_discount` | 营销折扣方案 |
| `group_buy_order` | 拼团组队主单 |
| `group_buy_order_list` | 用户拼团订单明细 |
| `sku` | 商品信息 |
| `sc_sku_activity` | 渠道-商品-活动关联 |
| `crowd_tags` | 人群标签 |
| `crowd_tags_detail` | 人群标签明细 |
| `crowd_tags_job` | 标签计算任务 |
| `notify_task` | 回调通知任务表 |

## 架构设计要点

- **DDD 分层架构**：领域层与基础设施层解耦，通过仓储接口（Repository）依赖倒置
- **决策树 + 策略模式**：折扣计算和交易规则通过责任链动态编排，支持灵活扩展
- **双重回调保障**：线程池异步触发 + 定时任务补偿，确保拼团结果通知可靠送达
- **本地消息表**：通过 `notify_task` 表实现最终一致性，支持重试和状态追踪
