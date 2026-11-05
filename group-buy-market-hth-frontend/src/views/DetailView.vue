<!--
  DetailView.vue - 商品详情页（拼多多风格）
  ==========================================
  【设计参考】拼多多商品详情页
  - 顶部返回按钮 + 分享
  - 商品图片轮播
  - 拼团价格卡片（原价、拼团价、已拼件数）
  - 拼团列表（带头像、进度条、倒计时）
  - 商品详情（图文介绍）
  - 底部固定操作栏（单独购买、开团购买）
-->

<template>
  <div class="detail-page">

    <!-- ==================== 顶部导航栏 ==================== -->
    <header class="detail-nav">
      <button class="back-btn" @click="goBack">
        <span>&#x2190;</span>
      </button>
      <div class="nav-title">商品详情</div>
      <button class="share-btn" @click="handleShare">
        <span>&#x1F4E4;</span>
      </button>
    </header>

    <!-- ==================== 商品图片轮播 ==================== -->
    <div class="product-gallery">
      <div class="gallery-wrapper">
        <img
          v-for="(img, index) in productImages"
          :key="index"
          :src="img"
          class="gallery-image"
          :class="{ active: currentImageIndex === index }"
          @click="currentImageIndex = index"
        />
      </div>
      <div class="gallery-dots">
        <span
          v-for="(_, index) in productImages"
          :key="index"
          class="dot"
          :class="{ active: currentImageIndex === index }"
          @click="currentImageIndex = index"
        ></span>
      </div>
    </div>

    <!-- ==================== 商品信息卡片 ==================== -->
    <div class="product-card">
      <!-- 价格区域 -->
      <div class="price-section">
        <div class="price-main">
          <span class="price-symbol">¥</span>
          <span class="price-integer">{{ getPriceInteger(goodsInfo.payPrice) }}</span>
          <span class="price-decimal">.{{ getPriceDecimal(goodsInfo.payPrice) }}</span>
        </div>
        <div class="price-original">
          <span>原价 </span>
          <span class="line-through">¥{{ goodsInfo.originalPrice.toFixed(2) }}</span>
        </div>
      </div>

      <!-- 促销标签 -->
      <div class="promo-tags">
        <span class="promo-tag">
          <span class="tag-icon">&#x1F525;</span>
          直降¥{{ goodsInfo.deductionPrice.toFixed(2) }}
        </span>
      </div>

      <!-- 商品标题 -->
      <h1 class="product-title">{{ goodsInfo.goodsName }}</h1>

      <!-- 商品描述 -->
      <div class="product-desc">
        <span class="desc-tag">品质保证</span>
        <span class="desc-tag">正品保障</span>
        <span class="desc-tag">极速退款</span>
      </div>
    </div>

    <!-- ==================== 拼团信息卡片 ==================== -->
    <div class="group-card">
      <div class="group-card-header">
        <div class="group-card-title">
          <span class="title-icon">&#x1F465;</span>
          <span>正在拼团 ({{ teamList.length }})</span>
        </div>
        <div class="group-stat">
          已拼{{ teamStatistic.allTeamCompleteCount }}团
        </div>
      </div>

      <!-- 拼团列表 -->
      <div class="group-list" v-if="teamList.length > 0">
        <div
          v-for="team in teamList"
          :key="team.teamId"
          class="group-item"
        >
          <div class="group-left">
            <!-- 团长头像 -->
            <div class="leader-avatar">
              <span>{{ team.userId.charAt(0).toUpperCase() }}</span>
            </div>
            <div class="group-info">
              <div class="leader-name">{{ team.userId }}</div>
              <div class="group-progress">
                <div class="progress-bar">
                  <div
                    class="progress-fill"
                    :style="{ width: (team.completeCount / team.targetCount * 100) + '%' }"
                  ></div>
                </div>
                <span class="progress-text">
                  还差{{ team.targetCount - team.completeCount }}人
                </span>
              </div>
              <div class="group-countdown">
                <span class="countdown-icon">&#x23F0;</span>
                <span>{{ team.validTimeCountdown || '即将结束' }}</span>
              </div>
            </div>
          </div>
          <button class="join-btn" @click="handleJoinTeam(team)">
            去参团
          </button>
        </div>
      </div>

      <!-- 空状态 -->
      <div class="empty-group" v-else>
        <div class="empty-icon">&#x1F389;</div>
        <p class="empty-text">暂无进行中的拼团</p>
        <p class="empty-hint">快来开团，邀请好友一起拼！</p>
      </div>
    </div>

    <!-- ==================== 商品详情卡片 ==================== -->
    <div class="detail-card">
      <div class="detail-card-header">
        <span class="detail-icon">&#x1F4CB;</span>
        <span>商品详情</span>
      </div>
      <div class="detail-content">
        <div class="detail-section">
          <h4 class="section-title">商品规格</h4>
          <div class="spec-list">
            <div class="spec-item">
              <span class="spec-label">商品名称</span>
              <span class="spec-value">{{ goodsInfo.goodsName }}</span>
            </div>
            <div class="spec-item">
              <span class="spec-label">拼团人数</span>
              <span class="spec-value">{{ targetCount }}人</span>
            </div>
            <div class="spec-item">
              <span class="spec-label">有效期</span>
              <span class="spec-value">24小时</span>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h4 class="section-title">购买须知</h4>
          <div class="notice-list">
            <div class="notice-item">
              <span class="notice-icon">&#x2714;</span>
              <span>拼团成功后自动发货</span>
            </div>
            <div class="notice-item">
              <span class="notice-icon">&#x2714;</span>
              <span>未成团自动退款</span>
            </div>
            <div class="notice-item">
              <span class="notice-icon">&#x2714;</span>
              <span>支持7天无理由退换</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== 底部操作栏 ==================== -->
    <div class="action-bar">
      <button class="action-btn buy-alone" @click="handleBuyAlone">
        <span class="btn-label">单独购买</span>
        <span class="btn-price">¥{{ goodsInfo.originalPrice.toFixed(2) }}</span>
      </button>
      <button class="action-btn group-buy" @click="handleCreateTeam">
        <span class="btn-label">开团购买</span>
        <span class="btn-price">¥{{ goodsInfo.payPrice.toFixed(2) }}</span>
      </button>
    </div>

    <!-- ==================== 支付弹窗 ==================== -->
    <div class="payment-overlay" v-if="paymentDialogVisible" @click="cancelPayment">
      <div class="payment-dialog" @click.stop>
        <div class="dialog-header">
          <h3>请扫码支付</h3>
          <button class="close-btn" @click="cancelPayment">&#x2715;</button>
        </div>
        <div class="dialog-body">
          <p class="payment-amount">支付金额：<span>¥{{ currentPayPrice.toFixed(2) }}</span></p>
          <div class="qr-code-placeholder">
            <span class="qr-icon">&#x1F4F1;</span>
            <p>扫码支付区域</p>
          </div>
          <div class="payment-tips">
            <p>请使用微信或支付宝扫码支付</p>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="cancel-btn" @click="cancelPayment">取消支付</button>
          <button class="confirm-btn" @click="completePayment">支付完成</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { queryGroupBuyMarketConfig } from '../api/index'
