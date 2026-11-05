<!--
  HomeView.vue - 拼团商城首页（拼多多风格）
  ===========================================
  【设计参考】拼多多首页布局
  - 顶部导航栏（橙色渐变）
  - 搜索栏
  - 轮播公告区
  - 商品卡片网格（带图片占位、价格、已拼件数、库存进度条）
  - 底部导航栏
-->

<template>
  <div class="home-page">

    <!-- ==================== 顶部导航栏 ==================== -->
    <header class="top-nav">
      <div class="nav-left">
        <span class="logo-icon">&#x1F6D2;</span>
        <span class="nav-title">拼团商城</span>
      </div>
      <div class="nav-right">
        <button class="nav-btn order-btn" @click="goToOrders">
          &#x1F4CB; 订单
        </button>
        <div class="user-avatar" @click="handleLogout" title="点击退出登录">
          <span>{{ userStore.username?.charAt(0)?.toUpperCase() || 'G' }}</span>
        </div>
      </div>
    </header>

    <!-- ==================== 搜索栏 ==================== -->
    <div class="search-bar">
      <div class="search-input-wrapper">
        <span class="search-icon">&#x1F50D;</span>
        <input type="text" class="search-input" placeholder="搜索商品、活动" disabled />
      </div>
    </div>

    <!-- ==================== 加载中状态 ==================== -->
    <div v-if="loading" class="loading-container">
      <div class="spinner"></div>
      <p>正在加载活动列表...</p>
    </div>

    <!-- ==================== 加载失败状态 ==================== -->
    <div v-else-if="error" class="error-container">
      <p class="error-icon">&#x1F635;</p>
      <p class="error-text">{{ error }}</p>
      <button class="retry-btn" @click="loadActivities">重新加载</button>
    </div>

    <!-- ==================== 主内容区域 ==================== -->
    <main v-else class="main-content">

      <!-- 轮播公告区 -->
      <section class="banner-area">
        <div class="banner-slide">
          <span class="banner-icon">&#x1F525;</span>
          <div class="banner-text-wrapper">
            <span class="banner-text">限时拼团特惠，低至5折起！多人拼更便宜~</span>
          </div>
          <span class="banner-action">GO &rsaquo;</span>
        </div>
      </section>

      <!-- 活动商品列表 -->
      <div v-if="activities.length > 0" class="activity-list">
        <div
          v-for="activity in activities"
          :key="activity.activityId"
          class="activity-card"
        >
          <!-- 活动标题栏 -->
          <div class="activity-header">
            <div class="activity-title-area">
              <h3 class="activity-name">{{ activity.activityName }}</h3>
              <span
                class="status-badge"
                :class="getStatusClass(activity.status)"
              >
                {{ getActivityStatusText(activity.status) }}
              </span>
            </div>
            <div class="activity-meta-row">
              <span class="meta-item">&#x1F465; {{ activity.target }}人团</span>
              <span class="meta-divider">|</span>
              <span class="meta-item">&#x23F0; {{ formatValidTime(activity.validTime) }}有效</span>
              <span class="meta-divider">|</span>
              <span class="meta-item">限{{ activity.takeLimitCount }}次</span>
            </div>
          </div>

          <!-- 商品网格 - 拼多多风格卡片 -->
          <div class="goods-grid">
            <div
              v-for="(goods, idx) in activity.goods"
              :key="goods.goodsId"
              class="goods-card"
              @click.stop="goToDetail(activity, goods)"
            >
              <!-- 商品图片区域 -->
              <div class="goods-image-wrapper">
                <img
                  v-if="goods.imageUrl"
                  :src="goods.imageUrl"
                  :alt="goods.goodsName"
                  class="goods-image"
                  @error="handleImageError"
                />
                <div v-else class="goods-image-placeholder">
                  <span>{{ goods.goodsName.charAt(0) }}</span>
                </div>
                <!-- 左上角角标 -->
                <div class="image-tag" v-if="idx === 0">
                  <span>爆款</span>
                </div>
                <div class="image-tag hot" v-else-if="idx === 1">
                  <span>热销</span>
                </div>
                <div class="image-tag new" v-else-if="idx === 2">
                  <span>新品</span>
                </div>
              </div>

              <!-- 商品信息区域 -->
              <div class="goods-info">
                <div class="goods-title">{{ goods.goodsName }}</div>

                <!-- 拼团信息 -->
                <div class="group-info-bar">
                  <span class="group-tag">{{ activity.target }}人团</span>
                </div>

                <!-- 价格区域 -->
                <div class="price-row">
                  <div class="price-group">
                    <span class="price-symbol">¥</span>
                    <span class="price-integer">{{ getPriceInteger(goods.originalPrice) }}</span>
                    <span class="price-decimal">.{{ getPriceDecimal(goods.originalPrice) }}</span>
                  </div>
                  <button class="quick-buy-btn" @click.stop="goToDetail(activity, goods)">
                    去拼团
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <div class="empty-icon">&#x1F4E6;</div>
        <p class="empty-text">暂无可用活动</p>
        <p class="empty-hint">更多活动即将上线，敬请期待</p>
      </div>
    </main>

    <!-- ==================== 底部导航栏 ==================== -->
    <nav class="bottom-nav">
      <div class="nav-item active">
        <span class="nav-icon">&#x1F3E0;</span>
        <span class="nav-label">首页</span>
      </div>
      <div class="nav-item" @click="goToOrders">
        <span class="nav-icon">&#x1F4CB;</span>
        <span class="nav-label">订单</span>
      </div>
      <div class="nav-item">
        <span class="nav-icon">&#x1F464;</span>
        <span class="nav-label">我的</span>
      </div>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { queryAllActivities } from '@/api/index'
