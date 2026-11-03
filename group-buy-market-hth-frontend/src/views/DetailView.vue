<!--
  DetailView.vue - 商品详情页（重构版）
  ======================================
  【重构说明】按照 docs/ui/html/index.html 参考设计重构
  - 顶部图片轮播（el-carousel）
  - 商品信息区（标题、促销标签、价格）
  - 拼团列表区（显示可参与的团）
  - 底部操作栏（单独购买/开团购买按钮，固定底部）
  - 支付弹窗（扫码支付）

  【前端概念】Vue 单文件组件 = 模板 + 逻辑 + 样式
  【类比后端】
    - <template>  → JSP/Thymeleaf 页面模板
    - <script>    → Controller 业务方法
    - <style>     → CSS 样式文件
-->

<template>
  <div class="detail-page">
    <!-- ==================== 顶部图片轮播 ==================== -->
    <!--
      【Element Plus 组件】el-carousel 轮播图
      - height: 轮播图高度
      - indicator-position: 指示器位置
      - autoplay: 自动播放
      - interval: 播放间隔（毫秒）
      【类比后端】相当于前端的多图展示组件，类似电商网站的商品图轮播
    -->
    <el-carousel height="375px" :interval="4000" indicator-position="outside">
      <el-carousel-item v-for="(img, index) in productImages" :key="index">
        <img :src="img" class="carousel-image" />
      </el-carousel-item>
    </el-carousel>

    <!-- ==================== 商品信息区 ==================== -->
    <!--
      【前端概念】商品信息展示区
      - 商品标题
      - 促销标签（大促优惠）
      - 促销描述（直降金额、抢购人数）
      【类比后端】相当于展示 GoodsMarketResponseDTO 中的商品信息
    -->
    <div class="product-info">
      <h1 class="product-title">{{ goodsInfo.goodsName }}</h1>
      <div class="promotion-area">
        <span class="promotion-tag">大促优惠</span>
        <span class="promotion-text">
          直降 ¥{{ goodsInfo.deductionPrice.toFixed(2) }}，
          {{ teamStatistic.allTeamUserCount }}人正在抢

        </span>
      </div>
      <div class="price-area">
        <span class="original-price">原价 ¥{{ goodsInfo.originalPrice.toFixed(2) }}</span>
        <span class="pay-price">¥{{ goodsInfo.payPrice.toFixed(2) }}</span>
      </div>
    </div>

    <!-- ==================== 拼团列表区 ==================== -->
    <!--
      【前端概念】显示可参与的拼团列表
      - v-for 遍历 teamList
      - 每个团显示：团长、剩余人数、倒计时、参团按钮
      【类比后端】相当于 <c:forEach items="${teamList}" var="team"> 在 JSTL 中
    -->
    <div class="group-list" v-if="teamList.length > 0">
      <div class="group-list-title">正在拼团</div>
      <div
        v-for="team in teamList"
        :key="team.teamId"
        class="group-item"
      >
        <div class="group-info">
          <div class="user-info">{{ team.userId }}</div>
          <div class="group-status">
            <span>组队仅剩{{ team.targetCount - team.completeCount }}人，拼单即将结束</span>
            <span class="countdown">{{ team.validTimeCountdown }}</span>
          </div>
        </div>
        <div class="group-action">
          <button
            class="group-btn"
            @click="handleJoinTeam(team)"
          >
            参与拼团
          </button>
        </div>
      </div>
    </div>

    <!-- 空状态提示 -->
    <div class="empty-group" v-else>
      <p>暂无进行中的拼团，快来开团吧！</p>
    </div>

    <!-- ==================== 空白区域（占位） ==================== -->
    <div class="area"></div>

    <!-- ==================== 底部操作栏 ==================== -->
    <!--
      【前端概念】固定底部的操作栏
      - position: fixed 固定在底部
      - 两个按钮：单独购买（原价）、开团购买（拼团价）
      【类比后端】相当于电商网站的底部购买栏
    -->
    <div class="action-bar">
      <button
        class="action-btn buy-alone"
        @click="handleBuyAlone"
      >
        单独购买(¥{{ goodsInfo.originalPrice.toFixed(2) }})
      </button>
      <button
        class="action-btn group-buy"
        @click="handleCreateTeam"
      >
        开团购买(¥{{ goodsInfo.payPrice.toFixed(2) }})
      </button>
    </div>

    <!-- ==================== 支付弹窗 ==================== -->
    <!--
      【Element Plus 组件】el-dialog 对话框
      - v-model 控制显示/隐藏
      - title 标题
      - width 宽度
      【类比后端】相当于模态框组件，用于展示支付二维码
    -->
    <el-dialog
      v-model="paymentDialogVisible"
      title="请扫码支付"
      width="400px"
      :close-on-click-modal="false"
    >
      <div class="payment-content">
        <p class="payment-amount">支付金额：¥{{ currentPayPrice.toFixed(2) }}</p>
        <div class="qr-code-placeholder">
          <span class="qr-icon">📱</span>
          <p>扫码支付区域</p>
        </div>
        <div class="payment-tips">
          <p>请使用微信或支付宝扫码支付</p>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancelPayment">取消支付</el-button>
          <el-button type="primary" @click="completePayment">支付完成</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * DetailView 业务逻辑（重构版）
 * ==============================
 * 【类比后端】相当于 DetailController 类
 * - 加载商品详情数据
 * - 处理参团/开团逻辑
 * - 处理支付逻辑
 */

