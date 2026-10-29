/**
 * trade.ts - 交易相关 API 接口（锁单、结算、退单）
 * =================================================
 *
 * 【对应后端模块】
 *   Controller: TradeController（交易控制器）
 *   Service: IMarketTradeService（交易服务接口）
 *   DTO: LockMarketPayOrderRequestDTO / SettlementMarketPayOrderRequestDTO / RefundMarketPayOrderRequestDTO
 *
 * 【交易流程（类比后端的拼团业务链路）】
 *   1. 锁单（lock）：用户点击"开团/参团"，系统创建订单并锁定库存
 *      → 相当于后端的 lockMarketPayOrder()，会校验库存、创建订单、加分布式锁
 *   2. 结算（settlement）：用户支付成功后，通知后端进行结算
 *      → 相当于后端的 settlementMarketPayOrder()，会校验支付时间、更新订单状态
 *   3. 退单（refund）：拼团失败或用户取消，进行退款
 *      → 相当于后端的 refundMarketPayOrder()，会走退单策略枚举路由
 *
 * 【前端概念】Promise 是什么？
 *   Promise = 异步操作的"凭证"，代表一个"未来才会有结果"的操作
 *   【类比后端】
 *     - 相当于 Java 的 CompletableFuture<T>
 *     - 后端：CompletableFuture<OrderDTO> future = asyncService.lockOrder(request)
 *     - 前端：const result: LockMarketPayOrderResponseDTO = await lockMarketPayOrder(request)
 *   - await 关键字 = 等待 Promise 完成（类比 future.get()）
 *   - .then() = Promise 完成后执行回调（类比 future.thenAccept()）
 */

import request from './request'  // 引入封装好的 Axios 实例
// 引入类型定义（只导入类型，不会打包到最终代码中）
import type {
  LockMarketPayOrderRequestDTO,       // 锁单请求参数
  LockMarketPayOrderResponseDTO,      // 锁单响应数据
  SettlementMarketPayOrderRequestDTO, // 结算请求参数
  SettlementMarketPayOrderResponseDTO,// 结算响应数据
  RefundMarketPayOrderRequestDTO,     // 退单请求参数
  RefundMarketPayOrderResponseDTO,    // 退单响应数据
} from './types'

// =============================================
// 一、拼团锁单接口
// =============================================

/**
 * 拼团锁单
 *
 * 【对应后端接口】
 *   POST /api/v1/gbm/trade/lock_market_pay_order
 *   Controller: TradeController.lockMarketPayOrder()
 *
 * 【业务含义】
 *   用户点击"开团"或"参团"时，调用此接口：
 *   - 检查商品库存（类比数据库的 SELECT ... FOR UPDATE）
 *   - 创建拼团订单（INSERT 订单记录）
 *   - 锁定库存（Redis 扣减 + 数据库更新）
 *   - 返回订单信息和团ID
 *
 * 【参数说明】
 *   params: LockMarketPayOrderRequestDTO
 *   - userId:       用户ID
 *   - teamId:       拼团ID（可选！为空 = 创建新团；有值 = 加入已有团）
 *   - activityId:   活动ID（关联到哪个拼团活动）
 *   - goodsId:      商品ID
 *   - source:       来源（APP/H5）
 *   - channel:      渠道（微信/支付宝）
 *   - outTradeNo:   外部交易单号（幂等键，防止重复提交，类比后端的 @Idempotent）
 *   - notifyConfigVO: 支付成功回调配置（通知类型、通知地址）
 *
 * 【返回值】
 *   Promise<LockMarketPayOrderResponseDTO>
 *   - orderId:          系统订单ID
 *   - originalPrice:    原价
 *   - deductionPrice:   优惠金额
 *   - payPrice:         实付价格
 *   - tradeOrderStatus: 订单状态
 *   - teamId:           拼团ID（开团时返回新建的团ID）
 *
 * 【类比后端】
 *   相当于 Service 方法签名：
 *   LockMarketPayOrderResponseDTO lockMarketPayOrder(LockMarketPayOrderRequestDTO request)
 *
 * 【使用示例】
 *   // 开团（teamId 为空）
 *   const result = await lockMarketPayOrder({
 *     userId: 'user001',
 *     teamId: '',           // 空字符串 = 创建新团
 *     activityId: 1001,
 *     goodsId: 'goods123',
 *     source: 'APP',
 *     channel: 'wechat',
 *     outTradeNo: 'ORDER_20240101_001',  // 保证幂等性
 *     notifyConfigVO: {
 *       notifyType: 'HTTP',
 *       notifyUrl: 'http://xxx/callback'
 *     }
 *   })
 *   console.log(result.teamId)   // 新创建的团ID
 *   console.log(result.payPrice) // 需要支付的价格
 *
 *   // 参团（teamId 有值）
 *   const result2 = await lockMarketPayOrder({
 *     ...params,
 *     teamId: 'team_abc123'     // 有值 = 加入这个团
 *   })
 */
