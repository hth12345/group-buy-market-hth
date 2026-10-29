/**
 * utils/index.ts - 工具函数集合
 * =================================
 * 【前端概念】工具函数 = 与 UI 无关的纯逻辑函数，可以在任何地方 import 使用
 * 【类比后端】相当于 Java 的 Utils/Helper 工具类（如 StringUtils, DateUtils）
 *   - 都是 static 方法，不需要实例化
 *   - 纯函数：相同输入永远返回相同输出，没有副作用
 *
 * 【文件组织说明】
 * - 前端项目通常把通用逻辑抽到 utils 目录
 * - 类比后端的 com.hth.common.utils 包
 * - 任何组件都可以通过 import 引入使用（类比 Java 的 import static）
 *
 * 【为什么需要工具函数】
 * 1. 代码复用：格式化价格、日期等逻辑在多个页面都需要使用
 * 2. 单一职责：把通用逻辑从组件中抽离，组件只关注 UI 渲染
 * 3. 易于测试：纯函数可以独立进行单元测试
 */

// =============================================
// 一、订单记录接口（类型定义）
// =============================================

/**
 * 订单记录接口
 * 【类比后端】相当于订单表的实体类（PO/Entity）
 *
 * 【前端概念】interface 接口
 * - 定义对象的"形状"：有哪些字段、每个字段是什么类型
 * - 只在编译时检查，编译后不产生任何代码（零运行时开销）
 * - 【类比后端】相当于 Java 的 interface 或 record，定义数据结构
 *
 * 【与 PayView 的对应关系】
 * PayView 中保存到 localStorage 时的字段名：
 *   outTradeNo, orderId, teamId, payPrice, status, createTime(→createdAt), goodsName
 *
 * 注意：PayView 用的是 'createTime'，这里定义为 'createdAt'
 * 在 getOrderList() 中会做兼容转换
 */
export interface OrderRecord {
  outTradeNo: string      // 交易单号（幂等键，类比后端的 outTradeNo 字段）
  orderId: string         // 订单ID（系统内部订单号，类比后端的 orderId 字段）
  teamId: string          // 组队ID（关联拼团，类比外键 teamId）
  payPrice: number        // 支付金额（单位：元，类比 BigDecimal payPrice）
  status: string          // 订单状态：'待支付' | '已支付' | '已退单'（PayView 中用中文）
  createdAt: string       // 创建时间（ISO 格式字符串，类比 LocalDateTime createTime
  goodsName: string       // 商品名称（冗余字段，方便展示）
  userId?: string         // 用户ID（可选字段，? 表示可以不存在）
  updateTime?: string     // 更新时间（可选字段）
}

// =============================================
// 二、localStorage 存储键名（常量）
// =============================================

/**
 * localStorage 中存储订单列表的 key
 * 【类比后端】相当于 Redis 的 key 前缀或数据库表名
 *
 * 【前端概念】const 常量
 * - 用全大写 + 下划线命名（类比 Java 的 static final String ORDER_STORAGE_KEY = 'orders'）
 * - 统一管理 key 名，避免不同页面使用不同的 key 导致数据不一致
 *
 * 【重要】PayView.vue 和 OrdersView.vue 必须使用相同的 key 才能读写同一份数据
 */
const ORDER_STORAGE_KEY = 'orders'

// =============================================
// 三、通用工具函数
// =============================================

/**
 * 生成12位随机交易单号
 *
 * 【类比后端】相当于后端的 outTradeNo 生成逻辑
 * - 后端通常用 UUID 或雪花算法生成唯一单号
 * - 这里简化为：时间戳后8位 + 4位随机数
 *
 * 【前端概念】Date.now() 和 Math.random()
 * - Date.now()：返回当前时间戳（毫秒），类比 System.currentTimeMillis()
 * - Math.random()：返回 0~1 之间的随机数，类比 new Random().nextInt(10000)
 * - String.prototype.slice(-8)：取字符串最后8个字符，类比 str.substring(str.length() - 8)
 *
 * 【为什么用时间戳 + 随机数？】
 * - 时间戳保证递增趋势（越晚生成的单号越大）
 * - 随机数保证同一毫秒内生成不同单号
 * - 组合起来基本不会重复（类比后端的幂等键设计）
 *
 * @returns 12位交易单号字符串，如 "17296845001234" → "68450012" + "3456"
 */
export function generateOutTradeNo(): string {
  // 1. 取当前时间戳的后8位
  // 【类比】String.valueOf(System.currentTimeMillis()).substring(8)
  const timestamp = String(Date.now()).slice(-8)

  // 2. 生成4位随机数（1000~9999）
  // 【类比】1000 + new Random().nextInt(9000)
  const random = String(Math.floor(1000 + Math.random() * 9000))

  // 3. 拼接返回
  // 【前端概念】字符串拼接用 + 号，类比 Java 的 String.concat()
  return timestamp + random
}

