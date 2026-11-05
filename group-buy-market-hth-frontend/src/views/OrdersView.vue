<!--
  OrdersView.vue - 我的订单页面（拼多多风格）
  ============================================
  【设计参考】拼多多订单列表页
  - 顶部导航栏（橙色渐变）
  - 订单状态筛选标签
  - 订单卡片列表（带商品图片、状态、操作按钮）
  - 订单详情弹窗
-->

<template>
  <div class="orders-page">

    <!-- ==================== 顶部导航栏 ==================== -->
    <header class="top-nav">
      <div class="nav-left">
        <button class="back-btn" @click="goHome">
          <span>&#x2190;</span>
        </button>
      </div>
      <div class="nav-center">
        <span class="nav-title">我的订单</span>
      </div>
      <div class="nav-right">
        <span class="username">{{ maskedName }}</span>
      </div>
    </header>

    <!-- ==================== 订单状态筛选 ==================== -->
    <div class="filter-bar">
      <div class="filter-group">
        <button
          :class="['filter-btn', { active: activeFilter === 'all' }]"
          @click="activeFilter = 'all'"
        >
          全部
        </button>
        <button
          :class="['filter-btn', { active: activeFilter === '待支付' }]"
          @click="activeFilter = '待支付'"
        >
          待支付
        </button>
        <button
          :class="['filter-btn', { active: activeFilter === '已支付' }]"
          @click="activeFilter = '已支付'"
        >
          已支付
        </button>
        <button
          :class="['filter-btn', { active: activeFilter === '已退单' }]"
          @click="activeFilter = '已退单'"
        >
          已退单
        </button>
      </div>
    </div>

    <!-- ==================== 订单列表 ==================== -->
    <div v-if="filteredOrders.length > 0" class="order-list">
      <div
        v-for="order in filteredOrders"
        :key="order.outTradeNo"
        class="order-card"
        @click="showDetail(order)"
      >
        <!-- 订单卡片头部 -->
        <div class="card-header">
          <div class="header-left">
            <span class="order-icon">&#x1F4E6;</span>
            <span class="order-no">订单号: {{ order.outTradeNo }}</span>
          </div>
          <span :class="['status-tag', getStatusClass(order.status)]">
            {{ order.status }}
          </span>
        </div>

        <!-- 订单内容区域 -->
        <div class="card-body">
          <!-- 商品信息 -->
          <div class="goods-info">
            <div class="goods-image">
              <span>{{ order.goodsName?.charAt(0) || '商' }}</span>
            </div>
            <div class="goods-detail">
              <div class="goods-name">{{ order.goodsName }}</div>
              <div class="goods-spec">
                <span v-if="order.teamId">团号: {{ order.teamId }}</span>
                <span v-else>单独购买</span>
              </div>
            </div>
            <div class="goods-price">
              <span class="price-symbol">¥</span>
              <span class="price-value">{{ formatPrice(order.payPrice) }}</span>
            </div>
          </div>

          <!-- 订单时间 -->
          <div class="order-time">
            <span class="time-label">下单时间</span>
            <span class="time-value">{{ formatDisplayTime(order.createdAt) }}</span>
          </div>
        </div>

        <!-- 订单操作按钮 -->
        <div class="card-actions">
          <button class="action-btn detail-btn" @click.stop="showDetail(order)">
            查看详情
          </button>
          <button
            v-if="order.status === '待支付'"
            class="action-btn pay-btn"
            @click.stop="goToPay(order)"
          >
            去支付
          </button>
          <button
            v-if="order.status === '待支付' || order.status === '已支付'"
            class="action-btn cancel-btn"
            :disabled="refundingTradeNo === order.outTradeNo"
            @click.stop="handleRefund(order)"
          >
            {{ refundingTradeNo === order.outTradeNo ? '退单中...' : '退单' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ==================== 空状态 ==================== -->
    <div v-else class="empty-state">
      <div class="empty-icon">&#x1F4CB;</div>
      <p class="empty-text">
        {{ activeFilter === 'all' ? '暂无订单记录' : '暂无' + activeFilter + '的订单' }}
      </p>
      <button class="action-btn primary-btn" @click="goHome">去逛逛</button>
    </div>

    <!-- ==================== 订单详情弹窗 ==================== -->
    <div v-if="dialogVisible" class="modal-overlay" @click.self="dialogVisible = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>订单详情</h3>
          <button class="modal-close" @click="dialogVisible = false">&#x2715;</button>
        </div>

        <div v-if="selectedOrder" class="modal-body">
          <!-- 订单状态卡片 -->
          <div class="status-card">
            <div class="status-icon">
              <span :class="['status-badge', getStatusClass(selectedOrder.status)]">
                {{ selectedOrder.status }}
              </span>
            </div>
            <div class="status-info">
              <div class="status-title">{{ getStatusTitle(selectedOrder.status) }}</div>
              <div class="status-desc">{{ getStatusDesc(selectedOrder.status) }}</div>
            </div>
          </div>

          <!-- 详细信息列表 -->
          <div class="info-section">
            <div class="info-row">
              <span class="info-label">订单号</span>
              <span class="info-value mono">{{ selectedOrder.orderId || '--' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">交易单号</span>
              <span class="info-value mono">{{ selectedOrder.outTradeNo }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">商品名称</span>
              <span class="info-value">{{ selectedOrder.goodsName }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">支付金额</span>
              <span class="info-value price">¥{{ formatPrice(selectedOrder.payPrice) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">拼团ID</span>
              <span class="info-value">{{ selectedOrder.teamId || '--' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">下单时间</span>
              <span class="info-value">{{ formatDisplayTime(selectedOrder.createdAt) }}</span>
            </div>
            <div v-if="selectedOrder.updateTime" class="info-row">
              <span class="info-label">更新时间</span>
              <span class="info-value">{{ formatDisplayTime(selectedOrder.updateTime) }}</span>
            </div>
            <div v-if="selectedOrder.userId" class="info-row">
              <span class="info-label">用户ID</span>
              <span class="info-value">{{ selectedOrder.userId }}</span>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="action-btn primary-btn" @click="dialogVisible = false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import { refundMarketPayOrder } from '../api/trade'
import { queryOrderStatus } from '../api/pay'
import type { RefundMarketPayOrderRequestDTO } from '../api/types'
import {
  type OrderRecord,
  getOrderList,
  formatPrice,
  updateOrderStatus,
  maskUsername,
} from '../utils/index'

const router = useRouter()
const userStore = useUserStore()

const orderList = ref<OrderRecord[]>([])
const activeFilter = ref<string>('all')
const dialogVisible = ref(false)
const selectedOrder = ref<OrderRecord | null>(null)
const refundingTradeNo = ref<string>('')

const maskedName = computed(() => {
  return maskUsername(userStore.username || '用户')
})

const filteredOrders = computed(() => {
  if (activeFilter.value === 'all') {
    return orderList.value
  }
  return orderList.value.filter(order => order.status === activeFilter.value)
})

onMounted(() => {
  loadOrders()
})

async function loadOrders() {
  const allOrders = getOrderList()
  const userOrders = allOrders.filter(order => order.userId === userStore.username)

  let hasUpdate = false
  for (const order of userOrders) {
    if (order.status === '待支付') {
      try {
        const backendStatus = await queryOrderStatus(order.outTradeNo)
        if (backendStatus === 'PAY_SUCCESS' || backendStatus === 'MARKET' || backendStatus === 'DEAL_DONE') {
          order.status = '已支付'
          updateOrderStatus(order.outTradeNo, '已支付')
          hasUpdate = true
        } else if (backendStatus === 'CLOSE') {
          order.status = '已退单'
          updateOrderStatus(order.outTradeNo, '已退单')
          hasUpdate = true
        }
      } catch (error) {
        console.warn('同步订单状态失败:', order.outTradeNo, error)
      }
    }
  }

  if (hasUpdate) {
    const updatedOrders = getOrderList()
    orderList.value = updatedOrders.filter(order => order.userId === userStore.username)
  } else {
    orderList.value = userOrders
  }
}

function getStatusClass(status: string): string {
  if (status === '待支付') return 'status-pending'
  if (status === '已支付') return 'status-paid'
  if (status === '已退单') return 'status-refunded'
  return ''
}

function getStatusTitle(status: string): string {
  if (status === '待支付') return '等待支付'
  if (status === '已支付') return '支付成功'
  if (status === '已退单') return '订单已取消'
  return '未知状态'
}

function getStatusDesc(status: string): string {
  if (status === '待支付') return '请尽快完成支付，超时订单将自动取消'
  if (status === '已支付') return '您的订单已支付成功，等待发货'
  if (status === '已退单') return '订单已取消，退款将原路返回'
  return ''
}

function formatDisplayTime(timeStr: string): string {
  if (!timeStr) return '--'
  try {
    const date = new Date(timeStr)
    if (isNaN(date.getTime())) return timeStr
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const seconds = String(date.getSeconds()).padStart(2, '0')
    return year + '-' + month + '-' + day + ' ' + hours + ':' + minutes + ':' + seconds
  } catch {
    return timeStr
  }
}

function showDetail(order: OrderRecord) {
  selectedOrder.value = order
  dialogVisible.value = true
}

async function handleRefund(order: OrderRecord) {
  try {
    await ElMessageBox.confirm(
      '确定要退单吗？\n交易单号：' + order.outTradeNo + '\n金额：' + formatPrice(order.payPrice),
      '确认退单',
      {
        confirmButtonText: '确定退单',
        cancelButtonText: '再想想',
        type: 'warning',
      }
    )

    refundingTradeNo.value = order.outTradeNo

    const requestParams: RefundMarketPayOrderRequestDTO = {
      userId: userStore.username,
      outTradeNo: order.outTradeNo,
      source: 's01',
      channel: 'c01',
    }

    await refundMarketPayOrder(requestParams)
    updateOrderStatus(order.outTradeNo, '已退单')
    loadOrders()
    ElMessage.success('退单成功！订单已取消')

  } catch (error: any) {
    if (error === 'cancel' || error?.toString().includes('cancel')) {
      return
    }
    const errorMsg = error?.response?.data?.info || error?.message || '退单失败，请稍后重试'
    ElMessage.error(errorMsg)
  } finally {
    refundingTradeNo.value = ''
  }
}

function goToPay(order: OrderRecord) {
  router.push({
    name: 'pay',
    query: {
      orderId: order.orderId,
      teamId: order.teamId,
      payPrice: order.payPrice.toString(),
      originalPrice: order.originalPrice?.toString() || '0',
      deductionPrice: order.deductionPrice?.toString() || '0',
      outTradeNo: order.outTradeNo,
      goodsId: order.goodsId,
      goodsName: order.goodsName,
      activityId: order.activityId?.toString() || '0',
    },
  })
}

function goHome() {
  router.push({ name: 'home' })
}
</script>

<style scoped>
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

.orders-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 24px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* ==================== 顶部导航栏 ==================== */
.top-nav {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 15px;
  height: 50px;
  background: linear-gradient(90deg, #ff5000, #ff7040);
}

.nav-left, .nav-center, .nav-right {
  display: flex;
  align-items: center;
}

.back-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 18px;
  color: white;
  transition: all 0.2s;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.05);
}

.nav-title {
  font-size: 16px;
  font-weight: 600;
  color: white;
}

.username {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
}

/* ==================== 筛选标签栏 ==================== */
.filter-bar {
  max-width: 640px;
  margin: 12px auto 0;
  padding: 0 15px;
  display: flex;
  justify-content: center;
}

.filter-group {
  display: flex;
  gap: 0;
  background: white;
  border-radius: 20px;
  padding: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.filter-btn {
  border: none;
  background: transparent;
  border-radius: 16px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-btn:hover {
  color: #ff5000;
}

.filter-btn.active {
  background: linear-gradient(90deg, #ff5000, #ff7040);
  color: white;
}

/* ==================== 订单列表 ==================== */
.order-list {
  max-width: 640px;
  margin: 12px auto 0;
  padding: 0 15px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* ==================== 订单卡片 ==================== */
.order-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.2s;
}

.order-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

/* 卡片头部 */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 15px;
  border-bottom: 1px solid #f5f5f5;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-icon {
  font-size: 16px;
}

.order-no {
  font-size: 12px;
  color: #999;
  font-family: monospace;
}

/* 状态标签 */
.status-tag {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  color: white;
}

.status-pending {
  background: linear-gradient(90deg, #ffa500, #ff8c00);
}

.status-paid {
  background: linear-gradient(90deg, #67c23a, #529b2e);
}

.status-refunded {
  background: #909399;
}

/* 卡片内容 */
.card-body {
  padding: 12px 15px;
}

.goods-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.goods-image {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  background: linear-gradient(135deg, #fff5eb, #ffe8d9);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.goods-image span {
  font-size: 24px;
  font-weight: 700;
  color: #ff5000;
  opacity: 0.8;
}

.goods-detail {
  flex: 1;
  min-width: 0;
}

.goods-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.goods-spec {
  font-size: 12px;
  color: #999;
}

.goods-price {
  text-align: right;
}

.price-symbol {
  font-size: 12px;
  color: #ff5000;
  font-weight: 600;
}

.price-value {
  font-size: 18px;
  color: #ff5000;
  font-weight: 700;
}

.order-time {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #fafafa;
  border-radius: 6px;
}

.time-label {
  font-size: 12px;
  color: #999;
}

.time-value {
  font-size: 12px;
  color: #666;
}

/* 卡片操作按钮 */
.card-actions {
  display: flex;
  gap: 8px;
  padding: 12px 15px;
  border-top: 1px solid #f5f5f5;
}

.action-btn {
  flex: 1;
  padding: 10px 16px;
  border: none;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover:not(:disabled) {
  transform: translateY(-1px);
}

.action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.detail-btn {
  background: #f5f5f5;
  color: #666;
}

.detail-btn:hover {
  background: #e8e8e8;
}

.pay-btn {
  background: linear-gradient(90deg, #67c23a, #85ce61);
  color: white;
}

.pay-btn:hover {
  background: linear-gradient(90deg, #85ce61, #95d475);
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3);
}

.cancel-btn {
  background: #fff3eb;
  color: #ff5000;
}

.cancel-btn:hover {
  background: #ffe8d9;
}

.primary-btn {
  background: linear-gradient(90deg, #ff5000, #ff7040);
  color: white;
  padding: 12px 32px;
}

.primary-btn:hover {
  background: linear-gradient(90deg, #ff4000, #ff6030);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.3);
}

/* ==================== 空状态 ==================== */
.empty-state {
  max-width: 640px;
  margin: 60px auto;
  padding: 60px 20px;
  text-align: center;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.empty-icon {
  font-size: 60px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 14px;
  color: #999;
  margin-bottom: 24px;
}

/* ==================== 模态弹窗 ==================== */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 100%;
  max-width: 480px;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f5f5f5;
}

.modal-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.modal-close {
  width: 32px;
  height: 32px;
  border: none;
  background: #f5f5f5;
  border-radius: 50%;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.modal-close:hover {
  background: #e8e8e8;
}

.modal-body {
  padding: 20px;
}

/* 状态卡片 */
.status-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: linear-gradient(135deg, #fff5eb, #ffe8d9);
  border-radius: 12px;
  margin-bottom: 20px;
}

.status-icon {
  flex-shrink: 0;
}

.status-badge {
  display: inline-block;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  color: white;
}

.status-badge.status-pending {
  background: linear-gradient(90deg, #ffa500, #ff8c00);
}

.status-badge.status-paid {
  background: linear-gradient(90deg, #67c23a, #529b2e);
}

.status-badge.status-refunded {
  background: #909399;
}

.status-info {
  flex: 1;
}

.status-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.status-desc {
  font-size: 13px;
  color: #666;
}

/* 信息区域 */
.info-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 8px;
}

.info-label {
  font-size: 13px;
  color: #999;
}

.info-value {
  font-size: 13px;
  color: #333;
  font-weight: 500;
}

.info-value.mono {
  font-family: monospace;
  font-size: 12px;
}

.info-value.price {
  color: #ff5000;
  font-size: 16px;
  font-weight: 700;
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #f5f5f5;
  display: flex;
  justify-content: center;
}

/* ==================== 响应式适配 ==================== */
@media (max-width: 480px) {
  .top-nav {
    padding: 0 10px;
  }

  .filter-group {
    padding: 3px;
  }

  .filter-btn {
    padding: 6px 12px;
    font-size:2px;
  }

  .order-list {
    padding: 0 10px;
  }

  .goods-image {
    width: 50px;
    height: 50px;
  }

  .goods-image span {
    font-size: 20px;
  }

  .price-value {
    font-size: 16px;
  }

  .card-actions {
    flex-wrap: wrap;
  }

  .action-btn {
    padding: 8px 12px;
    font-size: 12px;
  }
}
</style>