import type {
  AllActivitiesResponseDTO,
  ActivityGoods,
  QueryAllActivitiesRequestDTO,
} from '@/api/types'

const router = useRouter()
const userStore = useUserStore()

const loading = ref<boolean>(true)
const error = ref<string>('')
const activities = ref<AllActivitiesResponseDTO[]>([])

async function loadActivities() {
  loading.value = true
  error.value = ''

  try {
    const request: QueryAllActivitiesRequestDTO = {
      userId: userStore.username || 'guest',
      source: 's01',
      channel: 'c01',
    }

    const response = await queryAllActivities(request)
    activities.value = response
  } catch (err) {
    console.error('加载活动列表失败:', err)
    error.value = '加载活动列表失败，请稍后重试'
    ElMessage.error('加载活动列表失败')
  } finally {
    loading.value = false
  }
}

function goToDetail(activity: AllActivitiesResponseDTO, goods: ActivityGoods) {
  router.push({
    name: 'detail',
    query: {
      activityId: String(activity.activityId),
      goodsId: goods.goodsId,
      goodsName: goods.goodsName,
      originalPrice: String(goods.originalPrice),
      imageUrl: goods.imageUrl || '',
      target: String(activity.target),
    },
  })
}

function goToOrders() {
  router.push({ path: '/orders' })
}

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push({ name: 'login' })
}

function formatDateTime(isoString: string): string {
  if (!isoString) return '--'
  const date = new Date(isoString)
  if (isNaN(date.getTime())) return '--'
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

function formatValidTime(minutes: number): string {
  if (minutes < 60) return `${minutes}分钟`
  if (minutes < 1440) return `${Math.floor(minutes / 60)}小时`
  return `${Math.floor(minutes / 1440)}天`
}

function getRemainingTime(endTime: string): string {
  if (!endTime) return ''
  const diff = new Date(endTime).getTime() - Date.now()
  if (diff <= 0) return '已过期'

  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))

  if (days > 0) return `剩余${days}天${hours}小时`
  if (hours > 0) return `剩余${hours}小时${minutes}分钟`
  return `剩余${minutes}分钟`
}