import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { queryGroupBuyMarketConfig } from '../api/index'
import { lockMarketPayOrder } from '../api/trade'
import type {
  GoodsMarketResponseDTO,
  TeamInfo,
  LockMarketPayOrderRequestDTO,
} from '../api/types'
import { useUserStore } from '../stores/user'

// 路由和 Store 实例
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ==================== 响应式数据 ====================

// 商品图片列表（轮播图）- 使用占位图
const productImages = ref([
  'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="375"%3E%3Crect fill="%23ff5000" width="400" height="375"/%3E%3Ctext fill="white" font-size="24" x="50%" y="50%" text-anchor="middle" dy=".3em"%3E商品图片 1%3C/text%3E%3C/svg%3E',
  'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="375"%3E%3Crect fill="%23ff6a1a" width="400" height="375"/%3E%3Ctext fill="white" font-size="24" x="50%" y="50%" text-anchor="middle" dy=".3em"%3E商品图片 2%3C/text%3E%3C/svg%3E',
  'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="375"%3E%3Crect fill="%23ff8000" width="400" height="375"/%3E%3Ctext fill="white" font-size="24" x="50%" y="50%" text-anchor="middle" dy=".3em"%3E商品图片 3%3C/text%3E%3C/svg%3E',
])

// 商品信息
const goodsInfo = ref({
  goodsId: '',
  goodsName: '拼团商品',
  originalPrice: 0,
  deductionPrice: 0,
  payPrice: 0,
})

// 拼团列表
const teamList = ref<TeamInfo[]>([])

// 拼团统计
const teamStatistic = ref({
  allTeamCount: 0,
  allTeamCompleteCount: 0,
  allTeamUserCount: 0,
})

// 活动ID
const activityId = ref<number>(0)

// 支付弹窗显示状态
const paymentDialogVisible = ref(false)

// 当前支付金额
const currentPayPrice = ref(0)

// 当前选择的团（参团时有值）
const currentTeam = ref<TeamInfo | null>(null)

// ==================== 生命周期 ====================

onMounted(() => {
  // 从路由参数获取 activityId 和 goodsId
  const query = route.query
  activityId.value = Number(query.activityId) || 0
  goodsInfo.value.goodsId = (query.goodsId as string) || ''
  goodsInfo.value.goodsName = (query.goodsName as string) || '拼团商品'
  goodsInfo.value.originalPrice = Number(query.originalPrice) || 0

  // 参数校验
  if (!activityId.value || !goodsInfo.value.goodsId) {
    ElMessage.error('商品参数缺失，请从首页重新选择')
    router.push({ name: 'home' })
    return
  }

  // 加载商品详情数据
  loadData()
})

// ==================== 数据加载 ====================

/**
 * 从后端加载商品详情数据
 * 【类比后端】相当于 @GetMapping("/detail") 查询商品配置和拼团列表
 */