import type {
  GoodsMarketResponseDTO,
  TeamInfo,
} from '../api/types'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const productImages = ref<string[]>([])

const currentImageIndex = ref(0)

const goodsInfo = ref({
  goodsId: '',
  goodsName: '拼团商品',
  originalPrice: 0,
  deductionPrice: 0,
  payPrice: 0,
  imageUrl: '',
})

const teamList = ref<TeamInfo[]>([])

const teamStatistic = ref({
  allTeamCount: 0,
  allTeamCompleteCount: 0,
  allTeamUserCount: 0,
})

const activityId = ref<number>(0)
const targetCount = ref<number>(3)
const paymentDialogVisible = ref(false)
const currentPayPrice = ref(0)
const currentTeam = ref<TeamInfo | null>(null)

onMounted(() => {
  const query = route.query
  activityId.value = Number(query.activityId) || 0
  goodsInfo.value.goodsId = (query.goodsId as string) || ''
  goodsInfo.value.goodsName = (query.goodsName as string) || '拼团商品'
  goodsInfo.value.originalPrice = Number(query.originalPrice) || 0
  goodsInfo.value.imageUrl = (query.imageUrl as string) || ''
  targetCount.value = Number(query.target) || 3

  // 设置商品图片列表
  if (goodsInfo.value.imageUrl) {
    // 如果有图片URL，使用该图片生成多张图片（模拟轮播）
    productImages.value = [
      goodsInfo.value.imageUrl,
      goodsInfo.value.imageUrl,
      goodsInfo.value.imageUrl,
    ]
  } else {
    // 否则使用默认占位图
    productImages.value = [
      'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="375"%3E%3Crect fill="%23ff5000" width="400" height="375"/%3E%3Ctext fill="white" font-size="24" x="50%" y="50%" text-anchor="middle" dy=".3em"%3E' + encodeURIComponent(goodsInfo.value.goodsName.substring(0, 6)) + '%3C/text%3E%3C/svg%3E',
    ]
  }

  if (!activityId.value || !goodsInfo.value.goodsId) {
    ElMessage.error('商品参数缺失，请从首页重新选择')
    router.push({ name: 'home' })
    return
  }

  loadData()
})

