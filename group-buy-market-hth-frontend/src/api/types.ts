/**
 * types.ts - TypeScript 类型定义
 * ================================
 * 【前端概念】TypeScript 接口 = 定义数据的"形状"，规定一个对象有哪些字段、什么类型
 * 【类比后端】相当于 Java 的 DTO 类（如 GoodsMarketRequestDTO.java）
 *   - Java:  private String userId;
 *   - TS:    userId: string
 *
 * 【为什么要定义类型】
 * 1. 代码提示：写代码时 VS Code 会自动提示有哪些字段
 * 2. 类型检查：传错字段类型会在编译时报错（类比 Java 编译期检查）
 * 3. 文档作用：看接口定义就知道请求/响应的数据结构
 *
 * 【命名约定】
 * - 请求类型以 Request 结尾（类比后端的 XxxRequestDTO）
 * - 响应类型以 Response 结尾（类比后端的 XxxResponseDTO）
 * - 保持与后端 DTO 类名对应，方便前后端联调时对照
 */

// =============================================
// 一、通用响应类型（类比后端的 Response<T> 统一返回结构）
// =============================================

/**
 * 后端统一响应结构
 * 【类比后端】com.hth.api.response.Response<T>
 *
 * 后端所有接口都返回这个格式：
 * {
 *   "code": "0000",      ← 状态码，"0000" 表示成功
 *   "info": "成功",       ← 状态描述信息
 *   "data": { ... }      ← 真正的业务数据（泛型 T）
 * }
 *
 * 【前端概念】泛型 <T> = 类型的"参数"，data 的具体类型由调用方决定
 * 【类比后端】相当于 Java 泛型 Response<T>，T 可以是 GoodsMarketResponseDTO、Boolean 等
 */
export interface ApiResponse<T> {
  code: string    // 状态码，"0000" 表示成功（类比 Response.SUCCESS_CODE）
  info: string    // 状态描述（类比 Response.info 字段）
  data: T         // 业务数据，泛型（类比 Response.data 字段）
}

// =============================================
// 二、首页拼团配置查询（对应 GoodsMarketRequestDTO / GoodsMarketResponseDTO）
// =============================================

/**
 * 首页查询 - 请求参数
 * 【类比后端】com.hth.api.dto.GoodsMarketRequestDTO
 *
 * 前端调用首页接口时，需要传给后端的参数
 */
export interface GoodsMarketRequestDTO {
  userId: string    // 用户ID（标识当前用户）
  source: string    // 来源（如 APP、H5、小程序）
  channel: string   // 渠道（如微信、支付宝）
  goodsId: string   // 商品ID（查询哪个商品的拼团信息）
}

/**
 * 首页查询 - 响应中的「商品信息」
 * 【类比后端】GoodsMarketResponseDTO 内嵌的 goods 对象
 */
export interface GoodsInfo {
  goodsId: string          // 商品ID
  originalPrice: number    // 原价（单位：元）
  deductionPrice: number   // 优惠减免金额
  payPrice: number         // 实际支付价格 = 原价 - 优惠
}

/**
 * 首页查询 - 响应中的「拼团信息」
 * 【类比后端】GoodsMarketResponseDTO 内嵌的 teamList 数组元素
 *
 * 每一条代表一个正在进行的拼团
 */
export interface TeamInfo {
  userId: string           // 团长用户ID
  teamId: string           // 拼团ID（唯一标识一个团）
  activityId: number       // 活动ID（关联到哪个拼团活动）
  targetCount: number      // 成团目标人数（如 3 人团就是 3）
  completeCount: number    // 已成团人数（还差 targetCount - completeCount 人）
  lockCount: number        // 已锁单人数（已下单但未支付的）
  validStartTime: string   // 拼团有效期开始时间
  validEndTime: string     // 拼团有效期截止时间
  validTimeCountdown: string // 剩余倒计时描述（如 "还剩2小时"）
  outTradeNo: string       // 外部交易单号（团长的订单号）
}

/**
 * 首页查询 - 响应中的「拼团统计」
 * 【类比后端】GoodsMarketResponseDTO 内嵌的 teamStatistic 对象
 */
export interface TeamStatistic {
  allTeamCount: number          // 总团数
  allTeamCompleteCount: number  // 已成团团数
  allTeamUserCount: number      // 参与总人数
}

/**
 * 首页查询 - 完整响应数据
 * 【类比后端】com.hth.api.dto.GoodsMarketResponseDTO
 */