function getRemainingTimeClass(endTime: string): string {
  if (!endTime) return ''
  const diff = new Date(endTime).getTime() - Date.now()
  if (diff <= 0) return ''
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days < 1) return 'urgent'
  if (days < 3) return 'warning'
  return 'safe'
}

function getActivityStatusText(status: number): string {
  const statusMap: Record<number, string> = {
    0: '创建中',
    1: '进行中',
    2: '已过期',
    3: '已废弃',
  }
  return statusMap[status] || '未知'
}

function getStatusClass(status: number): string {
  const classMap: Record<number, string> = {
    0: 'info',
    1: 'success',
    2: 'warning',
    3: 'danger',
  }
  return classMap[status] || 'info'
}

// ==================== 工具函数 ====================

function getPriceInteger(price: number): string {
  return Math.floor(price).toString()
}

function getPriceDecimal(price: number): string {
  const decimal = (price % 1).toFixed(2).slice(2)
  return decimal === '00' ? '00' : decimal
}

function handleImageError(event: Event) {
  const img = event.target as HTMLImageElement
  img.style.display = 'none'
  const placeholder = img.nextElementSibling as HTMLElement
  if (placeholder) {
    placeholder.style.display = 'flex'
  }
}

onMounted(() => {
  loadActivities()
})
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.home-page {
  min-height: 100vh;
  background: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  padding-bottom: 60px;
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

.nav-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo-icon {
  font-size: 22px;
}

.nav-title {
  font-size: 18px;
  font-weight: 700;
  color: white;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.order-btn {
  padding: 6px 12px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 15px;
  background: rgba(255, 255, 255, 0.15);
  color: white;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.order-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.2s;
}

.user-avatar:hover {
  transform: scale(1.1);
}

.user-avatar span {
  font-size: 14px;
  font-weight: 700;
  color: #ff5000;
}

/* ==================== 搜索栏 ==================== */
.search-bar {
  padding: 10px 15px;
  background: white;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  padding: 0 15px;
  height: 36px;
  background: #f5f5f5;
  border-radius: 18px;
}

.search-icon {
  font-size: 14px;
  color: #999;
  margin-right: 8px;
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 14px;
  color: #333;
  outline: none;
}

.search-input::placeholder {
  color: #bbb;
}

/* ==================== 加载/错误状态 ==================== */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60vh;
  gap: 16px;
  color: #999;
}

.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #f0f0f0;
  border-top-color: #ff5000;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60vh;
  gap: 12px;
}

.error-icon {
  font-size: 60px;
}

.error-text {
  color: #ff5000;
  font-size: 16px;
}