async function loadData() {
  try {
    const response: GoodsMarketResponseDTO = await queryGroupBuyMarketConfig({
      userId: userStore.username || 'guest',
      source: 's01',
      channel: 'c01',
      goodsId: goodsInfo.value.goodsId,
    })

    goodsInfo.value = {
      goodsId: response.goods.goodsId,
      goodsName: goodsInfo.value.goodsName,
      originalPrice: response.goods.originalPrice,
      deductionPrice: response.goods.deductionPrice,
      payPrice: response.goods.payPrice,
    }

    teamList.value = response.teamList || []

    teamStatistic.value = response.teamStatistic || {
      allTeamCount: 0,
      allTeamCompleteCount: 0,
      allTeamUserCount: 0,
    }
  } catch (error) {
    console.error('加载商品详情失败:', error)
    ElMessage.error('加载商品信息失败')
  }
}

function goBack() {
  router.back()
}

function handleShare() {
  ElMessage.success('分享功能开发中')
}

function handleJoinTeam(team: TeamInfo) {
  currentTeam.value = team
  currentPayPrice.value = goodsInfo.value.payPrice
  goToPay(team.teamId)
}

function handleBuyAlone() {
  currentPayPrice.value = goodsInfo.value.originalPrice
  currentTeam.value = null
  paymentDialogVisible.value = true
}

function handleCreateTeam() {
  currentPayPrice.value = goodsInfo.value.payPrice
  currentTeam.value = null
  goToPay('')
}

// 跳转支付页。锁单由支付后端 create_pay_order 统一发起，
// 支付后端生成的 orderId 即为拼团后端的 outTradeNo，避免前端重复锁单产生两条记录。
function goToPay(teamId: string) {
  router.push({
    name: 'pay',
    query: {
      teamId,
      payPrice: goodsInfo.value.payPrice.toString(),
      originalPrice: goodsInfo.value.originalPrice.toString(),
      deductionPrice: goodsInfo.value.deductionPrice.toString(),
      goodsId: goodsInfo.value.goodsId,
      goodsName: goodsInfo.value.goodsName,
      activityId: activityId.value.toString(),
    },
  })
}

function cancelPayment() {
  paymentDialogVisible.value = false
  currentTeam.value = null
}

function completePayment() {
  ElMessage.success('支付成功！')
  paymentDialogVisible.value = false
  router.push({ name: 'orders' })
}

// ==================== 辅助函数 ====================

function getPriceInteger(price: number): string {
  return Math.floor(price).toString()
}

function getPriceDecimal(price: number): string {
  const decimal = (price % 1).toFixed(2).slice(2)
  return decimal === '00' ? '00' : decimal
}
</script>

<style scoped>
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

.detail-page {
  background: #f5f5f5;
  min-height: 100vh;
  padding-bottom: 70px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* ==================== 顶部导航栏 ==================== */
.detail-nav {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 15px;
  height: 50px;
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.back-btn, .share-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: #f5f5f5;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 18px;
  transition: all 0.2s;
}

.back-btn:hover, .share-btn:hover {
  background: #e8e8e8;
  transform: scale(1.05);
}

.nav-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

/* ==================== 商品图片轮播 ==================== */
.product-gallery {
  background: white;
  padding: 10px 0;
}

.gallery-wrapper {
  display: flex;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  scrollbar-width: none;
}

.gallery-wrapper::-webkit-scrollbar {
  display: none;
}

.gallery-image {
  flex: 0 0 100%;
  width: 100%;
  height: 375px;
  object-fit: cover;
  scroll-snap-align: start;
  display: none;
}

.gallery-image.active {
  display: block;
}

.gallery-dots {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 10px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ddd;
  cursor: pointer;
  transition: all 0.2s;
}

.dot.active {
  background: #ff5000;
  width: 20px;
  border-radius: 4px;
}

/* ==================== 商品信息卡片 ==================== */
.product-card {
  background: white;
  margin: 10px;
  border-radius: 12px;
  padding: 15px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.price-section {
  margin-bottom: 12px;
}

.price-main {
  display: flex;
  align-items: baseline;
  margin-bottom: 8px;
}

.price-symbol {
  font-size: 16px;
  color: #ff5000;
  font-weight: 600;
}

.price-integer {
  font-size: 32px;
  color: #ff5000;
  font-weight: 700;
}

.price-decimal {
  font-size: 16px;
  color: #ff5000;
  font-weight: 600;
}

.price-original {
  font-size: 13px;
  color: #999;
}

.line-through {
  text-decoration: line-through;
}

.promo-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.promo-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #fff3eb;
  color: #ff5000;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.promo-tag.stock-tag {
  background: #ffebeb;
  color: #ff3030;
}

.tag-icon {
  font-size: 12px;
}

.product-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  line-height: 1.4;
  margin-bottom: 12px;
}

