/**
 * index.ts - 首页相关 API 接口
 * ==============================
 *
 * 【前端概念】API 模块 = 把后端接口封装成函数，调用方不用关心请求细节
 * 【类比后端】
 *   - 相当于后端 Service 层调用 DAO 的模式
 *   - Controller 暴露接口 → Service 封装业务逻辑 → DAO 操作数据库
 *   - 前端：页面组件调用 API 函数 → API 函数内部用 Axios 发请求
 *
 * 【为什么要分模块？】
 *   - 按业务领域拆分文件，避免一个文件几百行
 *   - index.ts 负责首页相关接口（查询拼团配置）
 *   - trade.ts 负责交易相关接口（锁单、结算、退单）
 *   - 类比后端：IMarketTradeService（交易）、IMarketIndexService（首页）分开
 *
 * 【这个文件封装的接口】
 *   POST /api/v1/gbm/index/query_group_buy_market_config
 *   对应后端：IMarketIndexService.queryGroupBuyMarketConfig()
 */

import request from './request'  // 引入封装好的 Axios 实例（类比 @Autowired HttpClientUtil）
// 引入类型定义（type-only import，编译后会被擦除，只用于类型检查）
// 【类比后端】相当于 import DTO 类，但只用在方法签名的参数/返回值类型上
import type {
  GoodsMarketRequestDTO,     // 首页查询请求参数类型
  GoodsMarketResponseDTO,    // 首页查询响应数据类型
  QueryAllActivitiesRequestDTO,   // 查询所有活动 - 请求参数
  AllActivitiesResponseDTO,       // 查询所有活动 - 响应数据
} from './types'

/**
 * 查询首页拼团配置
 *
 * 【对应后端接口】
 *   POST /api/v1/gbm/index/query_group_buy_market_config
 *   Controller: IndexController.queryGroupBuyMarketConfig()
 *   Service: IMarketIndexService.queryGroupBuyMarketConfig()
 *
 * 【参数说明】
 *   params: GoodsMarketRequestDTO
 *   - userId:  用户ID（查询该用户的拼团信息）
 *   - source:  来源（APP/H5/小程序）
 *   - channel: 渠道（微信/支付宝）
 *   - goodsId: 商品ID（查询哪个商品的拼团配置）
 *
 * 【返回值】
 *   Promise<GoodsMarketResponseDTO>
 *   - 包含活动ID、商品信息、进行中的拼团列表、统计数据
 *   - 注意：request.post 返回的已经是解包后的 data 部分（响应拦截器处理过）
 *
 * 【类比后端】
 *   相当于 Service 方法签名：
 *   GoodsMarketResponseDTO queryGroupBuyMarketConfig(GoodsMarketRequestDTO request)
 *
 * 【使用示例】
 *   // 在 Vue 组件中调用：
 *   const data = await queryGroupBuyMarketConfig({
 *     userId: 'user001',
 *     source: 'APP',
 *     channel: 'wechat',
 *     goodsId: 'goods123'
 *   })
 *   console.log(data.goods.payPrice)  // 商品价格
 *   console.log(data.teamList)         // 拼团列表
 */
export function queryGroupBuyMarketConfig(
  params: GoodsMarketRequestDTO
): Promise<GoodsMarketResponseDTO> {
  // request.post(url, data) 发送 POST 请求
  // url 会自动拼接 baseURL，最终请求：/api/v1/gbm/index/query_group_buy_market_config
  // 【类比后端】相当于 RestTemplate.postForObject(url, request, Response.class)
  return request.post('/v1/gbm/index/query_group_buy_market_config', params)
}

/**
 * 查询所有活动列表
 *
 * 【对应后端接口】
 *   POST /api/v1/gbm/index/query_all_activities
 *   Controller: MarketIndexController.queryAllActivities()
 *   Service: IMarketIndexService.queryAllActivities()
 *
 * 【参数说明】
 *   request: QueryAllActivitiesRequestDTO
 *   - userId:  用户ID（必填，用于判断是否可参与该活动）
 *   - source:  来源（可选）
 *   - channel: 渠道（可选）
 *
 * 【返回值】
 *   Promise<AllActivitiesResponseDTO[]>
 *   - 返回一个数组，每个元素是一个活动卡片数据
 *   - 后端已过滤 visible=true，前端拿到的都是可见活动
 *   - 【类比后端】相当于 List<AllActivitiesResponseDTO>
 *
 * 【使用示例】
 *   const activities = await queryAllActivities({ userId: 'user001' })
 *   activities.forEach(a => console.log(a.goodsName, a.payPrice))
 */
export function queryAllActivities(
  params: QueryAllActivitiesRequestDTO
): Promise<AllActivitiesResponseDTO[]> {
  // 发送 POST 请求到后端查询所有活动接口
  // 【类比后端】相当于 RestTemplate.postForObject(url, request, List.class)
  return request.post('/v1/gbm/index/query_all_activities', params)
}