export function lockMarketPayOrder(
  params: LockMarketPayOrderRequestDTO
): Promise<LockMarketPayOrderResponseDTO> {
  return request.post('/v1/gbm/trade/lock_market_pay_order', params)
}

// =============================================
// 二、拼团结算接口
// =============================================

/**
 * 拼团结算
 *
 * 【对应后端接口】
 *   POST /api/v1/gbm/trade/settlement_market_pay_order
 *   Controller: TradeController.settlementMarketPayOrder()
 *
 * 【业务含义】
 *   用户支付成功后，调用方（支付系统）通知拼团系统进行结算：
 *   - 校验支付时间是否在拼团有效期内（责任链 SettableRuleFilter 节点）
 *   - 更新订单状态为"已支付"
 *   - 判断是否满团，满团则触发成团回调
 *
 * 【参数说明】
 *   params: SettlementMarketPayOrderRequestDTO
 *   - source:       来源
 *   - channel:      渠道
 *   - userId:       用户ID
 *   - outTradeNo:   外部交易单号（与锁单时的 outTradeNo 对应）
 *   - outTradeTime: 交易时间，格式 yyyy-MM-dd HH:mm:ss（用于校验有效期）
 *
 * 【返回值】
 *   Promise<SettlementMarketPayOrderResponseDTO>
 *   - userId:     用户ID
 *   - teamId:     拼团ID
 *   - activityId: 活动ID
 *   - outTradeNo: 外部交易单号
 *
 * 【类比后端】
 *   相当于 Service 方法签名：
 *   SettlementMarketPayOrderResponseDTO settlementMarketPayOrder(
 *     SettlementMarketPayOrderRequestDTO request
 *   )
 *
 * 【使用示例】
 *   const result = await settlementMarketPayOrder({
 *     source: 'APP',
 *     channel: 'wechat',
 *     userId: 'user001',
 *     outTradeNo: 'ORDER_20240101_001',
 *     outTradeTime: '2024-01-01 12:00:00'  // 支付完成时间
 *   })
 */
export function settlementMarketPayOrder(
  params: SettlementMarketPayOrderRequestDTO
): Promise<SettlementMarketPayOrderResponseDTO> {
  return request.post('/v1/gbm/trade/settlement_market_pay_order', params)
}

// =============================================
// 三、拼团退单接口
// =============================================

/**
 * 拼团退单
 *
 * 【对应后端接口】
 *   POST /api/v1/gbm/trade/refund_market_pay_order
 *   Controller: TradeController.refundMarketPayOrder()
 *
 * 【业务含义】
 *   拼团失败（超时未满团）或用户主动取消时，进行退款：
 *   - 根据订单状态路由到不同的退单策略（策略模式）
 *   - 释放库存（回滚 Redis 和数据库）
 *   - 发送退单通知（RabbitMQ）
 *
 * 【参数说明】
 *   params: RefundMarketPayOrderRequestDTO
 *   - userId:     用户ID
 *   - outTradeNo: 外部交易单号（要退哪笔订单）
 *   - source:     来源
 *   - channel:    渠道
 *
 * 【返回值】
 *   Promise<RefundMarketPayOrderResponseDTO>
 *   - userId:  用户ID
 *   - orderId: 系统内部订单ID
 *   - teamId:  拼团ID
 *   - code:    退单结果状态码
 *   - info:    退单结果描述
 *
 * 【类比后端】
 *   相当于 Service 方法签名：
 *   RefundMarketPayOrderResponseDTO refundMarketPayOrder(
 *     RefundMarketPayOrderRequestDTO request
 *   )
 *
 * 【使用示例】
 *   const result = await refundMarketPayOrder({
 *     userId: 'user001',
 *     outTradeNo: 'ORDER_20240101_001',
 *     source: 'APP',
 *     channel: 'wechat'
 *   })
 *   if (result.code === '0000') {
 *     console.log('退单成功')
 *   }
 */
export function refundMarketPayOrder(
  params: RefundMarketPayOrderRequestDTO
): Promise<RefundMarketPayOrderResponseDTO> {
  return request.post('/v1/gbm/trade/refund_market_pay_order', params)
}