.product-desc {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.desc-tag {
  padding: 4px 10px;
  background: #f5f5f5;
  color: #666;
  border-radius: 4px;
  font-size: 12px;
}

/* ==================== 拼团信息卡片 ==================== */
.group-card {
  background: white;
  margin: 10px;
  border-radius: 12px;
  padding: 15px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.group-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 15px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f5f5f5;
}

.group-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.title-icon {
  font-size: 18px;
}

.group-stat {
  font-size: 13px;
  color: #999;
}

.group-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.group-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background: #fafafa;
  border-radius: 10px;
  transition: all 0.2s;
}

.group-item:hover {
  background: #fff3eb;
}

.group-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.leader-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff5000, #ff7040);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
  font-weight: 600;
}

.group-info {
  flex: 1;
}

.leader-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 6px;
}

.group-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.progress-bar {
  flex: 1;
  height: 6px;
  background: #e8e8e8;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #ff5000, #ff7040);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 12px;
  color: #ff5000;
  font-weight: 500;
  min-width: 50px;
}

.group-countdown {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #999;
}

.countdown-icon {
  font-size: 12px;
}

.join-btn {
  padding: 8px 16px;
  background: linear-gradient(90deg, #ff5000, #ff7040);
  color: white;
  border: none;
  border-radius: 18px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.join-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.3);
}

.empty-group {
  text-align: center;
  padding: 30px 20px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-text {
  font-size: 15px;
  color: #333;
  margin-bottom: 8px;
}

.empty-hint {
  font-size: 13px;
  color: #999;
}

/* ==================== 商品详情卡片 ==================== */
.detail-card {
  background: white;
  margin: 10px;
  border-radius: 12px;
  padding: 15px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.detail-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 15px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f5f5f5;
}

.detail-icon {
  font-size: 18px;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.spec-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.spec-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #fafafa;
  border-radius: 6px;
}

.spec-label {
  font-size: 13px;
  color: #666;
}

.spec-value {
  font-size: 13px;
color: #333;
  font-weight: 500;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notice-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f5fff5;
  border-radius: 6px;
}

.notice-icon {
  color: #67c23a;
  font-size: 14px;
}

.notice-item span:last-child {
  font-size: 13px;
  color: #333;
}

/* ==================== 底部操作栏 ==================== */
.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  padding: 10px 15px;
  display: flex;
  gap: 10px;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.08);
  z-index: 100;
}

.action-btn {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 22px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover {
  transform: translateY(-2px);
}

.buy-alone {
  background: #fff3eb;
  color: #ff5000;
}

.buy-alone:hover {
  background: #ffe8d9;
}

.group-buy {
  background: linear-gradient(90deg, #ff5000, #ff7040);
  color: white;
}

.group-buy:hover {
  background: linear-gradient(90deg, #ff4000, #ff6030);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.3);
}

.btn-label {
  font-size: 14px;
  font-weight: 500;
}

.btn-price {
  font-size: 16px;
  font-weight: 700;
}

/* ==================== 支付弹窗 ==================== */
.payment-overlay {
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
}

.payment-dialog {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 400px;
  overflow: hidden;
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 20px;
  border-bottom: 1px solid #f5f5f5;
}

.dialog-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.close-btn {
  width: 30px;
  height: 30px;
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

.close-btn:hover {
  background: #e8e8e8;
}

.dialog-body {
  padding: 20px;
  text-align: center;
}

.payment-amount {
  font-size: 16px;
  color: #333;
  margin-bottom: 20px;
}

.payment-amount span {
  font-size: 24px;
  font-weight: 700;
  color: #ff5000;
}

.qr-code-placeholder {
  width: 200px;
  height: 200px;
  margin: 0 auto 20px;
  border: 2px dashed #ddd;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fafafa;
}

.qr-icon {
  font-size: 48px;
  margin-bottom: 10px;
}

.qr-code-placeholder p {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.payment-tips {
  font-size: 13px;
  color: #999;
}

.dialog-footer {
  display: flex;
  gap: 10px;
  padding: 15px 20px;
  border-top: 1px solid #f5f5f5;
}

.cancel-btn, .confirm-btn {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 22px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn {
  background: #f5f5f5;
  color: #666;
}

.cancel-btn:hover {
  background: #e8e8e8;
}

.confirm-btn {
  background: linear-gradient(90deg, #ff5000, #ff7040);
  color: white;
}

.confirm-btn:hover {
  background: linear-gradient(90deg, #ff4000, #ff6030);
}

/* ==================== 响应式适配 ==================== */
@media (max-width: 480px) {
  .gallery-image {
    height: 280px;
  }

  .price-integer {
    font-size: 28px;
  }

  .action-btn {
n: 10px;
  }

  .btn-label {
    font-size: 13px;
  }

  .btn-price {
    font-size: 15px;
  }
}
</style>
