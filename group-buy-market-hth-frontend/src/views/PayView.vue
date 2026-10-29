<!--
  PayView.vue - 支付确认页面
  ===========================
  【页面职责】
    锁单成功后跳转到此页面，展示订单信息、模拟二维码支付，并支持：
    1. 确认支付完成 → 调用结算接口
    2. 取消订单（退单）→ 调用退单接口

  【类比后端】
    相当于后端的 pay.html 支付页面 + 结算/退单 Controller：
    - 展示订单信息 → 类比 @GetMapping("/pay/{orderId}") 返回支付视图
    - 确认支付 → 类比 @PostMapping("/trade/settlement") 提交结算请求
    - 取消订单 → 类比 @PostMapping("/trade/refund") 提交退单请求

  【数据流转】
    DetailView ──(router.push + query参数)──→ PayView ──(settlement/refund API)──→ 后端
                                                  │
                                                  ↓ (支付成功)
                                             首页 / 订单页
-->

<template>
  <!-- 页面最外层容器 -->
  <div class="pay-page">
    <!-- ==================== 顶部标题栏 ==================== -->
    <header class="page-header">
      <h2 class="page-title">💳 支付确认</h2>
    </header>

    <!-- ==================== 订单信息卡片 ==================== -->
    <el-card shadow="hover" class="section-card order-info-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">📋 订单信息</span>
          <!--
            【Element Plus 组件】el-tag 状态标签
            - type 控制颜色：primary=蓝, success=绿, warning=橙, danger=红
            - effect="dark"：实心填充效果
          -->
          <el-tag type="warning" effect="dark" size="small">待支付</el-tag>
        </div>
      </template>

      <!--
        【Element Plus 组件】el-descriptions 描述列表
        - 用于展示键值对信息
        - :column="1" 每行 1 列
        - border 显示边框
      -->
      <el-descriptions :column="1" border>
        <el-descriptions-item label="订单号">
          <!--
            【前端概念】{{ }} 插值表达式
            - 显示 orderInfo.orderId 的值
            - 如果值为空，显示 '--'（使用 || 运算符提供默认值）
          -->
          {{ orderInfo.orderId || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="组队ID">
          {{ orderInfo.teamId || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="外部交易号">
          {{ orderInfo.outTradeNo || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="商品名称">
          {{ orderInfo.goodsName || '拼团商品' }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 金额大字展示区域 -->
      <div class="amount-display">
        <div class="amount-label">支付金额</div>
        <!--
          【前端概念】金额格式化
          - .toFixed(2) 保留两位小数
          - 类比 Java 的 String.format("%.2f", amount)
        -->
        <div class="amount-value">
          ¥{{ Number(orderInfo.payPrice || 0).toFixed(2) }}
        </div>
      </div>
    </el-card>

    <!-- ==================== 二维码模拟区域 ==================== -->
    <!--
      【前端概念】静态展示区域
      这里模拟一个支付二维码（实际项目中会调用微信/支付宝 SDK 生成）
    -->
    <el-card shadow="hover" class="section-card qrcode-card">
      <div class="qrcode-container">
        <!--
          【前端概念】SVG 图标 / 图片展示
          这里用一个占位图模拟二维码
          实际项目中可能是：<img :src="qrcodeUrl" /> 或 <qrcode-vue /> 组件
        -->
        <div class="qrcode-placeholder">
          <!--
            使用 Element Plus 的图标组件
            【类比后端】相当于在页面中嵌入一个图片资源
          -->
          <el-icon :size="80" color="#409eff">
            <Iphone />
          </el-icon>
          <p class="qrcode-text">扫码支付</p>
          <p class="qrcode-hint">（模拟二维码，实际项目中替换为真实二维码）</p>
        </div>
      </div>
    </el-card>

    <!-- ==================== 操作按钮区域 ==================== -->
    <div class="action-buttons">
      <!--
        【Element Plus 组件】el-button 按钮
        - type="danger"：红色按钮（危险操作）
        - type="primary"：蓝色按钮（主要操作）
        - size="large"：大尺寸
        - :loading="isSettling"：加载状态（请求中显示转圈动画）
        - :disabled="isSettling || isRefunding"：禁用状态（防止重复点击）
        - @click="handleSettlement"：点击事件绑定
      -->
      <el-button
        type="danger"
        size="large"
        class="action-btn cancel-btn"
        :loading="isRefunding"
        :disabled="isSettling || isRefunding"
        @click="handleRefund"
      >
        {{ isRefunding ? '退单中...' : '取消订单' }}
      </el-button>

      <el-button
        type="primary"
        size="large"
        class="action-btn confirm-btn"
        :loading="isSettling"
        :disabled="isSettling || isRefunding"
        @click="handleSettlement"
      >
        {{ isSettling ? '处理中...' : '确认支付完成' }}
      </el-button>
    </div>

    <!-- ==================== 底部提示 ==================== -->
    <div class="footer-tip">
      <el-icon color="#909399"><InfoFilled /></el-icon>
      <span>支付完成后请点击"确认支付完成"按钮</span>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * PayView 的业务逻辑
 * ====================
 *
 * 【类比后端】
 * 这个 <script setup> 块相当于一个 Controller 类：
 * - 接收参数（路由 query → 类比 @RequestParam）
 * - 定义状态变量（ref → 类比成员变量）
 * - 定义方法（handleSettlement / handleRefund → 类比 @PostMapping 方法）
 *
 * 【核心功能】
 * 1. 从路由参数获取订单信息并展示
 * 2. 确认支付 → 调用结算接口 → 成功提示 → 返回首页
 * 3. 取消订单 → 弹确认框 → 调用退单接口 → 成功提示 → 返回首页
 * 4. 将订单信息保存到 localStorage（供订单页使用）
 */

// ==================== 导入依赖 ====================

/**
 * Vue 3 核心 API
 * - ref：创建响应式变量
 * - onMounted：组件挂载完成后的钩子函数
 */
import { ref, onMounted } from 'vue'

/**
 * vue-router 路由 API
 * - useRoute：获取当前路由信息（读取 query 参数）
 * - useRouter：编程式导航（跳转到其他页面）
 */
import { useRoute, useRouter } from 'vue-router'

/**
 * Element Plus 组件
 * - ElMessage：轻量级消息提示（顶部弹出，几秒后自动消失）
 *   类比后端：logger.info("操作成功") 的日志输出，但展示给用户看
 * - ElMessageBox：模态对话框（需要用户点击确认/取消）
 *   类比后端：confirm("确定要删除吗？") 的二次确认弹窗
 * - ElNotification：通知弹窗（比 Message 更醒目，可自定义内容）
 */
import { ElMessage, ElMessageBox } from 'element-plus'

/**
 * Element Plus 图标
 * - Iphone：手机图标（模拟扫码）
 * - InfoFilled：信息图标（底部提示）
 */
import { Iphone, InfoFilled } from '@element-plus/icons-vue'

/**
 * 导入交易 API 函数
 * - settlementMarketPayOrder：结算接口（确认支付）
 * - refundMarketPayOrder：退单接口（取消订单）
 *
 * 【类比后端】
 * 相当于 @Autowired IMarketTradeService tradeService;
 * 然后通过 tradeService.settlementMarketPayOrder(request) 调用
 */
import {
  settlementMarketPayOrder,
  refundMarketPayOrder,
} from '../api/trade'

/**
 * 导入类型定义
 * - type 关键字表示只导入类型，不会打包到最终代码中
 * - 类比后端：import DTO 类，只用于编译时类型检查
 */
import type {
  SettlementMarketPayOrderRequestDTO,
  RefundMarketPayOrderRequestDTO,
} from '../api/types'

/**
 * 导入用户 Store
 * 【类比后端】相当于从 SecurityContext 获取当前登录用户
 */
import { useUserStore } from '../stores/user'

// ==================== 路由 & Store ====================

/**
 * useRoute() 获取当前路由
 * 【类比后端】相当于 HttpServletRequest
 * - route.query → request.getParameterMap()（URL ? 后面的参数）
 * - route.params → @PathVariable（URL 路径参数）
 */
const route = useRoute()

/**
 * useRouter() 路由导航
 * 【类比后端】相当于 HttpServletResponse.sendRedirect()
 */
const router = useRouter()

/**
 * 用户 Store
 * 【类比后端】相当于 HttpSession 中存储的当前用户信息
 */
const userStore = useUserStore()

// ==================== 响应式状态 ====================

/**
 * 订单信息（从路由 query 参数解析）
 *
 * 【类比后端】相当于从 request 中解析参数，封装成 DTO 对象
 * 使用 ref 包装，使得数据变化时页面自动更新
 */
const orderInfo = ref({
  orderId: '',
  teamId: '',
  payPrice: 0,
  originalPrice: 0,
  deductionPrice: 0,
  outTradeNo: '',
  goodsId: '',
  goodsName: '',
})

/**
 * 结算加载状态
 * 【类比后端】相当于 boolean isSettling = false;
 * true 表示正在调用结算接口，按钮显示 loading 并禁用
 */
const isSettling = ref(false)

/**
 * 退单加载状态
 * 【类比后端】相当于 boolean isRefunding = false;
 */
const isRefunding = ref(false)

// ==================== 生命周期 ====================

/**
 * onMounted() 组件挂载完成后执行
 * 【类比后端】相当于 @PostConstruct 初始化方法
 *
 * 在这里：
 * 1. 从路由 query 参数解析订单信息
 * 2. 将订单信息保存到 localStorage（供订单页展示）
 */
onMounted(() => {
  // 从路由 query 参数获取订单信息
  // 【类比后端】相当于 request.getParameter("orderId")
  const query = route.query

  // 解析订单信息（提供默认值防止 undefined）
  // 【前端概念】Number() 类型转换
  // - Number(query.payPrice) 将字符串转为数字
  // - 类比 Java 的 Double.parseDouble(str)
  // - 如果 str 为空或无效，Number() 返回 NaN，所以用 || 0 提供默认值
  orderInfo.value = {
    orderId: (query.orderId as string) || '',
    teamId: (query.teamId as string) || '',
    payPrice: Number(query.payPrice) || 0,
    originalPrice: Number(query.originalPrice) || 0,
    deductionPrice: Number(query.deductionPrice) || 0,
    outTradeNo: (query.outTradeNo as string) || '',
    goodsId: (query.goodsId as string) || '',
    goodsName: (query.goodsName as string) || '拼团商品',
  }

  // 保存订单信息到 localStorage
  // 【类比后端】相当于把订单信息写入数据库持久化
  // 但这里是写入浏览器本地存储（localStorage），刷新页面后数据不丢失
  // 用途：订单页（OrdersView）可以从 localStorage 读取订单列表
  saveOrderToLocalStorage()
})

// ==================== 方法 ====================

/**
 * 保存订单信息到 localStorage
 *
 * 【前端概念】localStorage 本地存储
 * - localStorage 是浏览器提供的持久化存储 API
 * - 数据以键值对形式存储，永久保存（除非手动清除）
 * - 容量约 5MB（类比后端：Cookie 4KB，Session 取决于服务器配置）
 *
 * 【类比后端】
 * - localStorage.setItem(key, value) → 相当于 database.insert(entity)
 * - localStorage.getItem(key) → 相当于 database.selectByKey(key)
 * - JSON.stringify(obj) → 相当于把 Java 对象序列化为 JSON 字符串
 * - JSON.parse(str) → 相当于把 JSON 字符串反序列化为 Java 对象
 *
 * 【为什么要保存】
 * 订单页（OrdersView）需要展示用户的订单列表
 * 这些数据来自多个页面（每次锁单成功后都会保存）
 * 所以用 localStorage 做持久化存储
 */
function saveOrderToLocalStorage() {
  try {
    // 1. 从 localStorage 读取已有的订单列表
    // 【类比后端】SELECT * FROM orders WHERE userId = ?
    const existingOrders = JSON.parse(localStorage.getItem('orders') || '[]')

    // 2. 检查是否已存在相同订单（避免重复保存）
    // 【前端概念】Array.prototype.some() 数组方法
    // - 检查数组中是否有元素满足条件（返回 true/false）
    // - 类比 Java 的 list.stream().anyMatch(o -> o.getOutTradeNo().equals(tradeNo))
    const exists = existingOrders.some(
      (order: any) => order.outTradeNo === orderInfo.value.outTradeNo
    )

    // 3. 如果不存在，则添加新订单并保存
    if (!exists) {
      existingOrders.push({
        ...orderInfo.value,  // 展开运算符，把对象所有属性复制（类比 BeanUtils.copyProperties）
        userId: userStore.username,
        createTime: new Date().toISOString(),  // ISO 格式时间字符串
        status: '待支付',
      })

      // 4. 写回 localStorage
      // JSON.stringify：把 JS 对象转为 JSON 字符串（类比 Jackson 序列化）
      localStorage.setItem('orders', JSON.stringify(existingOrders))
    }
  } catch (error) {
    // localStorage 操作失败不影响主流程，只打印警告
    // 【类比后端】catch (Exception e) { log.warn("保存订单失败", e); }
    console.warn('保存订单到 localStorage 失败:', error)
  }
}

/**
 * 处理结算（确认支付完成）
 *
 * 【类比后端】
 * 相当于 @PostMapping("/trade/settlement") 方法：
 *   1. 构建请求 DTO
 *   2. 调用 Service 层
 *   3. 处理响应（成功/失败）
 *
 * 【前端概念】async/await 异步编程
 * - async 标记函数为异步函数
 * - await 等待 Promise 完成并获取结果
 * - 类比 Java 的 CompletableFuture：
 *     // Java
 *     CompletableFuture<Response> future = tradeService.settlement(request);
 *     Response result = future.get();  // 阻塞等待
 *
 *     // 前端
 *     const result = await settlementMarketPayOrder(request);  // 非阻塞等待
 *
 * - 区别：Java 的 future.get() 阻塞线程，前端 await 不阻塞 UI
 */
async function handleSettlement() {
  // 1. 防重复点击
  if (isSettling.value) {
    ElMessage.warning('正在处理中，请勿重复操作')
    return
  }

  // 2. 设置加载状态
  isSettling.value = true

  try {
    // 3. 构建结算请求 DTO
    // 【类比后端】new SettlementMarketPayOrderRequestDTO()
    const requestParams: SettlementMarketPayOrderRequestDTO = {
      userId: userStore.username,
      outTradeNo: orderInfo.value.outTradeNo,
      source: 's01',
      channel: 'c01',
      outTradeTime: formatDateTime(new Date()),  // 当前时间格式化
    }

    // 4. 调用结算 API
    // 【类比后端】tradeService.settlementMarketPayOrder(request)
    await settlementMarketPayOrder(requestParams)

    // 5. 更新 localStorage 中的订单状态
    updateOrderStatusInLocalStorage('已支付')

    // 6. 显示成功对话框
    // 【前端概念】ElMessageBox 模态对话框
    // - 类比后端的 confirm 弹窗，但更美观
    // - ElMessageBox.alert：提示框（只有"确定"按钮）
    // - ElMessageBox.confirm：确认框（有"确定"和"取消"按钮）
    // - ElMessageBox({ title, message })：自定义对话框
    await ElMessageBox({
      title: '🎉 支付成功',
      message: `订单号：${orderInfo.value.orderId}<br/>支付金额：¥${Number(orderInfo.value.payPrice).toFixed(2)}<br/><br/>交易已完成，感谢您的购买！`,
      type: 'success',
      confirmButtonText: '返回首页',
      showClose: false,  // 不显示关闭按钮，强制用户点击"确定"
      dangerouslyUseHTMLString: true,  // 允许 message 中使用 HTML
    })

    // 7. 跳转到首页
    // 【类比后端】response.sendRedirect("/")
    router.push({ name: 'home' })

  } catch (error: any) {
    // 8. 错误处理
    // 如果用户点击了对话框的"取消"按钮，也会进入 catch，这里忽略
    if (error === 'cancel' || error?.toString().includes('cancel')) {
      return
    }

    // 其他错误显示提示
    const errorMsg = error?.response?.data?.info || error?.message || '结算失败，请稍后重试'
    ElMessage.error(errorMsg)
  } finally {
    // 9. 重置加载状态
    // 【类比后端】finally { lock.unlock(); } 确保资源释放
    isSettling.value = false
  }
}

/**
 * 处理退单（取消订单）
 *
 * 【类比后端】
 * 相当于 @PostMapping("/trade/refund") 方法：
 *   1. 弹出确认对话框（二次确认，防止误操作）
 *   2. 用户确认后调用退单 API
 *   3. 处理响应（成功提示 + 跳转）
 *
 * 【前端概念】ElMessageBox.confirm 确认对话框
 * - 返回 Promise：用户点"确定"时 resolve，点"取消"时 reject
 * - 类比后端的：
 *     if (confirm("确定要退单吗？")) {
 *         tradeService.refund(request);
 *     }
 *   但前端的实现是异步的（Promise），不会阻塞 UI
 */
async function handleRefund() {
  try {
    // 1. 弹出确认对话框
    // 【类比后端】相当于后端二次确认："确定要退单吗？退单后不可恢复"
    // ElMessageBox.confirm 返回 Promise
    // - 用户点"确定"：Promise resolve，继续执行后续代码
    // - 用户点"取消"：Promise reject，跳到 catch 块
    await ElMessageBox.confirm(
      '退单后订单将取消，已支付金额将原路退回。确定要退单吗？',
      '⚠️ 确认退单',
      {
        confirmButtonText: '确定退单',
        cancelButtonText: '再想想',
        type: 'warning',
        // 自定义样式类名（用于 CSS 定制对话框外观）
        customClass: 'refund-confirm-dialog',
      }
    )

    // 2. 用户点击"确定退单"，继续执行
    // 设置加载状态
    isRefunding.value = true

    // 3. 构建退单请求 DTO
    // 【类比后端】new RefundMarketPayOrderRequestDTO()
    const requestParams: RefundMarketPayOrderRequestDTO = {
      userId: userStore.username,
      outTradeNo: orderInfo.value.outTradeNo,
      source: 's01',
      channel: 'c01',
    }

    // 4. 调用退单 API
    // 【类比后端】tradeService.refundMarketPayOrder(request)
    await refundMarketPayOrder(requestParams)

    // 5. 更新 localStorage 中的订单状态
    updateOrderStatusInLocalStorage('已退单')

    // 6. 显示成功提示
    ElMessage.success('退单成功！订单已取消')

    // 7. 跳转回首页
    // 使用 router.replace 而不是 router.push
    // 【前端概念】replace vs push
    // - push：添加历史记录，用户可以点"后退"回到支付页
    // - replace：替换当前历史记录，用户点"后退"不会回到支付页
    // - 类比后端：sendRedirect (push) vs forward (replace)
    router.replace({ name: 'home' })

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
    // 9. 重置加载状态
    isRefunding.value = false
  }
}

/**
 * 格式化日期时间
 *
 * 【前端概念】日期格式化
 * - 类比 Java 的 SimpleDateFormat / DateTimeFormatter
 * - Java:  DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now())
 * - 前端：手动拼接字符串（或使用 dayjs / date-fns 库）
 *
 * 【为什么手动拼接】
 * - 原生 JS 的 toLocaleString() 格式不固定（不同浏览器可能不同）
 * - 手动拼接可以保证格式一致：yyyy-MM-dd HH:mm:ss
 *
 * @param date 日期对象
 * @returns 格式化后的字符串，如 "2024-01-01 12:30:45"
 */
function formatDateTime(date: Date): string {
  // 【前端概念】Date 对象方法
  // - getFullYear()：年份，如 2024（类比 Java 的 date.getYear() + 1900）
  // - getMonth()：月份，0-11（0 表示 1 月！所以要 +1）
  // - getDate()：日期，1-31
  // - getHours()：小时，0-23
  // - getMinutes()：分钟，0-59
  // - getSeconds()：秒，0-59

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')  // 月份 +1，补零到 2 位
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')

  // 【前端概念】模板字符串 `${var}`
  // - 用反引号 ` 包裹，${} 中插入变量
  // - 类比 Java 的 String.format("%s-%s-%s", year, month, day)
  // - 或 Python 的 f"{year}-{month}-{day}"
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

/**
 * 更新 localStorage 中的订单状态
 *
 * 【类比后端】
 * 相当于 UPDATE orders SET status = ? WHERE outTradeNo = ?
 *
 * @param newStatus 新状态，如 "已支付" / "已退单"
 */
function updateOrderStatusInLocalStorage(newStatus: string) {
  try {
    const orders = JSON.parse(localStorage.getItem('orders') || '[]')
    // 【前端概念】Array.prototype.find() 查找数组元素
    // - 返回第一个满足条件的元素，没有则返回 undefined
    // - 类比 Java 的 list.stream().filter(o -> ...).findFirst().orElse(null)
    const order = orders.find((o: any) => o.outTradeNo === orderInfo.value.outTradeNo)
    if (order) {
      order.status = newStatus
      order.updateTime = new Date().toISOString()
      localStorage.setItem('orders', JSON.stringify(orders))
    }
  } catch (error) {
    console.warn('更新订单状态失败:', error)
  }
}
</script>

<style scoped>
/**
 * 【前端概念】<style scoped>
 * - scoped 表示样式只作用于当前组件
 * - 类比后端的命名空间隔离：com.hth.moduleA 不影响 com.hth.moduleB
 */

/* ==================== 页面整体布局 ==================== */
.pay-page {
  max-width: 600px;          /* 最大宽度 */
  margin: 0 auto;            /* 水平居中 */
  padding: 20px 16px 40px;
  min-height: 100vh;
  background: linear-gradient(180deg, #f0f4ff 0%, #fafbff 100%);
}

/* ==================== 顶部标题栏 ==================== */
.page-header {
  text-align: center;
  margin-bottom: 24px;
  padding: 12px 0;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a202c;
  margin: 0;
}

/* ==================== 卡片通用样式 ==================== */
.section-card {
  margin-bottom: 16px;
  border-radius: 12px;
  overflow: hidden;
}

/* Element Plus 卡片头部自定义样式 */
/*
  【前端概念】:deep() 深度选择器
  - scoped 样式默认不影响子组件内部元素
  - :deep(.el-xxx) 可以穿透 scoped 限制，修改 Element Plus 组件内部样式
  - 类比后端：继承 + Override，覆盖第三方组件的默认样式
*/
.section-card :deep(.el-card__header) {
  padding: 14px 20px;
  background: linear-gradient(135deg, #f8faff 0%, #f0f5ff 100%);
  border-bottom: 1px solid #e8ecf4;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #2d3748;
}

/* ==================== 金额展示区域 ==================== */
.amount-display {
  text-align: center;
  padding: 28px 0 16px;
  background: linear-gradient(135deg, #fff5f5 0%, #fff 100%);
  margin: 16px -20px -20px;
  padding-left: 20px;
  padding-right: 20px;
}

.amount-label {
  font-size: 14px;
  color: #718096;
  margin-bottom: 8px;
}

.amount-value {
  font-size: 42px;
  font-weight: 800;
  color: #c53030;
  letter-spacing: 2px;
  /* 文字阴影效果 */
  text-shadow: 0 2px 4px rgba(197, 48, 48, 0.1);
}

/* ==================== 二维码区域 ==================== */
.qrcode-card {
  text-align: center;
}

.qrcode-container {
  padding: 20px 0;
}

.qrcode-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.qrcode-text {
  font-size: 16px;
  font-weight: 600;
  color: #2d3748;
  margin: 0;
}

.qrcode-hint {
  font-size: 12px;
  color: #a0aec0;
  margin: 0;
}

/* ==================== 操作按钮区域 ==================== */
.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  padding: 0 4px;
}

.action-btn {
  flex: 1;
  height: 50px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
  letter-spacing: 1px;
  transition: all 0.3s ease;
}

.cancel-btn {
  background: #fff5f5;
  color: #c53030;
  border: 1px solid #feb2b2;
}

.cancel-btn:hover:not(:disabled) {
  background: #fed7d7;
  border-color: #f56565;
}

.confirm-btn {
  background: linear-gradient(135deg, #48bb78 0%, #38a169 100%);
  border: none;
}

.confirm-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(72, 187, 120, 0.4);
}

.action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ==================== 底部提示 ==================== */
.footer-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 20px;
  padding: 12px;
  background: #f7fafc;
  border-radius: 8px;
  font-size: 13px;
  color: #718096;
}

/* ==================== Element Plus 组件样式覆盖 ==================== */
.pay-page :deep(.el-descriptions__label) {
  font-weight: 500;
  color: #718096;
  width: 110px;
}

.pay-page :deep(.el-descriptions__content) {
  color: #2d3748;
  font-weight: 500;
}
</style>
