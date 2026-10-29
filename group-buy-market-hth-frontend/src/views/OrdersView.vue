<!--
  OrdersView.vue - 我的订单页面
  ================================
  【页面职责】
    展示当前用户的所有订单记录，支持：
    1. 按状态筛选（全部 / 待支付 / 已支付 / 已退单）
    2. 查看订单详情（弹窗展示）
    3. 退单操作（仅待支付状态可退）
    4. 空状态友好提示

  【类比后端】
    相当于后端的 orders.html 订单列表页 + OrderController：
    - 页面加载时从 localStorage 读取订单列表（类比 @GetMapping("/orders") 查询数据库）
    - 状态筛选 → 类比 SQL 的 WHERE status = ?
    - 退单操作 → 类比 @PostMapping("/trade/refund") 调用退单接口
    - 详情弹窗 → 类比 AJAX 异步加载订单详情

  【数据来源】
    订单数据由 PayView.vue 在锁单成功后保存到 localStorage
    本页面通过 utils/index.ts 中的 getOrderList() 读取

  【组件依赖关系】
    OrdersView（当前页面）
      ├── utils/index.ts（工具函数：读取订单、格式化日期/价格等）
      ├── stores/user.ts（用户 Store：获取当前用户名）
      ├── api/trade.ts（退单 API：调用后端退单接口）
      └── Element Plus 组件（el-card, el-tag, el-dialog, el-empty 等）
-->

