<!--
  DetailView.vue - 商品详情 + 锁单页面
  ======================================
  【页面职责】
    用户从首页点击"开团购买"或"参与拼团"后跳转到此页面。
    展示商品详情、拼团信息，并允许用户确认锁单。

  【类比后端】
    相当于后端的 detail.html 页面 + 锁单 Controller：
    - 展示商品信息 → 类比 @GetMapping("/detail/{goodsId}") 返回商品详情视图
    - 确认锁单 → 类比 @PostMapping("/trade/lock") 提交锁单请求

  【数据流转】
    首页 ──(router.push + query参数)──→ DetailView ──(lockMarketPayOrder API)──→ 后端
                                              │
                                              ↓ (锁单成功)
                                         PayView (支付页)

  【Vue 组件结构说明】
    每个 .vue 文件由三部分组成：
    1. <template>  → HTML 模板（类比 Thymeleaf/JSP 页面）
    2. <script>    → JS/TS 逻辑（类比 Controller 中的方法）
    3. <style>     → CSS 样式（控制页面外观）

    <script setup> 是 Vue 3 的语法糖，等同于在 setup() 函数中编写逻辑
    lang="ts" 表示使用 TypeScript（类比 Java 的强类型检查）
-->

<template>
  <!-- 页面最外层容器 -->
  <div class="detail-page">
    <!-- ==================== 顶部导航栏 ==================== -->
    <!--
      【前端概念】flex 布局 = 弹性盒子，类比后端的流式布局但更强大
      这里用 flex 实现"返回按钮在左，标题居中"的效果
    -->
    <header class="page-header">
      <el-button
        text
        @click="goBack"
        class="back-btn"
      >
        <!--
          【Element Plus 组件】el-button
          - text 属性：文字按钮样式（没有背景色，类比链接按钮）
          - @click：点击事件绑定（类比 <button onclick="goBack()">）
          - el-icon：图标组件，包裹图标字体
        -->
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <h2 class="page-title">商品详情</h2>
      <!-- 右侧占位，保持标题居中（flex 布局技巧） -->
      <div class="header-placeholder"></div>
    </header>

    <!-- ==================== 商品详情卡片 ==================== -->
    <!--
      【Element Plus 组件】el-card 卡片
      - 类似 Bootstrap 的 card，用于包裹一块内容
      - shadow="hover"：鼠标悬停时显示阴影（交互效果）
      - 类比后端：相当于一个 <div class="panel"> 的 UI 容器
    -->
    <el-card shadow="hover" class="section-card">
      <!--
        【前端概念】模板语法 {{ }}
        - {{ goodsInfo.goodsId }} 会显示 goodsInfo.goodsId 变量的值
        - 类比 Thymeleaf 的 ${goods.goodsId} 或 JSP 的 ${goods.goodsId}
        - 变量在 <script setup> 中通过 ref/reactive 定义
      -->
      <template #header>
        <!--
          【Element Plus 组件】el-card 的具名插槽 #header
          - 插槽（slot）= 父组件向子组件传递内容的"口子"
          - #header 是具名插槽，类比 Thymeleaf 的 layout:fragment
          - 这里把自定义内容插入到卡片的头部区域
        -->
        <div class="card-header">
          <span class="card-title">🛍️ 商品信息</span>
          <!--
            【Element Plus 组件】el-tag 标签
            - type="primary"：蓝色主题标签
            - size="small"：小尺寸
            - 类比后端：相当于商品的状态标签，如"热销"、"新品"
          -->
          <el-tag type="primary" size="small">拼团特惠</el-tag>
        </div>
      </template>

      <!--
        【Element Plus 组件】el-descriptions 描述列表
        - 用于展示键值对信息（类比 Java 的 toString() 输出）
        - :column="1" 表示每行显示 1 列（绑定 prop 用冒号 :）
        - border：显示边框
        - 类比后端：相当于 DTO 的字段展示，一行一个属性
      -->
      <el-descriptions :column="1" border>
        <!--
          【el-descriptions-item】描述列表的每一行
          - label：标签名（类比 DTO 字段的注释）
          - 标签和标签之间的内容就是显示的值
        -->
        <el-descriptions-item label="商品ID">
          {{ goodsInfo.goodsId }}
        </el-descriptions-item>
        <el-descriptions-item label="商品名称">
          {{ goodsInfo.goodsName }}
        </el-descriptions-item>
        <el-descriptions-item label="原价">
          <!--
            【前端概念】金额格式化
            - .toFixed(2) 保留两位小数（类比 Java 的 String.format("%.2f", price)）
            - ¥ 符号直接写在模板中
          -->
          <span class="original-price">¥{{ goodsInfo.originalPrice?.toFixed(2) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="优惠减免">
          <span class="discount-price">-¥{{ goodsInfo.deductionPrice?.toFixed(2) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="拼团价">
          <!--
            【前端概念】条件渲染 v-if
            - v-if="goodsInfo.payPrice" 只有当 payPrice 有值时才渲染这个元素
            - 类比后端：if (dto.getPayPrice() != null) { 显示价格 }
            - 大字号 + 红色突出显示
          -->
          <span v-if="goodsInfo.payPrice" class="pay-price">
            ¥{{ goodsInfo.payPrice?.toFixed(2) }}
          </span>
          <span v-else class="pay-price">--</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- ==================== 拼团信息卡片 ==================== -->
    <!--
      【前端概念】v-if / v-else 条件分支
      - 根据 isJoinTeam 的值显示不同的内容
      - 类比后端的 if-else 分支：
        if (isJoinTeam) { 显示参团信息 } else { 显示开团信息 }
    -->
    <el-card shadow="hover" class="section-card" v-if="isJoinTeam">
      <template #header>
        <div class="card-header">
          <span class="card-title">👥 拼团信息（参团）</span>
          <el-tag type="success" size="small">进行中</el-tag>
        </div>
      </template>

      <el-descriptions :column="1" border>
        <el-descriptions-item label="团长">
          {{ teamInfo.userId }}
        </el-descriptions-item>
        <el-descriptions-item label="拼团ID">
          {{ teamInfo.teamId }}
        </el-descriptions-item>
        <el-descriptions-item label="成团目标">
          {{ teamInfo.targetCount }} 人
        </el-descriptions-item>
        <el-descriptions-item label="已参团">
          {{ teamInfo.completeCount }} 人
        </el-descriptions-item>
        <el-descriptions-item label="还差">
          <!--
            【前端概念】计算表达式
            - 直接在 {{ }} 中做数学运算
            - 类比后端：targetCount - completeCount
            - 用 el-tag 高亮显示剩余名额
          -->
          <el-tag type="warning" effect="dark">
            {{ teamInfo.targetCount - teamInfo.completeCount }} 人
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="有效期">
          {{ teamInfo.validTimeCountdown }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 开团时的提示卡片（v-else：上面 v-if 不成立时显示这个） -->
    <el-card shadow="hover" class="section-card" v-else>
      <template #header>
        <div class="card-header">
          <span class="card-title">🚀 开新团</span>
          <el-tag type="warning" size="small">新团</el-tag>
        </div>
      </template>
      <!--
        【前端概念】静态文本 + 样式
        这里没有动态数据，只是展示固定文案
      -->
      <div class="new-team-tip">
        <p>🎉 你将创建一个新的拼团</p>
        <p>邀请好友一起参团，享受更优惠的价格！</p>
      </div>
    </el-card>

    <!-- ==================== 订单确认卡片 ==================== -->
    <el-card shadow="hover" class="section-card order-confirm-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">📋 订单确认</span>
        </div>
      </template>

      <el-descriptions :column="1" border>
        <el-descriptions-item label="支付金额">
          <!--
            【前端概念】大字号金额展示
            通过 CSS class 控制字体大小和颜色，突出显示
          -->
          <span class="confirm-price">¥{{ goodsInfo.payPrice?.toFixed(2) || '0.00' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="交易单号">
          <!--
            【前端概念】计算属性 / 方法调用
            - outTradeNo 在组件挂载时就已生成（见 <script setup> 中的 generateOutTradeNo）
            - 类比后端：在 Controller 方法开始时生成一个唯一单号
          -->
          {{ outTradeNo }}
        </el-descriptions-item>
        <el-descriptions-item label="来源">
          {{ source }}
        </el-descriptions-item>
        <el-descriptions-item label="渠道">
          {{ channel }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- ==================== 锁单按钮区域 ==================== -->
    <!--
      【前端概念】事件绑定 + 按钮状态控制
      - @click="handleLockOrder"：点击时调用 handleLockOrder 方法
      - :disabled="isLocking"：动态绑定 disabled 属性
        冒号 : 表示绑定的是 JS 表达式（不是字符串）
        类比后端：按钮防重复点击 = 后端的幂等性控制 / 分布式锁
        防止用户连续点击导致多次提交
    -->
    <div class="action-area">
      <el-button
        type="primary"
        size="large"
        class="lock-btn"
        :disabled="isLocking"
        :loading="isLocking"
        @click="handleLockOrder"
      >
        <!--
          【Element Plus 组件】el-button 的 loading 状态
          - :loading="isLocking"：当 isLocking 为 true 时，按钮显示加载动画
          - 类比后端：处理中... 的等待状态
          - :disabled="isLocking"：加载时禁用按钮，防止重复点击
        -->
        {{ isLocking ? '锁单中...' : '确认锁单' }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * DetailView 的业务逻辑
 * ====================
 *
 * 【类比后端】
 * 这个 <script setup> 块相当于一个 Controller 类：
 * - 接收参数（路由 query → 类比 @RequestParam）
 * - 定义状态变量（ref → 类比成员变量）
 * - 定义方法（handleLockOrder → 类比 @PostMapping 方法）
 *
 * 【Vue 3 Composition API 核心概念】
 * - ref()：创建响应式变量，值变化时页面自动更新
 *   类比：private String name; 但带"自动通知UI刷新"功能
 * - computed()：计算属性，依赖其他变量自动计算
 *   类比：public String getFullName() { return firstName + lastName; }
 * - onMounted()：组件挂载完成后执行（页面加载完成）
 *   类比：@PostConstruct 初始化方法
 */

// ==================== 导入依赖 ====================

/**
 * 【前端概念】import 导入
 * - 从 'vue' 导入 Vue 3 的核心 API
 * - 从 'vue-router' 导入路由相关 API
 * - 类比后端：import javax.servlet.http.HttpServletRequest;
 *
 * 导入的每个函数/对象的用途：
 * - ref：创建响应式变量（值变了 UI 自动刷新）
 * - computed：创建计算属性（自动根据依赖重新计算）
 * - onMounted：生命周期钩子（组件加载完成后执行）
 * - useRoute：获取当前路由信息（类比 @PathVariable / @RequestParam）
 * - useRouter：编程式导航（类比 response.sendRedirect()）
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'

/**
 * 导入 API 函数和类型
 * - lockMarketPayOrder：锁单 API（类比 @Autowired IMarketTradeService）
 * - type { ... }：只导入类型，不会打包到最终代码（类比 Java 的 import 但只用于编译检查）
 */
import { lockMarketPayOrder } from '../api/trade'
import type {
  LockMarketPayOrderRequestDTO,
  LockMarketPayOrderResponseDTO,
} from '../api/types'

/**
 * 导入用户 Store
 * 【类比后端】相当于 @Autowired UserService 获取当前登录用户信息
 */
import { useUserStore } from '../stores/user'

// ==================== 路由相关 ====================

/**
 * 【前端概念】useRoute() - 获取当前路由信息
 * 【类比后端】
 *   - 相当于 HttpServletRequest 获取请求参数
 *   - route.query → 类比 request.getParameterMap()（URL ? 后面的参数）
 *   - route.params → 类比 @PathVariable（URL 路径中的参数）
 *
 * 举例：
 *   URL: /detail?goodsId=123&teamId=abc
 *   route.query.goodsId → '123'
 *   route.query.teamId  → 'abc'
 */
const route = useRoute()

/**
 * 【前端概念】useRouter() - 路由导航（跳转页面）
 * 【类比后端】
 *   - router.push() → 类比 response.sendRedirect("/pay")
 *   - router.replace() → 类比 forward（不产生新的历史记录）
 *   - router.back() → 类比 request.getHeader("Referer") 然后跳转回去
 */
const router = useRouter()

/**
 * 获取用户 Store 实例
 * 【类比后端】相当于从 SecurityContext / Session 中获取当前用户
 */
const userStore = useUserStore()

// ==================== 响应式状态（数据） ====================

/**
 * 【前端概念】ref() 响应式变量详解
 *
 * ref() 把普通值包装成"响应式"的：
 * - 在 JS 中读写需要 .value：goodsInfo.value.goodsId
 * - 在 <template> 中自动解包，不需要 .value：{{ goodsInfo.goodsId }}
 *
 * 类比后端：
 *   - ref('') → private String name = "";  （但会自动通知 UI 更新）
 *   - ref(0)   → private int count = 0;
 *   - ref(null) → private Object data = null;
 *
 * 为什么用 ref 而不是普通变量？
 * - 普通变量：let count = 0; count++; → 页面不会更新
 * - ref 变量：const count = ref(0); count.value++; → 页面自动刷新
 * - 这就是"响应式"的含义：数据驱动视图
 */

/**
 * 商品信息（从路由参数解析）
 * 使用 ref 包装，初始值是一个空对象（类型断言 as 告诉 TS "相信我，这就是这个类型"）
 */
const goodsInfo = ref({
  goodsId: '',
  goodsName: '拼团商品',
  originalPrice: 0,
  deductionPrice: 0,
  payPrice: 0,
})

/**
 * 拼团信息（参团时有值，开团时为空对象）
 */
const teamInfo = ref({
  teamId: '',
  userId: '',
  activityId: 0,
  targetCount: 0,
  completeCount: 0,
  lockCount: 0,
  validStartTime: '',
  validEndTime: '',
  validTimeCountdown: '',
  outTradeNo: '',
})

/**
 * 外部交易单号 - 随机生成
 * 【类比后端】相当于订单号生成器
 * - 后端：UUID / SnowFlake / 时间戳+随机数
 * - 前端：Date.now() + Math.random() 拼接
 */
const outTradeNo = ref('')

/**
 * 来源 & 渠道
 * 固定值，类比后端配置常量
 */
const source = ref('s01')
const channel = ref('c01')

/**
 * 锁单加载状态
 * 【类比后端】相当于 boolean isProcessing = false;
 * - true：正在处理中，按钮显示 loading，禁止再次点击
 * - false：空闲状态，可以正常点击
 *
 * 这就是前端的"幂等性控制"，防止重复提交
 * 后端的幂等性通常用 Redis + Token 或分布式锁实现
 * 前端则简单得多：一个 boolean 变量控制按钮 disabled 状态
 */
const isLocking = ref(false)

// ==================== 计算属性 ====================

/**
 * 【前端概念】computed() 计算属性
 * - 基于其他响应式数据自动计算
 * - 有缓存：依赖不变就不会重新计算（类比后端的 @Cacheable）
 * - 只读：不能在外部直接赋值
 *
 * 类比后端：
 *   public boolean isJoinTeam() {
 *       return teamId != null && !teamId.isEmpty();
 *   }
 */
const isJoinTeam = computed(() => {
  return !!teamInfo.value.teamId && teamInfo.value.teamId !== ''
})

// ==================== 生命周期 ====================

/**
 * 【前端概念】onMounted() 生命周期钩子
 * 【类比后端】
 *   - 相当于 @PostConstruct 方法，或 Servlet 的 init()
 *   - 组件"挂载"完成后执行（页面 DOM 已渲染完毕）
 *   - 通常在这里：发 API 请求、初始化数据、绑定事件
 *
 * Vue 组件生命周期简版：
 *   setup() → 组件创建（定义变量、方法）
 *   onMounted() → 页面加载完成（可以操作 DOM、发请求）
 *   onUpdated() → 数据变化导致页面重新渲染
 *   onUnmounted() → 组件销毁（清理定时器、解绑事件）
 */
onMounted(() => {
  /**
   * 【前端概念】路由参数获取
   * route.query 是 URL 中 ? 后面的键值对
   *
   * 举例：/detail?goodsId=g001&teamId=t001&goodsName=苹果
   * route.query.goodsId   → 'g001'
   * route.query.teamId    → 't001'
   * route.query.goodsName → '苹果'
   *
   * 【类比后端】
   *   - route.query.xxx → request.getParameter("xxx")  / @RequestParam("xxx")
   *   - route.params.xxx → @PathVariable("xxx")
   *
   * 注意：route.query 取到的值都是 string 类型（URL 参数都是字符串）
   * 如果后端需要数字，要手动转换：Number(route.query.activityId)
   */
  const query = route.query

  // 解析商品信息（从 query 参数中获取，提供默认值防止 undefined）
  // 【前端概念】|| 运算符提供默认值
  //   query.goodsName || '拼团商品'
  //   如果 query.goodsName 是 undefined/空字符串，则使用 '拼团商品'
  //   类比后端：Optional.ofNullable(name).orElse("拼团商品")
  goodsInfo.value = {
    goodsId: (query.goodsId as string) || '',
    goodsName: (query.goodsName as string) || '拼团商品',
    originalPrice: Number(query.originalPrice) || 0,
    deductionPrice: Number(query.deductionPrice) || 0,
    payPrice: Number(query.payPrice) || 0,
  }

  // 解析拼团信息（参团时才有 teamId）
  teamInfo.value = {
    teamId: (query.teamId as string) || '',
    userId: (query.teamUserId as string) || '',
    activityId: Number(query.activityId) || 0,
    targetCount: Number(query.targetCount) || 0,
    completeCount: Number(query.completeCount) || 0,
    lockCount: Number(query.lockCount) || 0,
    validStartTime: (query.validStartTime as string) || '',
    validEndTime: (query.validEndTime as string) || '',
    validTimeCountdown: (query.validTimeCountdown as string) || '',
    outTradeNo: (query.teamOutTradeNo as string) || '',
  }

  // 生成随机交易单号
  outTradeNo.value = generateOutTradeNo()
})

// ==================== 方法 ====================

/**
 * 生成随机外部交易单号
 *
 * 【类比后端】
 *   后端的订单号生成通常用：
 *   - UUID.randomUUID().toString().replace("-", "")
 *   - SnowFlake 雪花算法
 *   - 时间戳 + 机器ID + 序列号
 *
 *   前端简化版：时间戳（13位）+ 随机数（4位）→ 取前12位
 *
 * 【前端概念】
 * - Date.now()：返回当前时间戳（毫秒），如 1698765432100
 * - Math.random()：返回 0~1 之间的随机数，如 0.8765
 * - String.prototype.slice(start, end)：截取字符串
 * - 拼接用 + 号或模板字符串 `${a}${b}`
 *
 * @returns 12位数字字符串，如 "169876543218"
 */
function generateOutTradeNo(): string {
  const timestamp = Date.now().toString()          // 13位时间戳
  const random = Math.floor(Math.random() * 10000).toString().padStart(4, '0')  // 4位随机数
  // 拼接后取前12位（保证长度固定）
  return (timestamp + random).slice(0, 12)
}

/**
 * 返回上一页
 * 【类比后端】相当于 response.sendRedirect(request.getHeader("Referer"))
 *
 * 【前端概念】router.back()
 * - 浏览器历史回退（相当于点击浏览器的"后退"按钮）
 * - router.go(-1) 也可以，等价于 router.back()
 * - 如果历史记录为空，则不执行任何操作
 */
function goBack() {
  router.back()
}

/**
 * 处理锁单请求
 *
 * 【类比后端】
 * 相当于 Controller 中的 @PostMapping("/lock") 方法：
 *   1. 校验参数（前端校验 + 后端校验）
 *   2. 构建请求 DTO
 *   3. 调用 Service 层方法
 *   4. 处理响应（成功跳转 / 失败提示）
 *
 * 【前端概念】async/await 异步编程
 * - async 标记函数为异步函数（返回 Promise）
 * - await 等待 Promise 完成并获取结果
 * - 类比后端：
 *     // Java CompletableFuture
 *     CompletableFuture<LockResponse> future = tradeService.lockOrder(request);
 *     LockResponse response = future.get();  // 阻塞等待结果
 *
 *     // 前端 async/await（非阻塞等待）
 *     const response = await lockMarketPayOrder(request);
 *
 * - 区别：Java 的 future.get() 会阻塞线程，前端的 await 不会阻塞 UI
 *   前端是单线程事件循环模型，await 只是"暂停"当前函数的执行，
 *   让出控制权给事件循环，等 Promise 完成后再继续执行
 *
 * 【错误处理】
 * - try/catch 捕获异常（类比后端的 try-catch）
 * - catch 中显示错误提示（类比后端返回错误响应）
 */
async function handleLockOrder() {
  // ==================== 1. 防重复点击校验 ====================
  // 【类比后端】相当于 if (isProcessing) return; 幂等性检查
  if (isLocking.value) {
    ElMessage.warning('正在处理中，请勿重复提交')
    return
  }

  // ==================== 2. 设置加载状态 ====================
  isLocking.value = true

  try {
    // ==================== 3. 构建请求参数 ====================
    /**
     * 构建锁单请求 DTO
     * 【类比后端】相当于 new LockMarketPayOrderRequestDTO() 并 set 各字段
     *
     * 字段说明：
     * - userId：从 userStore 获取当前登录用户（类比 SecurityContextHolder.getContext().getAuthentication()）
     * - teamId：参团时有值，开团时为空字符串（后端根据此字段判断是开团还是参团）
     * - activityId：活动ID（关联到哪个拼团活动配置）
     * - goodsId：商品ID（购买哪个商品）
     * - source / channel：来源和渠道（用于数据统计和路由分发）
     * - outTradeNo：外部交易单号（幂等键，防止重复下单）
     * - notifyConfigVO：支付成功回调配置（告诉后端支付完成后通知哪里）
     */
    const requestParams: LockMarketPayOrderRequestDTO = {
      userId: userStore.username,
      teamId: teamInfo.value.teamId || '',  // 开团时为空，参团时有值
      activityId: teamInfo.value.activityId,
      goodsId: goodsInfo.value.goodsId,
      source: source.value,
      channel: channel.value,
      outTradeNo: outTradeNo.value,
      notifyConfigVO: {
        notifyType: 'HTTP',
        notifyUrl: 'http://localhost:8091/api/v1/test/group_buy_notify',
      },
    }

    // ==================== 4. 调用锁单 API ====================
    /**
     * 【前端概念】await 等待异步结果
     * - lockMarketPayOrder 返回 Promise<LockMarketPayOrderResponseDTO>
     * - await 会等待这个 Promise 完成（resolve），然后拿到结果
     * - 如果请求失败（reject），会跳到 catch 块
     *
     * 【类比后端】
     *   LockMarketPayOrderResponseDTO result = tradeService.lockMarketPayOrder(request);
     *   但前端是非阻塞的，等待期间 UI 仍然可以响应用户操作
     */
    const result: LockMarketPayOrderResponseDTO = await lockMarketPayOrder(requestParams)

    // ==================== 5. 锁单成功，跳转到支付页 ====================
    /**
     * 【前端概念】router.push() 路由跳转 + query 传参
     *
     * 跳转方式对比：
     * - router.push({ name: 'pay', query: { ... } })
     *   → URL: /pay?orderId=xxx&payPrice=99.00
     *   → 类比：response.sendRedirect("/pay?orderId=xxx")
     *   → 参数在 URL 中可见，刷新页面参数不丢失
     *
     * - router.push({ name: 'pay', params: { ... } })
     *   → 需要路由配置中定义 :orderId 等参数
     *   → 类比：@PathVariable 路径参数
     *   → 参数不在 URL 中显示（但刷新会丢失，除非配合 state）
     *
     * 这里使用 query 方式，因为支付页需要持久化这些参数
     */
    ElMessage.success('锁单成功！正在跳转到支付页面...')

    router.push({
      name: 'pay',
      query: {
        orderId: result.orderId,
        teamId: result.teamId,
        payPrice: result.payPrice.toString(),
        originalPrice: result.originalPrice.toString(),
        deductionPrice: result.deductionPrice.toString(),
        outTradeNo: outTradeNo.value,
        goodsId: goodsInfo.value.goodsId,
        goodsName: goodsInfo.value.goodsName,
      },
    })

  } catch (error: any) {
    /**
     * 【前端概念】try/catch 错误处理
     * - 类比后端的 try-catch-finally
     * - catch 捕获所有异常（网络错误、业务错误等）
     * - error.response?.data?.info 获取后端返回的错误信息
     *   （可选链 ?. 防止 null 报错，类比 Java 的 Optional）
     *
     * 【后端错误响应结构】
     * - error.response：Axios 自动附加的响应对象
     * - error.response.data：响应体（后端返回的 JSON）
     * - error.response.data.info：后端返回的错误描述
     */
    const errorMsg = error?.response?.data?.info || error?.message || '锁单失败，请稍后重试'
    ElMessage.error(errorMsg)
  } finally {
    /**
     * 【前端概念】finally 块
     * - 无论成功还是失败，都会执行
     * - 类比后端的 finally { lock.unlock(); }
     * - 这里用来重置加载状态，恢复按钮可点击
     */
    isLocking.value = false
  }
}
</script>

<style scoped>
/**
 * 【前端概念】<style scoped>
 * - scoped 表示样式只作用于当前组件（不会污染其他组件）
 * - 类比后端的命名空间 / 包隔离：com.hth.moduleA.Class 不影响 com.hth.moduleB.Class
 * - Vue 通过给 DOM 添加 data-v-xxx 属性实现样式隔离
 */

/* ==================== 页面整体布局 ==================== */
.detail-page {
  max-width: 680px;          /* 最大宽度，大屏时居中 */
  margin: 0 auto;            /* 水平居中（类比 margin: 0 auto;） */
  padding: 20px 16px 40px;   /* 上 左右 下 内边距 */
  min-height: 100vh;         /* 最小高度 = 视口高度（保证全屏） */
  background: linear-gradient(180deg, #f0f4ff 0%, #fafbff 100%);
  /* 渐变背景：从浅蓝到近白，营造层次感 */
}

/* ==================== 顶部导航栏 ==================== */
.page-header {
  display: flex;             /* Flex 弹性布局 */
  align-items: center;       /* 垂直居中 */
  justify-content: space-between; /* 两端对齐 */
  margin-bottom: 20px;
  padding: 8px 0;
}

.back-btn {
  font-size: 15px;
  color: #4a5568;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a202c;
  margin: 0;
}

.header-placeholder {
  width: 60px; /* 占位元素，保证标题居中 */
}

/* ==================== 卡片通用样式 ==================== */
.section-card {
  margin-bottom: 16px;
  border-radius: 12px;
  overflow: hidden;
}

/* Element Plus 卡片头部自定义样式 */
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

/* ==================== 价格样式 ==================== */
.original-price {
  text-decoration: line-through; /* 删除线（表示原价已优惠） */
  color: #a0aec0;
  font-size: 14px;
}

.discount-price {
  color: #38a169;               /* 绿色表示优惠 */
  font-weight: 500;
}

.pay-price {
  font-size: 22px;
  font-weight: 700;
  color: #e53e3e;               /* 红色突出实付价格 */
}

/* ==================== 订单确认区域 ==================== */
.confirm-price {
  font-size: 28px;
  font-weight: 800;
  color: #c53030;
  letter-spacing: 1px;
}

.new-team-tip {
  text-align: center;
  padding: 20px 0;
}

.new-team-tip p {
  margin: 8px 0;
  font-size: 15px;
  color: #4a5568;
}

.new-team-tip p:first-child {
  font-size: 18px;
  font-weight: 600;
  color: #2d3748;
}

/* ==================== 操作按钮区域 ==================== */
.action-area {
  margin-top: 24px;
  padding: 0 4px;
}

.lock-btn {
  width: 100%;
  height: 52px;
  font-size: 17px;
  font-weight: 600;
  border-radius: 12px;
  letter-spacing: 2px;
  /* 渐变背景按钮 */
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s ease;
}

.lock-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.lock-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ==================== Element Plus 组件样式覆盖 ==================== */
/*
  【前端概念】:deep() 深度选择器
  - scoped 样式默认不影响子组件内部元素
  - :deep(.el-xxx) 可以穿透 scoped 限制，修改子组件样式
  - 类比后端：继承 + 覆盖（Override），修改第三方组件的默认样式
*/
.detail-page :deep(.el-descriptions__label) {
  font-weight: 500;
  color: #718096;
  width: 100px;
}

.detail-page :deep(.el-descriptions__content) {
  color: #2d3748;
  font-weight: 500;
}
</style>