.retry-btn {
  padding: 10px 24px;
  background: linear-gradient(90deg, #ff5000, #ff8000);
  color: white;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.retry-btn:hover {
  opacity: 0.9;
}

/* ==================== 主内容区域 ==================== */
.main-content {
  max-width: 900px;
  margin: 0 auto;
  padding: 10px 10px 0;
}

/* ==================== 轮播公告区 ==================== */
.banner-area {
  margin-bottom: 10px;
}

.banner-slide {
  display: flex;
  align-items: center;
  padding: 12px 15px;
  background: linear-gradient(90deg, #fff3eb, #ffe8d9);
  border-radius: 8px;
  border: 1px solid #ffd6c7;
}

.banner-icon {
  font-size: 20px;
  margin-right: 10px;
}

.banner-text-wrapper {
  flex: 1;
  overflow: hidden;
}

.banner-text {
  font-size: 14px;
  color: #ff5000;
  font-weight: 500;
  white-space: nowrap;
  animation: scrollText 15s linear infinite;
}

@keyframes scrollText {
  0% { transform: translateX(100%); }
  100% { transform: translateX(-100%); }
}

.banner-action {
  padding: 4px 10px;
  background: #ff5000;
  color: white;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  margin-left: 10px;
}

/* ==================== 活动列表 ==================== */
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.activity-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

/* ==================== 活动头部 ==================== */
.activity-header {
  padding: 12px 15px;
  border-bottom: 1px solid #f5f5f5;
}

.activity-title-area {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.activity-name {
  font-size: 16px;
  font-weight: 700;
  color: #333;
  margin: 0;
}

.status-badge {
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  color: white;
}

.status-badge.info {
  background: #909399;
}

.status-badge.success {
  background: linear-gradient(90deg, #ff5000, #ff7040);
}

.status-badge.warning {
  background: #e6a23c;
}

.status-badge.danger {
  background: #f56c6c;
}

.activity-meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
}

.meta-item {
  display: flex;
  align-items: center;
}

.meta-divider {
  color: #ddd;
}

/* ==================== 商品网格 ==================== */
.goods-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  padding: 10px;
}

/* ==================== 商品卡片（拼多多风格） ==================== */
.goods-card {
  background: white;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.2s;
}

.goods-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.15);
  border-color: #ff5000;
}

.goods-image-wrapper {
  position: relative;
  width: 100%;
  height: 140px;
  background: linear-gradient(135deg, #fff5eb, #ffe8d9);
  display: flex;
  align-items: center;
  justify-content: center;
}

.goods-image-placeholder span {
  font-size: 48px;
  font-weight: 700;
  color: #ff5000;
  opacity: 0.6;
}

.goods-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 3px 8px;
  background: linear-gradient(90deg, #ff5000, #ff7040);
  color: white;
  font-size: 11px;
  font-weight: 600;
  border-radius: 4px;
}

.image-tag.hot {
  background: linear-gradient(90deg, #ff3030, #ff5050);
}

.image-tag.new {
  background: linear-gradient(90deg, #67c23a, #85ce61);
}

.goods-info {
  padding: 10px;
}

.goods-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.group-info-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.group-tag {
  padding: 2px 6px;
  background: #fff3eb;
  color: #ff5000;
  font-size: 11px;
  border-radius: 3px;
  font-weight: 500;
}

.price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.price-group {
  display: flex;
  align-items: baseline;
}

.price-symbol {
  font-size: 12px;
  color: #ff5000;
  font-weight: 600;
}

.price-integer {
  font-size: 20px;
  color: #ff5000;
  font-weight: 700;
}

.price-decimal {
  font-size: 12px;
  color: #ff5000;
  font-weight: 600;
}

.quick-buy-btn {
  padding: 5px 12px;
  background: linear-gradient(90deg, #ff5000, #ff7040);
  color: white;
  border: none;
  border-radius: 15px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-buy-btn:hover {
  background: linear-gradient(90deg, #ff4000, #ff6030);
  transform: scale(1.05);
}

/* ==================== 空状态 ==================== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.empty-icon {
  font-size: 60px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  color: #333;
  margin-bottom: 8px;
}

.empty-hint {
  font-size: 13px;
  color: #999;
}

/* ==================== 底部导航栏 ==================== */
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 55px;
  background: white;
  display: flex;
  justify-content: space-around;
  align-items: center;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.08);
  z-index: 100;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  cursor: pointer;
  padding: 5px 20px;
  transition: all 0.2s;
}

.nav-item:hover {
  transform: scale(1.05);
}

.nav-item.active .nav-icon {
  color: #ff5000;
}

.nav-item.active .nav-label {
  color: #ff5000;
}

.nav-icon {
  font-size: 20px;
  color: #666;
}

.nav-label {
  font-size: 11px;
  color: #666;
}

/* ==================== 响应式适配 ==================== */
@media (max-width: 480px) {
  .top-nav {
    padding: 0 10px;
  }

  .nav-title {
    font-size: 16px;
  }

  .goods-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 6px;
    padding: 8px;
  }

  .goods-image-wrapper {
    height: 120px;
  }

  .goods-info {
    padding: 8px;
  }

  .goods-title {
    font-size: 13px;
  }

  .price-integer {
    font-size: 18px;
  }
}

@media (min-width: 481px) and (max-width: 768px) {
  .goods-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (min-width: 769px) {
  .goods-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}
</style>