<template>
  <!--
    【前端概念】<template> 定义页面的 HTML 结构
    【类比后端】相当于 Thymeleaf/JSP 的模板文件
    注意：template 中只能有一个根元素（这里用 .orders-page 作为根）
  -->
  <div class="orders-page">

    <!-- ========================================== -->
    <!-- 一、顶部导航栏（与首页统一风格）              -->
    <!-- ========================================== -->
    <!--
      【前端概念】固定的顶部导航，使用 Flexbox 水平排列
      【类比后端】相当于 JSP 的 header.jsp 被 include 到每个页面
      包含返回按钮、页面标题
    -->
    <header class="top-nav">
      <div class="nav-left">
        <!--
          【前端概念】@click 绑定点击事件
          【类比后端】相当于 <a href="/" onclick="goBack()"> 的简化版
          点击返回按钮跳转到首页
        -->
        <el-button size="small" round @click="goHome">
          ← 返回首页
        </el-button>
      </div>
      <div class="nav-center">
        <span class="nav-title">📋 我的订单</span>
      </div>
      <div class="nav-right">
        <!--
          显示当前用户名（脱敏处理）
          【类比后端】相当于从 Session 中获取当前登录用户
        -->
        <span class="username">{{ maskedName }}</span>
      </div>
    </header>

    <!-- ========================================== -->
    <!-- 二、订单状态筛选标签栏                       -->
    <!-- ========================================== -->
    <!--
      【Element Plus 组件】el-radio-group 单选按钮组
      - v-model 双向绑定当前选中的值
      - el-radio-button 每个选项
      - 【类比后端】相当于下拉框筛选：SELECT * FROM orders WHERE status = #{status}
        前端用 filter() 方法实现相同的过滤效果

      【前端概念】v-model 双向数据绑定
      - 用户点击按钮时，activeFilter 变量自动更新
      - activeFilter 变量更新时，按钮选中状态自动更新
      - 【类比后端】相当于 Spring MVC 的 @ModelAttribute 自动绑定表单参数
    -->
    <div class="filter-bar">
      <el-radio-group v-model="activeFilter" size="default">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="待支付">待支付</el-radio-button>
        <el-radio-button value="已支付">已支付</el-radio-button>
        <el-radio-button value="已退单">已退单</el-radio-button>
      </el-radio-group>
    </div>

    <!-- ========================================== -->
    <!-- 三、订单列表区域                             -->
    <!-- ========================================== -->
    <!--
      【前端概念】v-if / v-else 条件渲染
      - 如果过滤后的订单列表不为空，显示订单卡片列表
      - 如果为空，显示空状态提示
      - 【类比后端】相当于 JSTL 的 <c:choose><c:when test="${!empty list}">...
    -->

    <!-- 3.1 订单列表（有数据时显示） -->
    <div v-if="filteredOrders.length > 0" class="order-list">
      <!--
        【前端概念】v-for 列表渲染
        - 遍历 filteredOrders 数组，为每个订单生成一个卡片
        - :key="order.outTradeNo" 用交易单号作为唯一标识
        - 【类比后端】相当于 JSTL 的 <c:forEach items="${orders}" var="order">
          或 Thymeleaf 的 th:each="order : ${orders}"
      -->
      <el-card
        v-for="order in filteredOrders"
        :key="order.outTradeNo"
        shadow="hover"
        class="order-card"
      >
        <!--
          卡片头部：交易单号 + 状态标签
          【Element Plus 组件】el-tag 状态标签
          - :type 根据订单状态动态切换颜色
          - 【类比后端】相当于 th:classappend="${order.status == '待支付'} ? 'tag-warning' : ..."
            但前端用三元表达式更简洁
        -->
        <template #header>
          <div class="card-header">
            <span class="trade-no">单号：{{ order.outTradeNo }}</span>
            <!--
              【前端概念】动态绑定 :type 属性
              - 根据 order.status 的值返回不同的 type
              - 待支付 → warning（橙色）
              - 已支付 → success（绿色）
              - 已退单 → info（灰色）
              - 【类比后端】相当于 Java 的 switch-case 或三元运算符
            -->
            <el-tag
              :type="getStatusTagType(order.status)"
              effect="dark"
              size="small"
              round
            >
              {{ order.status }}
            </el-tag>
          </div>
        </template>

        <!--
          卡片内容：订单详细信息
          【Element Plus 组件】el-descriptions 描述列表
          - 用于展示键值对信息
          - :column="1" 每行 1 列
          - border 显示边框
        -->
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="商品名称">
            {{ order.goodsName }}
          </el-descriptions-item>
          <el-descriptions-item label="支付金额">
            <!--
              【前端概念】金额格式化
              - 使用 formatPrice() 工具函数
              - 类比 Java 的 String.format("¥%.2f", amount)
            -->
            <span class="pay-price">{{ formatPrice(order.payPrice) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            <!--
              直接显示 ISO 格式的时间字符串
              PayView 保存时用的是 new Date().toISOString()
            -->
            {{ formatDisplayTime(order.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="组队ID">
            {{ order.teamId || '--' }}
          </el-descriptions-item>
        </el-descriptions>

        <!--
          卡片底部操作按钮
          【前端概念】v-if 条件渲染按钮
          - "查看详情" 按钮：所有状态都显示
          - "退单" 按钮：仅待支付状态显示
          - 【类比后端】相当于
            <c:if test="${order.status == '待支付'}">
              <button>退单</button>
            </c:if>
        -->
        <div class="card-actions">
          <el-button size="small" @click="showDetail(order)">
            查看详情
          </el-button>
          <!--
            退单按钮（仅待支付状态显示）
            【前端概念】v-if 条件渲染
            - 只有 order.status === '待支付' 时才渲染这个按钮
            - 【类比后端】相当于 @PreAuthorize("order.status == 'PENDING'")
              但前端用 v-if 更直观
          -->
          <el-button
            v-if="order.status === '待支付'"
            type="danger"
            size="small"
            :loading="refundingTradeNo === order.outTradeNo"
            @click="handleRefund(order)"
          >
            退单
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 3.2 空状态（无数据时显示） -->
    <!--
      【Element Plus 组件】el-empty 空状态
      - 当没有订单数据时显示友好的空状态提示
      - 【类比后端】相当于 if (list.isEmpty()) { 显示空状态页面 }
      - description 属性设置提示文字
    -->
    <div v-else class="empty-state">
      <el-empty :description="activeFilter === 'all' ? '暂无订单记录' : '暂无' + activeFilter + '的订单'">
        <!--
          【前端概念】el-empty 的默认插槽（slot）
          - 可以在空状态组件中插入自定义内容
          - 这里放一个"去逛逛"按钮，引导用户返回首页
        -->
        <el-button type="primary" @click="goHome">去逛逛</el-button>
      </el-empty>
    </div>

    <!-- ========================================== -->
    <!-- 四、订单详情弹窗                             -->
    <!-- ========================================== -->
    <!--
      【Element Plus 组件】el-dialog 弹窗
      - v-model 控制弹窗的显示/隐藏
      - title 弹窗标题
      - width 弹窗宽度
      - 【类比后端】相当于 Bootstrap Modal 或 Layer.js 的弹窗组件
        但 Element Plus 的弹窗是声明式的（通过 v-model 控制）
    -->
    <el-dialog
      v-model="dialogVisible"
      title="📋 订单详情"
      width="90%"
      :close-on-click-modal="true"
    >
      <!--
        【前端概念】v-if 条件渲染弹窗内容
        - 只有 selectedOrder 不为 null 时才渲染
        - 防止弹窗打开时数据还没准备好
      -->
      <div v-if="selectedOrder" class="dialog-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="订单号">
            {{ selectedOrder.orderId || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="交易单号">
            {{ selectedOrder.outTradeNo }}
          </el-descriptions-item>
          <el-descriptions-item label="组队ID">
            {{ selectedOrder.teamId || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="商品名称">
            {{ selectedOrder.goodsName }}
          </el-descriptions-item>
          <el-descriptions-item label="支付金额">
            <span class="pay-price">{{ formatPrice(selectedOrder.payPrice) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag
              :type="getStatusTagType(selectedOrder.status)"
              effect="dark"
              size="small"
              round
            >
              {{ selectedOrder.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDisplayTime(selectedOrder.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedOrder.updateTime" label="更新时间">
            {{ formatDisplayTime(selectedOrder.updateTime) }}
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedOrder.userId" label="用户ID">
            {{ selectedOrder.userId }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!--
        【前端概念】el-dialog 的 footer 插槽
        - 自定义弹窗底部按钮
        - 类比后端弹窗的 "确定" 按钮区域
      -->
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * OrdersView 的业务逻辑
 * ======================
 *
 * 【类比后端】
 * 这个 <script setup> 块相当于一个 Controller 类：
 * - 页面加载时查询订单列表（类比 @GetMapping 方法中调用 Service 查询数据）
 * - 处理筛选、退单、查看详情等交互事件（类比 @PostMapping 方法）
 *
 * 【核心功能】
 * 1. 从 localStorage 读取订单列表（使用 utils 中的 getOrderList）
 * 2. 按状态筛选订单（使用 computed 计算属性 + Array.filter）
 * 3. 退单操作（调用后端 refundMarketPayOrder API + 更新本地状态）
 * 4. 查看订单详情（弹窗展示完整信息）
 */

// ==================== 导入依赖 ====================

/**
 * Vue 3 核心 API
 * - ref：创建响应式变量（类比 AtomicReference）
 * - computed：创建计算属性（类比 getter 方法，有缓存）
 * - onMounted：组件挂载完成后的钩子（类比 @PostConstruct）
 */
import { ref, computed, onMounted } from 'vue'

/**
 * vue-router 路由 API
 * - useRouter：编程式导航（跳转到其他页面）
 *   【类比后端】HttpServletResponse.sendRedirect()
 */
import { useRouter } from 'vue-router'

/**
 * Element Plus 组件
 * - ElMessage：轻量级消息提示（顶部弹出，几秒后自动消失）
 *   【类比后端】logger.info("操作成功") 的日志输出
 * - ElMessageBox：模态对话框（需要用户点击确认/取消）
 *   【类比后端】confirm("确定要退单吗？") 的二次确认
 */
import { ElMessage, ElMessageBox } from 'element-plus'

/**
 * 导入用户 Store
 * 【类比后端】相当于从 SecurityContext / HttpSession 获取当前登录用户
 */
import { useUserStore } from '../stores/user'

/**
 * 导入退单 API 函数
 * 【类比后端】相当于 @Autowired IMarketTradeService tradeService
 */
import { refundMarketPayOrder } from '../api/trade'

/**
 * 导入类型定义（只用于类型检查，编译后擦除）
 * 【类比后端】相当于 import DTO 类
 */
import type { RefundMarketPayOrderRequestDTO } from '../api/types'

/**
 * 导入工具函数
 * 【类比后端】相当于 import static com.hth.common.utils.*
 * 引入后可以直接使用函数名调用，不需要加类名前缀
 */
import {
  type OrderRecord,       // 订单记录接口（type 表示只导入类型）
  getOrderList,           // 获取订单列表
  formatPrice,            // 格式化价格
  updateOrderStatus,      // 更新订单状态
  maskUsername,           // 用户名脱敏
} from '../utils/index'

// ==================== 路由 & Store ====================

/**
 * 路由实例
 * 【类比后端】相当于 HttpServletResponse 用于重定向
 */
const router = useRouter()

/**
 * 用户 Store 实例
 * 【类比后端】相当于 HttpSession 中存储的当前用户信息
 */
const userStore = useUserStore()

// ==================== 响应式状态 ====================

/**
 * 订单列表数据
 * 【类比后端】相当于 private List<OrderRecord> orderList
 * 存储从 localStorage 读取的所有订单
 */
const orderList = ref<OrderRecord[]>([])

/**
 * 当前筛选条件
 * 【类比后端】相当于筛选参数 String statusFilter = "all"
 * - 'all'：显示所有订单
 * - '待支付'/'已支付'/'已退单'：只显示对应状态的订单
 */
const activeFilter = ref<string>('all')

/**
 * 详情弹窗显示状态
 * 【类比后端】相当于 boolean isDialogVisible = false
 * true 时弹窗打开，false 时弹窗关闭
 */
const dialogVisible = ref(false)

/**
 * 当前选中的订单（用于详情弹窗展示）
 * 【类比后端】相当于 OrderRecord selectedOrder
 * null 表示没有选中任何订单
 */
const selectedOrder = ref<OrderRecord | null>(null)

/**
 * 正在退单的交易单号
 * 【类比后端】相当于 String refundingTradeNo
 * 用于标记哪个订单正在执行退单操作（显示 loading 状态，防止重复点击）
 */
const refundingTradeNo = ref<string>('')

// ==================== 计算属性 ====================

/**
 * 脱敏后的用户名
 * 【类比后端】相当于从 Session 获取用户名后做脱敏处理
 * 使用 computed 计算属性，依赖 userStore.username 变化时自动重算
 */
const maskedName = computed(() => {
  return maskUsername(userStore.username || '用户')
})

/**
 * 过滤后的订单列表
 *
 * 【前端概念】computed 计算属性 + Array.filter()
 * - computed：依赖 activeFilter 或 orderList 变化时自动重算（有缓存）
 * - filter()：返回满足条件的元素组成新数组
 *
 * 【类比后端】相当于 Java Stream 的过滤操作：
 *   orderList.stream()
 *     .filter(o -> "all".equals(activeFilter) || o.getStatus().equals(activeFilter))
 *     .collect(Collectors.toList())
 *
 * 当 activeFilter 为 'all' 时返回全部订单
 * 否则只返回状态匹配的订单
 */
const filteredOrders = computed(() => {
  if (activeFilter.value === 'all') {
    return orderList.value
  }
  return orderList.value.filter(order => order.status === activeFilter.value)
})

// ==================== 生命周期 ====================

/**
 * 组件挂载完成后执行
 * 【类比后端】相当于 @PostConstruct 初始化方法
 *
 * 在这里：从 localStorage 加载订单列表
 */
onMounted(() => {
  loadOrders()
})

// ==================== 方法 ====================

/**
 * 加载订单列表
 *
 * 【类比后端】相当于 Controller 调用 Service 查询数据：
 *   List<OrderRecord> orders = orderService.selectOrderList(userId)
 *   model.addAttribute("orders", orders)
 *
 * 这里从 localStorage 读取，并按当前登录用户过滤
 * （localStorage 是浏览器级别的，不区分用户，需要手动过滤）
 */
function loadOrders() {
  const allOrders = getOrderList()
  // 按当前登录用户名过滤，只显示自己的订单
  // 【类比后端】相当于 SQL: SELECT * FROM orders WHERE userId = #{username}
  // 或 Java Stream: list.stream().filter(o -> username.equals(o.getUserId())).collect(...)
  orderList.value = allOrders.filter(order => order.userId === userStore.username)
}

/**
 * 获取状态标签的类型（颜色）
 *
 * 【前端概念】三元表达式嵌套
 * - 相当于 Java 的 switch-case 或 if-else if-else
 * - 待支付 → 'warning'（橙色）
 * - 已支付 → 'success'（绿色）
 * - 已退单 → 'info'（灰色）
 * - 其他 → 'primary'（蓝色，兜底）
 *
 * 【类比后端】相当于 Java 的：
 *   switch (status) {
 *     case "待支付": return "warning";
 *     case "已支付": return "success";
 *     case "已退单": return "info";
 *     default: return "primary";
 *   }
 *
 * @param status 订单状态字符串
 * @returns Element Plus el-tag 的 type 属性值
 */
function getStatusTagType(status: string): 'warning' | 'success' | 'info' | 'primary' {
  if (status === '待支付') return 'warning'
  if (status === '已支付') return 'success'
  if (status === '已退单') return 'info'
  return 'primary'
}

/**
 * 格式化显示时间
 *
 * 【兼容处理】
 * PayView 保存的时间格式是 ISO 字符串（new Date().toISOString()）
 * 如 "2026-10-23T14:00:00.000Z"
 * 这里转为更友好的 "yyyy-MM-dd HH:mm:ss" 格式
 *
 * 【前端概念】new Date(isoString).toLocaleString()
 * - 将 ISO 字符串转为本地时间格式
 * - 【类比后端】相当于 Java 的：
 *   LocalDateTime.parse(isoStr).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
 *
 * @param timeStr ISO 格式或普通格式的时间字符串
 * @returns 格式化后的时间字符串
 */
function formatDisplayTime(timeStr: string): string {
  if (!timeStr) return '--'
  try {
    const date = new Date(timeStr)
    // 检查日期是否有效（Invalid Date 的 getTime() 返回 NaN）
    if (isNaN(date.getTime())) return timeStr

    // 【前端概念】手动拼接格式化字符串
    // 也可以用 toLocaleString('zh-CN') 但格式不够稳定
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const seconds = String(date.getSeconds()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
  } catch {
    return timeStr
  }
}

/**
 * 查看订单详情
 *
 * 打开弹窗并设置当前选中的订单
 * 【类比后端】相当于点击"查看"按钮后，AJAX 请求获取订单详情
 * 这里直接从已有数据中取（不需要额外请求后端）
 *
 * @param order 要查看的订单记录
 */
function showDetail(order: OrderRecord) {
  selectedOrder.value = order
  dialogVisible.value = true
}

/**
 * 处理退单操作
 *
 * 【类比后端】相当于退单 Controller 方法：
 *   1. 弹出确认对话框（二次确认，防止误操作）
 *   2. 用户确认后调用退单 API
 *   3. 更新本地订单状态
 *   4. 显示操作结果
 *
 * 【前端概念】async/await 异步编程
 * - async 标记函数为异步函数
 * - await 等待 Promise 完成（类比 CompletableFuture.get()，但不阻塞 UI）
 * - try/catch/finally 结构与 Java 完全一致
 *
 * @param order 要退单的订单记录
 */
async function handleRefund(order: OrderRecord) {
  try {
    // 1. 弹出确认对话框
    // 【类比后端】相当于后端二次确认："确定要退单吗？退单后不可恢复"
    // ElMessageBox.confirm 返回 Promise：
    // - 用户点"确定"：Promise resolve，继续执行后续代码
    // - 用户点"取消"：Promise reject，跳到 catch 块
    await ElMessageBox.confirm(
      `确定要退单吗？\n交易单号：${order.outTradeNo}\n金额：${formatPrice(order.payPrice)}`,
      '⚠️ 确认退单',
      {
        confirmButtonText: '确定退单',
        cancelButtonText: '再想想',
        type: 'warning',
      }
    )

    // 2. 用户点击"确定退单"，设置 loading 状态
    // 【类比后端】相当于加分布式锁，防止重复提交
    refundingTradeNo.value = order.outTradeNo

    // 3. 构建退单请求 DTO
    // 【类比后端】new RefundMarketPayOrderRequestDTO()
    const requestParams: RefundMarketPayOrderRequestDTO = {
      userId: userStore.username,
      outTradeNo: order.outTradeNo,
      source: 's01',
      channel: 'c01',
    }

    // 4. 调用退单 API
    // 【类比后端】tradeService.refundMarketPayOrder(request)
    await refundMarketPayOrder(requestParams)

    // 5. 更新 localStorage 中的订单状态
    // 【类比后端】UPDATE orders SET status = '已退单' WHERE outTradeNo = ?
    updateOrderStatus(order.outTradeNo, '已退单')

    // 6. 重新加载订单列表（刷新页面数据）
    loadOrders()

    // 7. 显示成功提示
    ElMessage.success('退单成功！订单已取消')

  } catch (error: any) {
    // 8. 错误处理
    // 如果用户点击"再想想"（取消），ElMessageBox.confirm 会 reject
    // 错误信息中包含 'cancel'，这里忽略
    if (error === 'cancel' || error?.toString().includes('cancel')) {
      return
    }

    // 其他错误显示提示
    const errorMsg = error?.response?.data?.info || error?.message || '退单失败，请稍后重试'
    ElMessage.error(errorMsg)
  } finally {
    // 9. 重置 loading 状态
    // 【类比后端】finally { lock.unlock(); } 确保资源释放
    refundingTradeNo.value = ''
  }
}

/**
 * 返回首页
 * 【类比后端】相当于 response.sendRedirect("/")
 */
function goHome() {
  router.push({ name: 'home' })
}
</script>

<style scoped>
/**
 * 【前端概念】<style scoped>
 * - scoped 表示样式只作用于当前组件
 * - 类比后端的命名空间隔离：com.hth.moduleA 不影响 com.hth.moduleB
 *
 * 样式设计说明：
 * - 与首页（HomeView）保持统一风格
 * - 使用相同的配色方案（蓝色主色调、灰色辅助色）
 * - 订单卡片使用 hover 阴影效果增加层次感
 * - 不同状态用不同颜色的 el-tag 区分
 */

/* ====== 全局页面容器 ====== */
.orders-page {
  min-height: 100vh;
  /* 【前端概念】vh = viewport height，100vh = 屏幕高度的 100% */
  background: #f5f7fa;
  /* 与首页相同的背景色 */
  padding-bottom: 24px;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ====== 顶部导航栏（与首页统一风格） ====== */
.top-nav {
  position: sticky;
  /* 【前端概念】sticky 定位：滚动时固定在顶部 */
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 56px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  /* 【前端概念】backdrop-filter 毛玻璃效果 */
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.nav-left {
  display: flex;
  align-items: center;
}

.nav-center {
  display: flex;
  align-items: center;
}

.nav-title {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, #409EFF, #f56c6c);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  /* 【前端概念】渐变文字效果 */
}

.nav-right {
  display: flex;
  align-items: center;
}

.username {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

/* ====== 筛选标签栏 ====== */
.filter-bar {
  max-width: 640px;
  margin: 16px auto 0;
  padding: 0 16px;
  display: flex;
  justify-content: center;
}

/*
  【前端概念】:deep() 深度选择器
  - scoped 样式默认不影响子组件内部元素
  - :deep(.el-xxx) 可以穿透 scoped 限制，修改 Element Plus 组件内部样式
  - 【类比后端】继承 + Override，覆盖第三方组件的默认样式
*/
.filter-bar :deep(.el-radio-group) {
  display: flex;
  gap: 0;
  background: white;
  border-radius: 20px;
  padding: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.filter-bar :deep(.el-radio-button__inner) {
  border: none;
  background: transparent;
  border-radius: 16px;
  padding: 6px 16px;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.filter-bar :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: #409EFF;
  color: white;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

/* ====== 订单列表区域 ====== */
.order-list {
  max-width: 640px;
  margin: 0 auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  /* 【前端概念】gap 属性设置子元素之间的间距 */
  /* 【类比后端】相当于 CSS 的 margin-bottom，但更优雅（最后一个元素不需要额外处理） */
}

/* ====== 订单卡片样式 ====== */
.order-card {
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
  /* 【前端概念】transition 过渡动画 */
  /* 鼠标悬停时阴影变化会有平滑过渡效果 */
}

.order-card:hover {
  transform: translateY(-2px);
  /* 【前端概念】transform 变换：向上平移 2px */
  /* 配合 el-card 的 shadow="hover" 产生浮起效果 */
}

/* 卡片头部样式 */
.order-card :deep(.el-card__header) {
  padding: 12px 20px;
  background: linear-gradient(135deg, #f8faff 0%, #f0f5ff 100%);
  border-bottom: 1px solid #e8ecf4;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.trade-no {
  font-size: 13px;
  color: #606266;
  font-family: 'Courier New', monospace;
  /* 【前端概念】monospace 等宽字体，适合展示单号 */
}

/* 卡片内容区域 */
.order-card :deep(.el-card__body) {
  padding: 16px 20px;
}

/* 金额样式 */
.pay-price {
  font-weight: 700;
  color: #f56c6c;
  font-size: 15px;
}

/* 卡片底部操作按钮 */
.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;
  /* 【前端概念】dashed 虚线边框 */
}

/* ====== 空状态 ====== */
.empty-state {
  max-width: 640px;
  margin: 60px auto;
  padding: 40px 16px;
  text-align: center;
}

/* ====== 弹窗内容 ====== */
.dialog-content {
  padding: 0 4px;
}

/* ====== Element Plus 组件样式覆盖 ====== */
/*
  【前端概念】:deep() 深度选择器覆盖 Element Plus 默认样式
  让 el-descriptions 的标签和内容有更合适的宽度和颜色
*/
.orders-page :deep(.el-descriptions__label) {
  font-weight: 500;
  color: #909399;
  width: 90px;
}

.orders-page :deep(.el-descriptions__content) {
  color: #303133;
  font-weight: 500;
}

/* ====== 响应式适配（小屏幕） ====== */
@media (max-width: 480px) {
  .order-list {
    padding: 12px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .filter-bar :deep(.el-radio-button__inner) {
    padding: 6px 10px;
    font-size: 12px;
  }
}
</style>
