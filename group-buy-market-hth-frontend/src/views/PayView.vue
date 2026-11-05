<!--
  PayView.vue - 支付确认页面（对接支付宝）
  ===========================
  【页面职责】
    锁单成功后跳转到此页面，调用支付后端创建支付宝订单：
    1. 页面加载 → 调用支付后端 create_pay_order → 获取支付宝支付链接
    2. 展示支付链接 → 用户打开支付宝完成支付
    3. 支付宝回调 → 支付后端更新订单状态
    4. 用户点击"确认支付完成" → 调用拼团后端结算接口
    5. 取消订单（退单）→ 调用拼团后端退单接口

  【微服务调用链路】
    前端 ──(/pay-api)──→ 支付后端 s-pay-mall-ddd (8080) ──→ 支付宝
    前端 ──(/api)──────→ 拼团后端 group-buy-market-hth (8091) ──→ 锁单/结算/退单

  【数据流转】
    DetailView ──(router.push + query参数)──→ PayView
      ├── onMounted: 调用支付后端创建支付宝订单
      ├── handleSettlement: 调用拼团后端结算
      └── handleRefund: 调用拼团后端退单
-->

<template>
  <div class="pay-page">
    <!-- ==================== 顶部标题栏 ==================== -->
    <header class="page-header">
      <button class="back-btn" @click="goBack">&#x2190; 返回</button>
      <h2 class="page-title">&#x1F4B3; 支付确认</h2>
      <div class="header-placeholder"></div>
    </header>

    <!-- ==================== 订单信息卡片 ==================== -->
    <div class="card order-card">
      <div class="card-header">
        <span class="card-title">&#x1F4CB; 订单信息</span>
        <span class="status-tag pending">待支付</span>
      </div>

      <!-- 订单详情列表（替代 el-descriptions） -->
      <div class="order-details">
        <div class="detail-row">
          <span class="detail-label">订单号</span>
          <span class="detail-value">{{ orderInfo.orderId || '--' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">组队ID</span>
          <span class="detail-value">{{ orderInfo.teamId || '--' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">外部交易号</span>
          <span class="detail-value">{{ orderInfo.outTradeNo || '--' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">商品名称</span>
          <span class="detail-value">{{ orderInfo.goodsName || '拼团商品' }}</span>
        </div>
      </div>

      <!-- 金额大字展示区域 -->
      <div class="amount-display">
        <div class="amount-label">支付金额</div>
        <div class="amount-value">
          &#xA5;{{ Number(orderInfo.payPrice || 0).toFixed(2) }}
        </div>
      </div>
    </div>

    <!-- ==================== 支付宝支付区域 ==================== -->
    <div class="card qrcode-card">
      <!-- 正在创建支付单 -->
      <div v-if="isCreatingPay" class="qrcode-placeholder">
        <div class="qr-icon loading-spinner">&#x1F504;</div>
        <p class="qrcode-text">正在创建支付宝订单...</p>
      </div>

      <!-- 支付单创建成功，展示支付按钮 -->
      <div v-else-if="payUrl" class="qrcode-placeholder">
        <div class="qr-icon">&#x1F4B3;</div>
        <p class="qrcode-text">支付宝订单已创建</p>
        <p class="qrcode-hint">点击下方按钮打开支付宝完成支付</p>
        <button class="alipay-btn" @click="handleOpenAlipay">
          打开支付宝支付
        </button>
        <button
          class="mock-pay-btn"
          :disabled="isMockPaying"
          @click="handleMockPay"
        >
          {{ isMockPaying ? '结算中...' : '模拟支付（测试）' }}
        </button>
        <p class="qrcode-hint" style="margin-top: 10px; font-size: 11px; color: #999;">
          （如浏览器拦截弹窗，请允许弹出窗口）
        </p>
      </div>

      <!-- 支付单创建失败 -->
      <div v-else-if="payCreateFailed" class="qrcode-placeholder">
        <div class="qr-icon">&#x26A0;&#xFE0F;</div>
        <p class="qrcode-text">支付订单创建失败</p>
        <p class="qrcode-hint">请检查支付后端是否启动（端口8080）</p>
        <button class="retry-btn" @click="handleRetryCreatePay">
          重试
        </button>
      </div>

      <!-- 默认状态（兜底） -->
      <div v-else class="qrcode-placeholder">
        <div class="qr-icon">&#x1F4F1;</div>
        <p class="qrcode-text">支付宝支付</p>
        <p class="qrcode-hint">支付订单准备中...</p>
      </div>
    </div>

    <!-- ==================== 操作按钮区域 ==================== -->
    <div class="action-buttons">
      <button
        class="action-btn cancel-btn"
        :disabled="isRefunding"
        @click="handleRefund"
      >
        {{ isRefunding ? '退单中...' : (isPaySuccess ? '退单' : '取消订单') }}
      </button>

      <!-- 支付成功：显示成功状态 -->
      <button
        v-if="isPaySuccess"
        class="action-btn success-btn"
        disabled
      >
        ✓ 支付成功，即将跳转...
      </button>

      <!-- 等待支付：显示轮询状态 -->
      <button
        v-else-if="isWaitingPay"
        class="action-btn waiting-btn"
        disabled
      >
        等待支付完成...
      </button>

      <!-- 默认：打开支付宝支付 -->
      <button
        v-else
        class="action-btn confirm-btn"
        :disabled="!payUrl || isCreatingPay"
        @click="handleOpenAlipay"
      >
        打开支付宝支付
      </button>
    </div>

    <!-- ==================== 底部提示 ==================== -->
    <div class="footer-tip">
      <span class="tip-icon">&#x2139;&#xFE0F;</span>
      <span v-if="isPaySuccess">支付成功！正在跳转到订单页面...</span>
      <span v-else-if="isWaitingPay">请在支付宝完成支付，系统自动检测中...</span>
      <span v-else>点击按钮打开支付宝，支付完成后自动跳转</span>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * PayView 的业务逻辑（对接支付宝）
 * ====================
 *
 * 核心功能：
 * 1. 从路由参数获取订单信息并展示
 * 2. 页面加载时调用支付后端创建支付宝订单
 * 3. 展示支付链接 → 用户打开支付宝完成支付
 * 4. 确认支付 → 调用拼团后端结算接口 → 成功提示 → 返回首页
 * 5. 取消订单 → 弹确认框 → 调用拼团后端退单接口 → 成功提示 → 返回首页
 * 6. 将订单信息保存到 localStorage（供订单页使用）
 *
 * 【微服务分工】
 * - 支付后端(s-pay-mall-ddd): 创建支付单、对接支付宝、处理支付回调
 * - 拼团后端(group-buy-market-hth): 锁单、结算、退单
 */

// ==================== 导入依赖 ====================

import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { refundMarketPayOrder, settlementMarketPayOrder } from '../api/trade'
import type { RefundMarketPayOrderRequestDTO } from '../api/types'
// 导入支付后端 API（对接支付宝）
import { createPayOrder, openAlipayPage, queryOrderStatus, parsePayOrderId } from '../api/pay'
import type { CreatePayRequestDTO } from '../api/pay'
import { useUserStore } from '../stores/user'

// ==================== 路由 & Store ====================

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ==================== 响应式状态 ====================

/** 订单信息（从路由 query 参数解析） */
const orderInfo = ref({
  orderId: '',
  teamId: '',
  payPrice: 0,
  originalPrice: 0,
  deductionPrice: 0,
  outTradeNo: '',
  goodsId: '',
  goodsName: '',
  activityId: 0,  // 活动ID，用于创建支付单
})

/** 退单加载状态 */
const isRefunding = ref(false)

/** 支付宝支付链接（从支付后端获取，实际是 HTML 表单） */
const payUrl = ref('')

/** 支付后端的支付单号（支付宝 out_trade_no），轮询与回调以此为准 */
const payOrderId = ref('')

/** 支付单创建加载状态 */
const isCreatingPay = ref(false)

/** 支付单创建是否失败 */
const payCreateFailed = ref(false)

/** 是否正在等待支付（轮询中） */
const isWaitingPay = ref(false)

/** 是否支付成功 */
const isPaySuccess = ref(false)

/** 模拟支付处理中状态 */
const isMockPaying = ref(false)

/** 轮询定时器 */
let pollTimer: number | null = null

// ==================== 生命周期 ====================

onMounted(async () => {
  const query = route.query

  orderInfo.value = {
    orderId: (query.orderId as string) || '',
    teamId: (query.teamId as string) || '',
    payPrice: Number(query.payPrice) || 0,
    originalPrice: Number(query.originalPrice) || 0,
    deductionPrice: Number(query.deductionPrice) || 0,
    outTradeNo: (query.outTradeNo as string) || '',
    goodsId: (query.goodsId as string) || '',
    goodsName: (query.goodsName as string) || '拼团商品',
    activityId: Number(query.activityId) || 0,
  }

  // 调用支付后端创建支付宝订单（支付后端在此发起拼团锁单，其 orderId 即拼团 outTradeNo）
  await createPayOrderAsync()
})

/** 组件卸载时清理定时器 */
onUnmounted(() => {
  stopPolling()
})

// ==================== 方法 ====================

/**
 * 调用支付后端创建支付宝订单
 * 【类比后端】相当于 Feign 调用 payService.createPayOrder(request)
 *
 * 流程：
 * 1. 构建支付请求参数（userId, productId, teamId, activityId, marketType）
 * 2. 调用支付后端 /api/v1/alipay/create_pay_order
 * 3. 支付后端调用支付宝生成支付链接
 * 4. 前端展示支付链接，用户打开完成支付
 */
async function createPayOrderAsync() {
  if (isCreatingPay.value) return

  isCreatingPay.value = true
  payCreateFailed.value = false

  try {
    // 构建支付请求参数
    const payRequest: CreatePayRequestDTO = {
      userId: userStore.username,
      productId: orderInfo.value.goodsId,
      teamId: orderInfo.value.teamId,
      activityId: orderInfo.value.activityId,
      marketType: 1,  // 1=拼团
    }

    // 调用支付后端（对接支付宝）
    const resultPayUrl = await createPayOrder(payRequest)

    if (resultPayUrl) {
      payUrl.value = resultPayUrl
      // 支付后端生成的支付单号（支付宝 out_trade_no）即拼团后端的 outTradeNo，
      // 前端统一以它作为订单标识：轮询、结算、退单、localStorage 均使用此单号
      payOrderId.value = parsePayOrderId(resultPayUrl)
      orderInfo.value.orderId = payOrderId.value
      orderInfo.value.outTradeNo = payOrderId.value
      console.log('支付宝支付链接:', resultPayUrl)
      console.log('支付单号:', payOrderId.value)

      // 拿到支付单号后再落库到 localStorage（供订单页展示）
      saveOrderToLocalStorage()
    } else {
      throw new Error('支付链接为空')
    }
  } catch (error: any) {
    console.error('创建支付宝订单失败:', error)
    payCreateFailed.value = true
    ElMessage.error('创建支付订单失败，请检查支付后端是否启动（端口8080）')
  } finally {
    isCreatingPay.value = false
  }
}

/** 打开支付宝支付页面 */
function handleOpenAlipay() {
  if (payUrl.value) {
    // payUrl 实际上是支付宝返回的 HTML 表单
    openAlipayPage(payUrl.value)
    // 开始轮询检测支付状态
    startPolling()
  } else {
    ElMessage.warning('支付表单尚未获取，请稍后重试')
  }
}

/**
 * 模拟支付：直接调用拼团后端交易结算接口
 * 跳过支付宝，用当前时间作为交易时间触发结算，方便本地演示拼团已支付链路
 */
async function handleMockPay() {
  if (isMockPaying.value) return

  if (!orderInfo.value.outTradeNo) {
    ElMessage.warning('订单信息缺失，无法结算')
    return
  }

  isMockPaying.value = true
  try {
    await settlementMarketPayOrder({
      source: 's01',
      channel: 'c01',
      userId: userStore.username,
      outTradeNo: orderInfo.value.outTradeNo,
      outTradeTime: formatDateTime(new Date()),
    })

    stopPolling()
    isPaySuccess.value = true
    isWaitingPay.value = false
    updateOrderStatusInLocalStorage('已支付')
    ElMessage.success('支付成功！')

    setTimeout(() => {
      router.push({ name: 'orders' })
    }, 1500)
  } catch (error: any) {
    console.error('模拟支付失败:', error)
    const errorMsg = error?.response?.data?.info || error?.message || '结算失败，请稍后重试'
    ElMessage.error(errorMsg)
  } finally {
    isMockPaying.value = false
  }
}

/**
 * 开始轮询检测支付状态
 * 每 3 秒查询一次支付后端，检测支付宝回调是否完成
 */
function startPolling() {
  isWaitingPay.value = true

  // 清除之前的定时器
  if (pollTimer) {
    clearInterval(pollTimer)
  }

  // 每 3 秒轮询一次
  pollTimer = window.setInterval(async () => {
    try {
      // 使用支付后端的支付单号（支付宝 out_trade_no）查询，而非前端锁单生成的 outTradeNo
      const queryKey = payOrderId.value || orderInfo.value.outTradeNo
      const status = await queryOrderStatus(queryKey)
      console.log('轮询订单状态:', queryKey, status)

      // 支付成功状态：PAY_SUCCESS, MARKET, DEAL_DONE
      if (status === 'PAY_SUCCESS' || status === 'MARKET' || status === 'DEAL_DONE') {
        stopPolling()
        isPaySuccess.value = true
        isWaitingPay.value = false

        // 更新 localStorage 中的订单状态
        updateOrderStatusInLocalStorage('已支付')

        // 弹出成功提示
        ElMessage.success('支付成功！')

        // 2 秒后自动跳转到订单页面
        setTimeout(() => {
          router.push({ name: 'orders' })
        }, 2000)
      }
    } catch (error) {
      console.error('轮询订单状态失败:', error)
    }
  }, 3000)

  // 5 分钟后超时
  setTimeout(() => {
    if (isWaitingPay.value) {
      stopPolling()
      isWaitingPay.value = false
      ElMessage.warning('支付超时，请刷新页面重试')
    }
  }, 5 * 60 * 1000)
}

/** 停止轮询 */
function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

/** 重新创建支付单 */
function handleRetryCreatePay() {
  payCreateFailed.value = false
  createPayOrderAsync()
}

/** 返回上一页 */
function goBack() {
  router.back()
}

/**
 * 保存订单信息到 localStorage
 * 【类比后端】相当于 INSERT INTO orders WHERE userId = ?
 */
function saveOrderToLocalStorage() {
  try {
    const existingOrders = JSON.parse(localStorage.getItem('orders') || '[]')

    const exists = existingOrders.some(
      (order: any) => order.outTradeNo === orderInfo.value.outTradeNo
    )

    if (!exists) {
      existingOrders.push({
        ...orderInfo.value,
        userId: userStore.username,
        createTime: new Date().toISOString(),
        status: '待支付',
      })

      localStorage.setItem('orders', JSON.stringify(existingOrders))
    }
  } catch (error) {
    console.warn('保存订单到 localStorage 失败:', error)
  }
}

/**
 * 处理退单（取消订单）
 * 【类比后端】相当于 @PostMapping("/trade/refund")
 */
async function handleRefund() {
  try {
    await ElMessageBox.confirm(
      '退单后订单将取消，已支付金额将原路退回。确定要退单吗？',
      '确认退单',
      {
        confirmButtonText: '确定退单',
        cancelButtonText: '再想想',
        type: 'warning',
        customClass: 'refund-confirm-dialog',
      }
    )

    isRefunding.value = true

    const requestParams: RefundMarketPayOrderRequestDTO = {
      userId: userStore.username,
      outTradeNo: orderInfo.value.outTradeNo,
      source: 's01',
      channel: 'c01',
    }

    await refundMarketPayOrder(requestParams)

    updateOrderStatusInLocalStorage('已退单')

    ElMessage.success('退单成功！订单已取消')

    router.replace({ name: 'home' })
  } catch (error: any) {
    if (error === 'cancel' || error?.toString().includes('cancel')) {
      return
    }

    const errorMsg = error?.response?.data?.info || error?.message || '退单失败，请稍后重试'
    ElMessage.error(errorMsg)
  } finally {
    isRefunding.value = false
  }
}

/** 格式化日期时间（yyyy-MM-dd HH:mm:ss） */
function formatDateTime(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

/**
 * 更新 localStorage 中的订单状态
 * 【类比后端】相当于 UPDATE orders SET status = ? WHERE outTradeNo = ?
 */
function updateOrderStatusInLocalStorage(newStatus: string) {
  try {
    const orders = JSON.parse(localStorage.getItem('orders') || '[]')
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
 * PayView 私有样式
 * 【设计语言】与 docs/ui/html/css/index.css 保持一致
 * - 橙色主题 #ff5000
 * - #f5f5f5 背景
 * - 白色卡片 + border-radius: 8px + box-shadow
 * - Arial 字体
 */

/* ==================== 页面整体布局 ==================== */
.pay-page {
  max-width: 600px;
  margin: 0 auto;
  padding: 0 15px 40px;
  min-height: 100vh;
  background: #f5f5f5;
  font-family: Arial, sans-serif;
}

/* ==================== 顶部标题栏 ==================== */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 0;
  margin-bottom: 15px;
}

.back-btn {
  padding: 6px 14px;
  border: 1px solid #ddd;
  border-radius: 15px;
  background: white;
  color: #333;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.back-btn:hover {
  border-color: #ff5000;
  color: #ff5000;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #333;
  margin: 0;
}

.header-placeholder {
  width: 60px;
}

/* ==================== 卡片通用样式（替代 el-card） ==================== */
.card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  margin-bottom: 15px;
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 15px;
  background: #fafafa;
  border-bottom: 1px solid #eee;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

/* 状态标签（替代 el-tag） */
.status-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 2px;
  font-size: 12px;
  color: white;
}

.status-tag.pending {
  background: #e6a23c;
}

.status-tag.paid {
  background: #67c23a;
}

.status-tag.refunded {
  background: #909399;
}

/* ==================== 订单详情列表（替代 el-descriptions） ==================== */
.order-details {
  padding: 0 15px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-label {
  font-size: 14px;
  color: #999;
  flex-shrink: 0;
  width: 100px;
}

.detail-value {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  text-align: right;
  word-break: break-all;
}

/* ==================== 金额展示区域 ==================== */
.amount-display {
  text-align: center;
  padding: 24px 15px;
  background: #fff9f5;
  border-top: 1px solid #ffe5d5;
}

.amount-label {
  font-size: 14px;
  color: #999;
  margin-bottom: 8px;
}

.amount-value {
  font-size: 36px;
  font-weight: 800;
  color: #ff5000;
  letter-spacing: 2px;
}

/* ==================== 支付宝支付区域 ==================== */
.qrcode-card {
  text-align: center;
  padding: 20px 15px;
}

.qrcode-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.qr-icon {
  font-size: 60px;
}

.qrcode-text {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.qrcode-hint {
  font-size: 12px;
  color: #999;
  margin: 0;
}

/* 支付宝支付按钮 */
.alipay-btn {
  margin-top: 10px;
  padding: 12px 32px;
  background: linear-gradient(90deg, #ff5000, #ff8000);
  color: white;
  border: none;
  border-radius: 25px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.alipay-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.4);
}

/* 模拟支付按钮（测试用，低调小按钮） */
.mock-pay-btn {
  margin-top: 8px;
  padding: 6px 16px;
  background: white;
  color: #999;
  border: 1px dashed #ccc;
  border-radius: 14px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.mock-pay-btn:hover:not(:disabled) {
  color: #ff5000;
  border-color: #ff5000;
}

.mock-pay-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 重试按钮 */
.retry-btn {
  margin-top: 10px;
  padding: 10px 24px;
  background: white;
  color: #ff5000;
  border: 1px solid #ff5000;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.retry-btn:hover {
  background: #fff5f0;
}

/* 支付链接提示 */
.pay-url-hint {
  margin-top: 15px;
  font-size: 11px;
  color: #999;
  line-height: 1.6;
  max-width: 100%;
  word-break: break-all;
}

.pay-url-text {
  display: inline-block;
  margin-top: 5px;
  padding: 6px 10px;
  background: #f5f5f5;
  border-radius: 4px;
  font-family: monospace;
  font-size: 10px;
  color: #666;
  max-width: 100%;
  word-break: break-all;
}

/* 加载动画 */
.loading-spinner {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ==================== 操作按钮区域 ==================== */
.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.action-btn {
  flex: 1;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.cancel-btn {
  background: #fff5f5;
  color: #f56c6c;
  border: 1px solid #ffd6d6;
}

.cancel-btn:hover:not(:disabled) {
  background: #ffe0e0;
  border-color: #f56c6c;
}

.confirm-btn {
  background: linear-gradient(90deg, #ff5000, #ff8000);
  color: white;
}

.confirm-btn:hover:not(:disabled) {
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.3);
  transform: translateY(-1px);
}

/* 等待支付按钮 */
.waiting-btn {
  background: #f5f5f5;
  color: #999;
  border: 1px solid #ddd;
  cursor: not-allowed;
}

/* 支付成功按钮 */
.success-btn {
  background: linear-gradient(90deg, #67c23a, #85ce61);
  color: white;
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
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  font-size: 13px;
  color: #999;
}

.tip-icon {
  font-size: 14px;
}

/* ==================== 响应式适配 ==================== */
@media (max-width: 480px) {
  .pay-page {
    padding: 0 10px 40px;
  }

  .amount-value {
    font-size: 30px;
  }

  .action-btn {
    height: 44px;
    font-size: 14px;
  }
}
</style>