async function loadData() {
  try {
    const response: GoodsMarketResponseDTO = await queryGroupBuyMarketConfig({
      userId: userStore.username || 'guest',
      source: 's01',
      channel: 'c01',
      goodsId: goodsInfo.value.goodsId,
    })

    // 更新商品信息
    goodsInfo.value = {
      goodsId: response.goods.goodsId,
      goodsName: goodsInfo.value.goodsName, // 保持从路由参数获取的名称
      originalPrice: response.goods.originalPrice,
      deductionPrice: response.goods.deductionPrice,
      payPrice: response.goods.payPrice,
    }

    // 更新拼团列表
    teamList.value = response.teamList || []

    // 更新拼团统计
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

// ==================== 事件处理 ====================

/**
 * 处理参团
 * 【类比后端】相当于点击"参与拼团"按钮，准备锁单
 */
function handleJoinTeam(team: TeamInfo) {
  currentTeam.value = team
  currentPayPrice.value = goodsInfo.value.payPrice
  // 直接发起锁单请求
  lockOrder(team.teamId)
}

/**
 * 处理单独购买
 * 【类比后端】相当于以原价购买，不开团
 */
function handleBuyAlone() {
  currentPayPrice.value = goodsInfo.value.originalPrice
  currentTeam.value = null
  // 显示支付弹窗（单独购买走普通购买流程）
  paymentDialogVisible.value = true
}

/**
 * 处理开团购买
 * 【类比后端】相当于创建新团并以拼团价购买
 */
function handleCreateTeam() {
  currentPayPrice.value = goodsInfo.value.payPrice
  currentTeam.value = null
  // 发起锁单请求（teamId 为空表示开新团）
  lockOrder('')
}

/**
 * 发起锁单请求
 * 【类比后端】相当于调用 tradeService.lockMarketPayOrder()
 */
async function lockOrder(teamId: string) {
  try {
    const request: LockMarketPayOrderRequestDTO = {
      userId: userStore.username,
      teamId: teamId,
      activityId: activityId.value,
      goodsId: goodsInfo.value.goodsId,
      source: 's01',
      channel: 'c01',
      outTradeNo: generateOutTradeNo(),
      notifyConfigVO: {
        notifyType: 'HTTP',
        notifyUrl: 'http://localhost:8091/api/v1/test/group_buy_notify',
      },
    }

    const result = await lockMarketPayOrder(request)

    ElMessage.success('锁单成功！')

    // 跳转到支付页
    router.push({
      name: 'pay',
      query: {
        orderId: result.orderId,
        teamId: result.teamId,
        payPrice: result.payPrice.toString(),
        originalPrice: result.originalPrice.toString(),
        deductionPrice: result.deductionPrice.toString(),
        outTradeNo: request.outTradeNo,
        goodsId: goodsInfo.value.goodsId,
        goodsName: goodsInfo.value.goodsName,
      },
    })
  } catch (error) {
    console.error('锁单失败:', error)
    ElMessage.error('锁单失败，请重试')
  }
}

/**
 * 生成外部交易单号
 * 【类比后端】相当于 UUID 或 SnowFlake 生成订单号
 */
function generateOutTradeNo(): string {
  const timestamp = Date.now().toString()
  const random = Math.floor(Math.random() * 10000).toString().padStart(4, '0')
  return (timestamp + random).slice(0, 12)
}

/**
 * 取消支付
 */
function cancelPayment() {
  paymentDialogVisible.value = false
  currentTeam.value = null
}

/**
 * 完成支付
 */
function completePayment() {
  ElMessage.success('支付成功！')
  paymentDialogVisible.value = false
  // 跳转到订单页
  router.push({ name: 'orders' })
}
</script>

<style scoped>
/**
 * DetailView 样式（按照 index.css 重构）
 * 【前端概念】scoped 表示样式只对当前组件生效
 */

/* 页面容器 */
.detail-page {
  background: #f5f5f5;
  min-height: 100vh;
  padding-bottom: 70px; /* 为底部操作栏留出空间 */
}

/* ==================== 轮播图样式 ==================== */
.carousel-image {
  width: 100%;
  height: 375px;
  object-fit: cover;
}

/* ==================== 商品信息样式 ==================== */
.product-info {
  padding: 15px;
  background: white;
  margin-bottom: 10px;
}

.product-title {
  font-size: 18px;
  color: #333;
  line-height: 1.4;
  margin: 0 0 8px 0;
}

.promotion-area {
  margin-bottom: 8px;
}

.promotion-tag {
  display: inline-block;
  padding: 2px 6px;
  background: linear-gradient(90deg, #ff5000, #ff8000);
  color: white;
  border-radius: 2px;
  font-size: 12px;
  margin-right: 8px;
}

.promotion-text {
  font-size: 14px;
  color: #ff5000;
}

.price-area {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.original-price {
  font-size: 14px;
  color: #999;
  text-decoration: line-through;
}

.pay-price {
  font-size: 24px;
  color: #ff5000;
  font-weight: bold;
}

/* ==================== 拼团列表样式 ==================== */
.group-list {
  margin: 15px;
  background: white;
  border-radius: 8px;
  padding: 10px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.group-list-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.group-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
}

.group-item:last-child {
  border-bottom: none;
}

.group-info {
  flex: 1;
}

.user-info {
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
}

.group-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #999;
}

.countdown {
  color: #ff5000;
  font-weight: bold;
}

.group-btn {
  background: #ff5000;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 15px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.group-btn:hover {
  background: #ff6a1a;
  transform: scale(1.05);
}

/* 空状态 */
.empty-group {
  padding: 40px 20px;
  text-align: center;
  color: #999;
  font-size: 14px;
}

/* 占位区域 */
.area {
  padding: 30px;
  border-top: 1px solid #eee;
}

/* ==================== 底部操作栏样式 ==================== */
.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  padding: 10px 15px;
  display: flex;
  gap: 10px;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.1);
  z-index: 100;
}

.action-btn {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.buy-alone {
  background: #ffece5;
  color: #ff5000;
}

.buy-alone:hover {
  background: #ffd9c5;
}

.group-buy {
  background: linear-gradient(90deg, #ff5000, #ff8000);
  color: white;
}

.group-buy:hover {
  background: linear-gradient(90deg, #ff6a1a, #ff9a1a);
}

/* ==================== 支付弹窗样式 ==================== */
.payment-content {
  text-align: center;
  padding: 20px 0;
}

.payment-amount {
  font-size: 18px;
  color: #333;
  margin-bottom: 20px;
}

.qr-code-placeholder {
  width: 200px;
  height: 200px;
  margin: 20px auto;
  border: 2px dashed #ddd;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f9f9f9;
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
  margin-top: 20px;
  font-size: 14px;
  color: #999;
}

.dialog-footer {
  display: flex;
  justify-content: space-around;
  gap: 10px;
}
</style>
