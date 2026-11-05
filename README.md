q# 拼团营销市场系统（Group Buy Market）

一个完整的拼团营销交易平台，采用前后端分离架构。后端基于 DDD（领域驱动设计）分层架构实现，前端使用 Vue 3 + TypeScript 构建拼多多风格的移动端商城页面。支持开团/参团、锁单、支付宝支付、结算、退款逆向、回调通知、人群标签、营销折扣试算等完整业务闭环。

## 技术栈

### 后端

| 类别 | 技术 |
|------|------|
| 开发语言 | Java 8 |
| 核心框架 | Spring Boot 2.7.12 |
| 持久层 | MyBatis + MySQL 8.x |
| 缓存 | Redis（Redisson 3.26.0） |
| 消息队列 | RabbitMQ |
| 日志 | Logback + Logstash（ELK 上报） |
| 监控 | Prometheus + Grafana |
| 部署 | Docker Compose |

### 前端

| 类别 | 技术 |
|------|------|
| 框架 | Vue 3 + TypeScript |
| 构建工具 | Vite |
| UI 组件库 | Element Plus |
| 状态管理 | Pinia |
| 路由 | Vue Router（History 模式） |
| HTTP 客户端 | Axios |

## 项目结构

```
group-buy-market-hth
├── group-buy-market-hth-api            # 接口层 - 对外暴露的 RPC/HTTP 接口定义（DTO）
├── group-buy-market-hth-app            # 应用层 - 业务流程编排、配置管理
├── group-buy-market-hth-domain         # 领域层 - 核心业务逻辑（活动/交易/标签/退款）
├── group-buy-market-hth-infrastructure # 基础设施层 - 数据库、Redis、MQ、网关
├── group-buy-market-hth-trigger        # 触发器层 - HTTP Controller、定时任务、MQ 监听
├── group-buy-market-hth-types          # 公共类型 - 枚举、异常、通用对象
├── group-buy-market-hth-frontend       # 前端工程 - Vue 3 拼多多风格商城
│   ├── src/api/                        #   API 接口封装（锁单、结算、退单、支付）
│   ├── src/views/                      #   页面组件（登录、首页、详情、支付、订单）
│   ├── src/components/                 #   公共组件（倒计时、支付弹窗、拼团卡片）
│   ├── src/stores/                     #   Pinia 状态管理
│   └── src/router/                     #   路由配置 + 导航守卫
└── docs/dev-ops                        # 运维部署 - Docker Compose、SQL、配置
```

## 核心功能

### 拼团交易（正向流程）

- **锁单**：用户开团/参团时预锁定订单，Redis 原子扣减库存，创建拼团记录
- **支付**：对接支付宝，前端调用支付后端创建支付订单，用户完成支付
- **结算**：支付成功后触发结算流程，通过决策树责任链规则过滤，更新订单状态
- **回调通知**：拼团结果通过 HTTP/MQ 双通道回调，配合定时任务补偿（每 3 分钟扫描）确保可靠送达

### 退款逆向流程

- **未支付超时退单**（unpaid2Refund）：锁单后超时未支付，自动退单并恢复库存
- **已支付未成团退单**（paid2Refund）：支付后拼团未达成目标人数，触发退款
- **已支付已成团退单**（paidTeam2Refund）：成团后个别用户退单，最后一人退单则整团失败
- **策略模式**：通过 `IRefundOrderStrategy` 接口 + 枚举路由，实现不同退单场景的策略分发
- **库存恢复**：退单后通过 MQ 消息触发库存回补，保证数据一致性

### 营销活动

- **活动管理**：配置拼团活动（目标人数、有效时长、参与次数限制）
- **折扣试算**：基于决策树架构实现多级营销折扣计算（直减、满减、N元购），节点包括 RootNode → SwitchRoot → TagNode → MarketNode → EndNode
- **人群标签**：基于用户行为数据打标，支持按人群限制活动可见性和参与资格
- **SC 分层校验**：试算阶段前置拦截 → 锁单阶段轻量处理 → 支付阶段最终确认

### 并发控制

- Redis `setNx` 原子操作 + 数据库唯一索引，实现拼团库存的并发安全
- 令牌桶限流策略保护核心接口
- 幂等设计（outTradeNo + biz_id 双重防重）
- 数据库事务保障（`@Transactional` 超时 5 秒）

### 前端页面

| 页面 | 功能 |
|------|------|
| **LoginView** | 用户登录（演示系统，仅输入用户名即可） |
| **HomeView** | 商城首页，展示活动列表、商品卡片（拼多多风格） |
| **DetailView** | 商品详情，支持开团/参团、拼团列表、倒计时 |
| **PayView** | 支付确认，对接支付宝、模拟支付、退单操作 |
| **OrdersView** | 我的订单，状态筛选、订单详情、退单操作 |

### 基础设施

- **Redis 分布式锁**：保障关键业务的并发安全
- **Redis 缓存**：热点数据缓存加速（活动、商品、库存）
- **RabbitMQ 消息队列**：异步解耦、退单通知、库存恢复
- **本地消息表**（notify_task）：实现最终一致性，支持重试和状态追踪
- **ELK 日志收集**：全链路日志上报与检索
- **Prometheus + Grafana 监控**：系统运行状态可视化

## 微服务架构

```
前端 (Vue 3)
  ├── /api ──────→ 拼团后端 (group-buy-market-hth:8091)
  │                   ├── 锁单接口
  │                   ├── 结算接口
  │                   ├── 退单接口
  │                   └── 活动查询接口
  └── /pay-api ──→ 支付后端 (s-pay-mall-ddd:8080)
                      ├── 创建支付订单
                      ├── 支付宝对接
                      └── 支付状态查询
```

## 快速启动

### 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 8.x
- Redis 6.x+
- RabbitMQ 3.x
- Node.js 18+（前端）

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

### 4. 启动前端

```bash
cd group-buy-market-hth-frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，后端运行在 `http://localhost:8091`。

### 5. Docker 部署

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
| `group_buy_activity` | 拼团活动配置（目标人数、有效时长、参与限制） |
| `group_buy_discount` | 营销折扣方案（直减、满减、N元购） |
| `group_buy_order` | 拼团组队主单（团号、状态、回调配置） |
| `group_buy_order_list` | 用户拼团订单明细（用户、商品、金额、状态） |
| `sku` | 商品信息 |
| `sc_sku_activity` | 渠道-商品-活动关联 |
| `crowd_tags` | 人群标签 |
| `crowd_tags_detail` | 人群标签明细 |
| `crowd_tags_job` | 标签计算任务 |
| `notify_task` | 回调通知任务表（本地消息表） |

## 架构设计要点

- **DDD 分层架构**：领域层与基础设施层解耦，通过仓储接口（Repository）依赖倒置
- **决策树 + 策略模式**：折扣计算、交易规则、退款策略通过责任链/策略模式动态编排
- **双重回调保障**：线程池异步触发 + 定时任务补偿（每 3 分钟），确保拼团结果通知可靠送达
- **本地消息表**：通过 `notify_task` 表实现最终一致性，支持重试计数（最多 5 次）和状态追踪
- **分层校验**：SC 黑名单在试算、锁单、支付三阶段分层拦截，兼顾性能与安全
- **前后端分离**：Vue 3 前端 + Spring Boot 后端 + 支付微服务，通过 API 网关解耦