/**
 * 格式化日期为 yyyy-MM-dd HH:mm:ss
 *
 * 【类比后端】相当于 Java 的 DateTimeFormatter：
 *   DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now())
 *
 * 【前端概念】Date 对象的方法
 * - getFullYear()：获取4位年份（类比 Java 的 Year.now()）
 * - getMonth()：获取月份，注意 0=1月, 11=12月，所以要 +1
 * - getDate()：获取日期（1~31）
 * - getHours()：获取小时（0~23）
 * - getMinutes()：获取分钟（0~59）
 * - getSeconds()：获取秒数（0~59）
 * - padStart(2, '0')：补齐到2位（类比 String.format("%02d", value)）
 *
 * @param date JavaScript Date 对象
 * @returns 格式化后的字符串，如 "2026-10-23 22:00:00"
 */
export function formatDateTime(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

/**
 * 格式化价格显示（保留2位小数，加 ¥ 前缀）
 *
 * 【类比后端】相当于 Java 的：
 *   String.format("¥%.2f", price)
 *   或使用 DecimalFormat：new DecimalFormat("¥#,##0.00").format(price)
 *
 * 【前端概念】Number.toFixed(2)
 * - 保留2位小数，四舍五入
 * - 类比 BigDecimal.setScale(2, RoundingMode.HALF_UP)
 *
 * @param price 价格数字
 * @returns 格式化后的字符串，如 "¥59.00"
 */
export function formatPrice(price: number): string {
  return `¥${price.toFixed(2)}`
}

/**
 * 用户名脱敏
 *
 * 【类比后端】相当于后端的手机号/用户名脱敏工具方法：
 *   StringUtils.mask("张三") → "张*"
 *   StringUtils.mask("王五丰") → "王*丰"
 *
 * 【脱敏规则】
 * - 1个字符：显示为 *
 * - 2个字符：首字符 + *
 * - 3个及以上：首字符 + (n-2)个* + 尾字符
 *
 * 【前端概念】String.prototype.repeat()
 * - 'x'.repeat(3) → "xxx"（重复字符串 n 次）
 * - 类比 Java 的 "x".repeat(3)（Java 11+）或 StringUtils.repeat("x", 3)
 *
 * @param name 原始用户名
 * @returns 脱敏后的用户名
 */
export function maskUsername(name: string): string {
  if (!name) return '*'
  if (name.length === 1) return '*'
  if (name.length === 2) return name[0] + '*'
  // 3个及以上：首 + 中间* + 尾
  return name[0] + '*'.repeat(name.length - 2) + name[name.length - 1]
}

// =============================================
// 四、localStorage 订单数据操作函数
// =============================================

/**
 * 从 localStorage 获取订单列表
 *
 * 【类比后端】相当于 DAO 层的查询方法：
 *   List<OrderPO> selectAllOrders()
 *
 * 【前端概念】JSON.parse() 反序列化
 * - localStorage 中存储的是 JSON 字符串
 * - 需要用 JSON.parse() 转为 JS 对象（类比 Jackson 的 ObjectMapper.readValue()）
 * - 如果 localStorage 中没有数据，返回空数组 []
 *
 * 【数据兼容处理】
 * PayView 保存时字段名是 'createTime'，而 OrderRecord 接口定义的是 'createdAt'
 * 这里做兼容转换：优先使用 createdAt，如果没有则取 createTime
 *
 * @returns 订单记录数组，按创建时间倒序排列（最新的在前）
 */
export function getOrderList(): OrderRecord[] {
  try {
    // 1. 从 localStorage 读取 JSON 字符串
    // 【类比】String json = redisTemplate.opsForValue().get("orders");
    const raw = localStorage.getItem(ORDER_STORAGE_KEY)

    // 2. 解析 JSON 字符串为 JS 对象数组
    // 【类比】List<Map> list = objectMapper.readValue(json, List.class);
    const orders = JSON.parse(raw || '[]')

    // 3. 字段兼容转换 + 排序
    // 【前端概念】Array.prototype.map() 数组映射
    // - 对数组每个元素执行转换函数，返回新数组
    // - 【类比后端】list.stream().map(o -> convert(o)).collect(Collectors.toList())
    //
    // 【前端概念】Array.prototype.sort() 数组排序
    // - (a, b) => new Date(b) - new Date(a) 表示按时间倒序
    // - 【类比后端】.sorted(Comparator.comparing(OrderPO::getCreateTime).reversed())
    return orders.map((o: any) => ({
      outTradeNo: o.outTradeNo || '',
      orderId: o.orderId || '',
      teamId: o.teamId || '',
      payPrice: Number(o.payPrice) || 0,
      // 兼容处理：PayView 保存的 status 是中文（'待支付'/'已支付'/'已退单'）
      status: o.status || '待支付',
      // 兼容处理：PayView 用的字段名是 createTime，这里映射为 createdAt
      createdAt: o.createdAt || o.createTime || '',
      goodsName: o.goodsName || '拼团商品',
      userId: o.userId || '',
      updateTime: o.updateTime || '',
    })).sort((a: OrderRecord, b: OrderRecord) => {
      // 按创建时间倒序（最新的排前面）
      // 【前端概念】new Date(str) 将字符串转为 Date 对象
      // - 如果日期字符串格式正确，可以直接比较
      // - 如果格式不对，new Date() 返回 Invalid Date，减法结果为 NaN
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    })
  } catch (error) {
    // 解析失败返回空数组（防止 localStorage 数据损坏导致页面崩溃）
    // 【类比后端】catch (Exception e) { log.error("解析订单列表失败", e); return Collections.emptyList(); }
    console.warn('获取订单列表失败:', error)
    return []
  }
}

/**
 * 保存订单记录到 localStorage
 *
 * 【类比后端】相当于 DAO 层的插入方法：
 *   void insertOrder(OrderPO order)
 *
 * 【前端概念】localStorage.setItem(key, value)
 * - 将 JSON 字符串存入 localStorage
 * - 【类比后端】redisTemplate.opsForValue().set("orders", jsonString)
 *
 * 【幂等性保证】
 * - 先检查是否已存在相同 outTradeNo 的订单
 * - 存在则跳过，不存在才添加
 * - 【类比后端】相当于数据库的 INSERT ... ON DUPLICATE KEY IGNORE
 *
 * @param order 要保存的订单记录
 */
export function saveOrderRecord(order: OrderRecord): void {
  try {
    // 1. 读取已有订单列表
    const orders = getOrderList()

    // 2. 检查是否已存在（幂等性）
    // 【前端概念】Array.prototype.some()
    // - 检查数组中是否有元素满足条件，返回 boolean
    // - 【类比后端】orders.stream().anyMatch(o -> o.getOutTradeNo().equals(order.getOutTradeNo()))
    const exists = orders.some(o => o.outTradeNo === order.outTradeNo)

    // 3. 不存在则添加
    if (!exists) {
      orders.push(order)
      // 4. 写回 localStorage
      // 【前端概念】JSON.stringify() 序列化
      // - 将 JS 对象转为 JSON 字符串
      // - 【类比后端】objectMapper.writeValueAsString(orderList)
      localStorage.setItem(ORDER_STORAGE_KEY, JSON.stringify(orders))
    }
  } catch (error) {
    console.warn('保存订单记录失败:', error)
  }
}

/**
 * 移除指定订单记录
 *
 * 【类比后端】相当于 DAO 层的删除方法：
 *   void deleteByOutTradeNo(String outTradeNo)
 *
 * 【前端概念】Array.prototype.filter()
 * - 返回满足条件的所有元素组成的新数组
 * - 【类比后端】orders.stream().filter(o -> !o.getOutTradeNo().equals(outTradeNo)).collect(Collectors.toList())
 *
 * @param outTradeNo 要删除的交易单号
 */
export function removeOrderRecord(outTradeNo: string): void {
  try {
    const orders = getOrderList()
    // 过滤掉要删除的订单（保留 outTradeNo 不匹配的）
    const filtered = orders.filter(o => o.outTradeNo !== outTradeNo)
    localStorage.setItem(ORDER_STORAGE_KEY, JSON.stringify(filtered))
  } catch (error) {
    console.warn('移除订单记录失败:', error)
  }
}

/**
 * 更新指定订单的状态
 *
 * 【类比后端】相当于 DAO 层的更新方法：
 *   void updateStatusByOutTradeNo(String outTradeNo, String newStatus)
 *
 * 【使用场景】
 * - 支付成功后将状态从 '待支付' 更新为 '已支付'
 * - 退单成功后将状态从 '待支付' 更新为 '已退单'
 *
 * @param outTradeNo 交易单号
 * @param newStatus 新的订单状态
 */
export function updateOrderStatus(outTradeNo: string, newStatus: string): void {
  try {
    const raw = localStorage.getItem(ORDER_STORAGE_KEY)
    const orders = JSON.parse(raw || '[]')

    // 【前端概念】Array.prototype.find() 查找元素
    // - 返回第一个满足条件的元素
    // - 【类比后端】orders.stream().filter(o -> ...).findFirst().orElse(null)
    const order = orders.find((o: any) => o.outTradeNo === outTradeNo)

    if (order) {
      order.status = newStatus
      order.updateTime = new Date().toISOString()
      localStorage.setItem(ORDER_STORAGE_KEY, JSON.stringify(orders))
    }
  } catch (error) {
    console.warn('更新订单状态失败:', error)
  }
}