export interface GoodsMarketResponseDTO {
  activityId: number        // 拼团活动ID
  goods: GoodsInfo          // 商品信息
  teamList: TeamInfo[]      // 进行中的拼团列表（Java 的 List<TeamInfo> = TS 的 TeamInfo[]）
  teamStatistic: TeamStatistic // 拼团统计数据
}

// =============================================
// 三、拼团锁单（对应 LockMarketPayOrderRequestDTO / LockMarketPayOrderResponseDTO）
// =============================================

/**
 * 锁单请求中的「通知配置」
 * 【类比后端】LockMarketPayOrderRequestDTO 内嵌的 notifyConfigVO
 *
 * 支付成功后，后端通过这个配置通知调用方（类似回调 webhook）
 */
export interface NotifyConfigVO {
  notifyType: string   // 通知类型（如 HTTP、MQ）
  notifyUrl: string    // 通知地址（回调 URL）
  notifyMQ?: string    // 消息队列地址（可选，? 表示该字段可以不存在）
}

/**
 * 拼团锁单 - 请求参数
 * 【类比后端】com.hth.api.dto.LockMarketPayOrderRequestDTO
 *
 * 用户点击"开团/参团"时，前端提交的锁单请求
 */
export interface LockMarketPayOrderRequestDTO {
  userId: string           // 用户ID
  teamId: string           // 拼团ID（可选，为空则创建新团）
  activityId: number       // 活动ID
  goodsId: string          // 商品ID
  source: string           // 来源
  channel: string          // 渠道
  outTradeNo: string       // 外部交易单号（幂等键，防止重复提交）
  notifyConfigVO: NotifyConfigVO // 支付成功回调配置
}

/**
 * 拼团锁单 - 响应数据
 * 【类比后端】com.hth.api.dto.LockMarketPayOrderResponseDTO
 *
 * 锁单成功后，后端返回的订单信息
 */
export interface LockMarketPayOrderResponseDTO {
  orderId: string          // 系统内部订单ID
  originalPrice: number    // 商品原价
  deductionPrice: number   // 优惠金额
  payPrice: number         // 实际应付价格
  tradeOrderStatus: number // 交易订单状态（数字枚举，如 1=待支付 2=已支付）
  teamId: string           // 拼团ID（如果是开团，这里返回新建的团ID）
}

// =============================================
// 四、拼团结算（对应 SettlementMarketPayOrderRequestDTO / SettlementMarketPayOrderResponseDTO）
// =============================================

/**
 * 拼团结算 - 请求参数
 * 【类比后端】com.hth.api.dto.SettlementMarketPayOrderRequestDTO
 *
 * 用户支付完成后，调用方通知后端进行结算
 */
export interface SettlementMarketPayOrderRequestDTO {
  source: string       // 来源
  channel: string      // 渠道
  userId: string       // 用户ID
  outTradeNo: string   // 外部交易单号（与锁单时的 outTradeNo 对应）
  outTradeTime: string // 交易时间，格式 yyyy-MM-dd HH:mm:ss
}

/**
 * 拼团结算 - 响应数据
 * 【类比后端】com.hth.api.dto.SettlementMarketPayOrderResponseDTO
 */
export interface SettlementMarketPayOrderResponseDTO {
  userId: string       // 用户ID
  teamId: string       // 拼团ID
  activityId: number   // 活动ID
  outTradeNo: string   // 外部交易单号
}

// =============================================
// 五、拼团退单（对应 RefundMarketPayOrderRequestDTO / RefundMarketPayOrderResponseDTO）
// =============================================

/**
 * 拼团退单 - 请求参数
 * 【类比后端】com.hth.api.dto.RefundMarketPayOrderRequestDTO
 *
 * 需要退款时，调用方提交的退单请求
 */
export interface RefundMarketPayOrderRequestDTO {
  userId: string       // 用户ID
  outTradeNo: string   // 外部交易单号（要退哪笔订单）
  source: string       // 来源
  channel: string      // 渠道
}

/**
 * 拼团退单 - 响应数据
 * 【类比后端】com.hth.api.dto.RefundMarketPayOrderResponseDTO
 */
export interface RefundMarketPayOrderResponseDTO {
  userId: string       // 用户ID
  orderId: string      // 系统内部订单ID
  teamId: string       // 拼团ID
  code: string         // 退单结果状态码
  info: string         // 退单结果描述
}
