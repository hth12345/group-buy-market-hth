<!--
  HomeView.vue - 拼团商城首页
  ============================
  【前端概念】这是登录后的主页面，展示拼团商品信息和进行中的拼团列表
  【类比后端】
    - 相当于后端的首页 Controller：MarketIndexController
    - @GetMapping("/") → 渲染首页模板（类似 ModelAndview("home"))
    - 页面加载时调用 queryGroupBuyMarketConfig() 获取数据（类比 @PostConstruct 初始化）

  【页面结构】
    1. 顶部导航栏：拼团商城 Logo | 用户名 | 订单入口
    2. 商品图片轮播图（3张渐变色块，3秒自动切换）
    3. 商品信息区：标题、价格、统计数据
    4. 拼团列表：使用 TeamCard 组件循环渲染
    5. 底部操作栏：单独购买 | 开团购买

  【组件依赖关系】（类比后端的 @Autowired 依赖注入）
    HomeView（父组件）
      ├── TeamCard（子组件，渲染每个拼团）
      │     └── CountdownTimer（孙组件，显示倒计时）
      └── Element Plus 组件（el-button, el-tag 等）
-->

<template>
  <!--
    【前端概念】<template> 定义页面的 HTML 结构
    【类比后端】相当于 JSP/Thymeleaf 的模板文件，定义页面长什么样
    注意：template 中只能有一个根元素（这里用 .home-page 作为根）
  -->
  <div class="home-page">

    <!-- ========================================== -->
    <!-- 一、顶部导航栏                              -->
    <!-- ========================================== -->
    <!--
      【前端概念】固定的顶部导航，使用 Flexbox 水平排列
      【类比后端】相当于 JSP 的 header.jsp 被 include 到每个页面
    -->
    <header class="top-nav">
      <div class="nav-left">
        <span class="logo">🛒</span>
        <span class="nav-title">拼团商城</span>
      </div>
      <div class="nav-right">
        <!--
          【前端概念】{{ userStore.username }} 显示响应式变量的值
          【类比后端】相当于 Thymeleaf 的 th:text="${session.username}"
          userStore 是 Pinia Store 的实例，username 是其中的响应式字段
        -->
        <span class="username">{{ userStore.username }}</span>
        <!--
          【前端概念】@click="goToOrders" 绑定点击事件
          【类比后端】相当于 <a href="/orders" onclick="redirect()"> 的简化版
          点击后调用 goToOrders() 方法，内部使用 router.push() 跳转路由
        -->
        <el-button size="small" round @click="goToOrders">
          📋 我的订单
        </el-button>
        <!--
          【前端概念】退出登录按钮
          点击后调用 handleLogout()，清除用户信息并跳转登录页
        -->
        <el-button size="small" type="info" plain round @click="handleLogout">
          退出
        </el-button>
      </div>
    </header>

    <!-- ========================================== -->
    <!-- 二、加载中状态                              -->
    <!-- ========================================== -->
    <!--
      【v-if 条件渲染】
      - v-if="loading" 表示只有 loading 为 true 时才渲染这段 DOM
      - 【类比后端】相当于 JSTL 的 <c:if test="${loading}">
      - 数据加载期间显示 loading 动画，加载完成后隐藏
    -->
    <div v-if="loading" class="loading-container">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <p>正在加载拼团数据...</p>
    </div>

    <!-- ========================================== -->
    <!-- 三、加载失败状态                            -->
    <!-- ========================================== -->
    <!--
      【v-if + v-else-if 多条件渲染】
      - 类似 Java 的 if-else if-else 分支
      - loading 结束后，如果 error 有值，显示错误提示
    -->
    <div v-else-if="error" class="error-container">
      <p class="error-icon">😵</p>
      <p class="error-text">{{ error }}</p>
      <el-button type="primary" @click="loadData">重新加载</el-button>
    </div>

    <!-- ========================================== -->
    <!-- 四、主内容区域（数据加载成功后显示）         -->
    <!-- ========================================== -->
    <!--
      【v-else】
      - loading 为 false 且 error 为空时显示主内容
      - 相当于 if-else if-else 的最后一个 else 分支
    -->
    <main v-else class="main-content">

      <!-- ====== 4.1 商品图片轮播图 ====== -->
      <!--
        【前端概念】CSS 实现的轮播图，3张图用渐变色块代替
        不用第三方库，纯 CSS + JS 控制 currentIndex 切换显示
        【类比后端】相当于 Bootstrap Carousel 或 Slick Slider 组件
      -->
      <section class="carousel-section">
        <div class="carousel-wrapper">
          <!--
            【v-for 列表渲染】
            - v-for="(slide, index) in slides" 遍历 slides 数组，为每个元素生成一个 div
            - :key="index" 是 Vue 用来高效更新 DOM 的唯一标识
            - :class="{ active: currentSlide === index }" 动态绑定 CSS class
            - 【类比后端】
              - v-for 相当于 JSTL 的 <c:forEach items="${slides}" var="slide">
              - 或 Thymeleaf 的 th:each="slide : ${slides}"
              - :key 相当于 <c:forEach> 中每条记录的 id，帮助框架识别哪些元素变了
          -->
          <div
            v-for="(slide, index) in slides"
            :key="index"
            class="carousel-slide"
            :class="{ active: currentSlide === index }"
            :style="{ background: slide.bg }"
          >
            <div class="slide-content">
              <span class="slide-icon">{{ slide.icon }}</span>
              <h2 class="slide-title">{{ slide.title }}</h2>
              <p class="slide-desc">{{ slide.desc }}</p>
            </div>
          </div>
        </div>
        <!-- 轮播指示器（小圆点） -->
        <div class="carousel-indicators">
          <span
            v-for="(_, index) in slides"
            :key="index"
            class="indicator"
            :class="{ active: currentSlide === index }"
            @click="currentSlide = index"
          />
        </div>
      </section>

      <!-- ====== 4.2 商品信息区 ====== -->
      <section class="goods-section">
        <div class="goods-header">
          <h1 class="goods-title">🎯 精选好物拼团</h1>
          <el-tag type="danger" effect="dark" round size="large">限时特惠</el-tag>
        </div>

        <!--
          价格展示区
          【前端概念】使用 computed 计算属性显示价格
          价格数据来自 marketData.goods（API 返回的商品信息）
        -->
        <div class="price-area">
          <div class="price-row">
            <span class="original-price">原价：¥{{ formatPrice(originalPrice) }}</span>
            <span class="group-price">拼团价：¥{{ formatPrice(groupPrice) }}</span>
          </div>
          <div class="save-row">
            <el-tag type="success" effect="plain" round>
              💰 已省：¥{{ formatPrice(savedAmount) }}
            </el-tag>
          </div>
        </div>

        <!--
          拼团统计信息
          使用 teamStatistic 数据展示总团数、已成团数、参与人数
        -->
        <div class="stats-row">
          <div class="stat-item">
            <span class="stat-value">{{ teamStatistic.allTeamCount }}</span>
            <span class="stat-label">个团</span>
          </div>
          <div class="stat-divider">|</div>
          <div class="stat-item">
            <span class="stat-value">{{ teamStatistic.allTeamCompleteCount }}</span>
            <span class="stat-label">个已成团</span>
          </div>
          <div class="stat-divider">|</div>
          <div class="stat-item">
            <span class="stat-value">{{ teamStatistic.allTeamUserCount }}</span>
            <span class="stat-label">人参与</span>
          </div>
        </div>
      </section>

      <!-- ====== 4.3 拼团列表区域 ====== -->
      <section class="team-list-section">
        <h2 class="section-title">🔥 进行中的拼团</h2>

        <!--
          【v-for 渲染 TeamCard 组件列表】
          - v-for="team in teamList" 遍历拼团列表，每个 team 渲染一个 TeamCard
          - :key="team.teamId" 用 teamId 作为唯一标识（类比数据库主键）
          - :teamInfo="team" 将 team 数据作为 props 传给 TeamCard
            【类比后端】相当于 <jsp:include> 时传入 model attribute
          - @joinTeam="handleJoinTeam" 监听 TeamCard 发出的 joinTeam 事件
            【类比后端】相当于 button.addActionListener(listener)

          【v-if 空列表处理】
          - 如果 teamList 为空，显示"暂无拼团"提示
          - 【类比后端】相当于 if (list.isEmpty()) { 显示空状态 }
        -->
        <div v-if="teamList.length > 0" class="team-list">
          <TeamCard
            v-for="team in teamList"
            :key="team.teamId"
            :teamInfo="team"
            @joinTeam="handleJoinTeam"
          />
        </div>
        <div v-else class="empty-teams">
          <p>🎉 暂无进行中的拼团</p>
          <p class="empty-hint">快来开第一个团吧！</p>
        </div>
      </section>
    </main>

    <!-- ========================================== -->
    <!-- 五、底部操作栏（固定在底部）                 -->
    <!-- ========================================== -->
    <!--
      只在数据加载完成且无错误时显示
      【类比后端】相当于页面底部的 footer 区域
    -->
    <footer v-if="!loading && !error" class="bottom-bar">
      <!--
        单独购买按钮
        【前端概念】@click 绑定点击事件，调用 handleSoloBuy() 方法
        【类比后端】相当于 <a href="/detail?action=solo"> 的 JS 版本
      -->
      <el-button
        class="action-btn solo-btn"
        size="large"
        @click="handleSoloBuy"
      >
        🛍️ 单独购买
        <span class="btn-price">¥{{ formatPrice(groupPrice) }}</span>
      </el-button>

      <!--
        开团购买按钮
        【前端概念】点击后跳转到详情页，通过 router.push 传递参数
        【类比后端】相当于 response.sendRedirect("/detail?action=newTeam")
      -->
      <el-button
        class="action-btn team-btn"
        type="primary"
        size="large"
        @click="handleNewTeam"
      >
        🚀 开团购买
        <span class="btn-price">¥{{ formatPrice(groupPrice) }}</span>
      </el-button>
    </footer>
  </div>
</template>

<script setup lang="ts">
/**
 * HomeView 组件的逻辑部分（相当于 Java Controller）
 *
 * 【前端概念】<script setup lang="ts"> 是 Vue 3 组合式 API 的语法糖
 * 【类比后端】
 *   - 相当于 @Controller 注解的类，处理页面逻辑
 *   - template 是视图（View），script 是控制器（Controller）
 *   - 合在一起就是 MVC 的完整实现
 *
 * 本组件职责：
 *   1. 页面加载时调用 API 获取拼团数据（类比 @PostConstruct 初始化）
 *   2. 将数据绑定到模板（类比 Model.addAttribute("data", result)）
 *   3. 处理用户交互事件（按钮点击 → 路由跳转）
 */

import { ref, computed, onMounted, onUnmounted } from 'vue'
// 【前端概念】从 vue 中导入响应式 API 和生命周期钩子
// 【类比后端】相当于 import javax.annotation.PostConstruct; 等
//
// 各 API 说明：
// - ref:          创建响应式变量（类比 AtomicReference，修改值触发页面重渲染）
// - computed:     创建计算属性（类比 getter 方法，依赖变化时自动重算，有缓存）
// - onMounted:    生命周期钩子，组件挂载到 DOM 后执行（类比 @PostConstruct）
// - onUnmounted:  生命周期钩子，组件从 DOM 卸载前执行（类比 @PreDestroy）

import { useRouter } from 'vue-router'
// 【前端概念】useRouter() 获取路由实例，用于编程式导航（代码控制跳转）
// 【类比后端】
//   - 相当于 HttpServletResponse.sendRedirect("/detail")
//   - 或者 Spring MVC 的 return "redirect:/detail"
//   - 但前端路由跳转不刷新页面，通过 JS 动态切换组件（SPA 单页应用）

import { ElMessage } from 'element-plus'
// 【前端概念】Element Plus 的消息提示组件
// 【类比后端】相当于后端的 response.sendSuccessMessage("操作成功")

import { Loading } from '@element-plus/icons-vue'
// 【前端概念】Element Plus 的 Loading 图标组件

import { useUserStore } from '@/stores/user'
// 【前端概念】引入 Pinia 用户 Store（全局共享的用户状态）
// 【类比后端】
//   - 相当于 @Autowired UserService userService;
//   - 或者 HttpSession session; (String) session.getAttribute("username")
//   - Store 中存储了当前登录用户的用户名、登录状态等

import { queryGroupBuyMarketConfig } from '@/api/index'
// 【前端概念】引入首页 API 函数
// 【类比后端】相当于 @Autowired IMarketIndexService marketIndexService;
// 调用这个函数就是调用后端接口获取数据

import type { GoodsMarketResponseDTO, TeamInfo } from '@/api/types'
// 【前端概念】引入类型定义（只用于类型检查，编译后擦除）
// 【类比后端】相当于 import DTO 类，用于声明变量类型

import TeamCard from '@/components/TeamCard.vue'
// 【前端概念】引入 TeamCard 子组件
// 【类比后端】
//   - 相当于 @Autowired TeamCardBean teamCard; 在页面中注入这个组件
//   - 或者 JSP 的 <jsp:include page="TeamCard.jsp">
//   - 引入后就可以在 template 中使用 <TeamCard /> 标签

// =============================================
// 一、获取路由和 Store 实例（类比依赖注入）
// =============================================

/**
 * 获取路由实例
 * 【类比后端】相当于获取 HttpServletResponse 对象用于重定向
 *   - HttpServletResponse response = ...;
 *   - response.sendRedirect("/detail?teamId=xxx");
 * 前端用 router.push() 实现同样效果，但不刷新页面
 */
const router = useRouter()

/**
 * 获取用户 Store 实例
 * 【类比后端】相当于 @Autowired UserService userService;
 *   - 通过 useUserStore() 获取全局单例的 Store
 *   - 可以读取 userStore.username（当前用户名）
 *   - 可以调用 userStore.logout()（退出登录）
 */
const userStore = useUserStore()

// =============================================
// 二、响应式状态（类比 Java 类的成员变量）
// =============================================

/**
 * 页面加载状态
 * 【类比后端】相当于 private boolean loading = false;
 *   - true:  正在请求后端接口，显示 loading 动画
 *   - false: 请求完成（成功或失败），隐藏 loading
 *
 * 【为什么用 ref 而不是普通变量？】
 *   - let loading = false;  → 普通变量，赋值后页面不会自动更新
 *   - const loading = ref(false);  → 响应式变量，赋值 loading.value = true 后
 *     页面中 v-if="loading" 的部分自动显示/隐藏
 *   - 【类比后端】ref ≈ 有 PropertyChangeListener 的字段，变化时通知所有监听者
 */
const loading = ref<boolean>(true)

/**
 * 错误信息
 * 【类比后端】相当于 private String errorMsg = null;
 *   - null 或空字符串: 没有错误
 *   - 非空字符串: 有错误，显示错误提示
 */
const error = ref<string>('')

/**
 * 首页完整数据
 * 【类比后端】相当于 private GoodsMarketResponseDTO marketData;
 *   - 存储从后端接口获取的全部数据（商品信息、拼团列表、统计数据）
 *   - 初始值为 null，API 调用成功后赋值
 *
 * 【泛型说明】
 *   - ref<GoodsMarketResponseDTO | null>(null)
 *   - | 是 TypeScript 的"联合类型"，表示可以是 GoodsMarketResponseDTO 或 null
 *   - 【类比后端】相当于 Java 的 GoodsMarketResponseDTO marketData（对象可以为 null）
 */
const marketData = ref<GoodsMarketResponseDTO | null>(null)

/**
 * 轮播图当前索引
 * 【类比后端】相当于 private int currentSlideIndex = 0;
 *   - 控制当前显示第几张轮播图（0, 1, 2 对应三张图）
 */
const currentSlide = ref<number>(0)

/**
 * 轮播定时器 ID
 * 【类比后端】相当于 private ScheduledFuture<?> carouselTimer;
 *   - 保存定时器引用，用于组件销毁时清除（防止内存泄漏）
 */
let carouselTimerId: ReturnType<typeof setInterval> | null = null

// =============================================
// 三、轮播图数据（静态配置，类比常量）
// =============================================

/**
 * 轮播图配置数组
 * 【类比后端】相当于 Java 的 static final List<SlideConfig> SLIDES = Arrays.asList(...)
 * 定义 3 张轮播图的内容（用渐变色块代替真实图片）
 */
const slides = [
  {
    bg: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    icon: '🎁',
    title: '精选好物',
    desc: '品质保证，超值拼团'
  },
  {
    bg: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    icon: '🔥',
    title: '限时特惠',
    desc: '三人成团，立享优惠'
  },
  {
    bg: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    icon: '🚀',
    title: '快速成团',
    desc: '海量好团，等你来拼'
  }
]

// =============================================
// 四、计算属性（类比 Java 的 getter 方法，带缓存）
// =============================================

/**
 * 商品原价
 * 【类比后端】相当于 Java 的：
 *   public double getOriginalPrice() {
 *       return marketData != null ? marketData.getGoods().getOriginalPrice() : 99.00;
 *   }
 *
 * 【前端概念】computed 计算属性，依赖 marketData 变化时自动重算
 * 如果数据还没加载完，显示默认值 99.00（防止页面空白）
 */
const originalPrice = computed(() => marketData.value?.goods?.originalPrice ?? 99.00)

/**
 * 拼团价（实际支付价格）
 * 【类比后端】相当于 getGoods().getPayPrice()
 */
const groupPrice = computed(() => marketData.value?.goods?.payPrice ?? 59.00)

/**
 * 节省金额（原价 - 拼团价）
 * 【类比后端】相当于 Java 的：
 *   public double getSavedAmount() {
 *       return originalPrice - groupPrice;  // 调用上面的 getter
 *   }
 *
 * 【computed 的链式依赖】
 *   - savedAmount 依赖 originalPrice 和 groupPrice
 *   - originalPrice 和 groupPrice 依赖 marketData
 *   - 当 marketData 变化时，整条链自动重新计算
 *   - 【类比后端】相当于 Java getter 之间的相互调用，数据变了所有相关值都更新
 */
const savedAmount = computed(() => originalPrice.value - groupPrice.value)

/**
 * 拼团统计数据
 * 【类比后端】相当于 getTeamStatistic()，如果数据未加载则返回默认值
 *
 * 【对象展开运算符 ...】
 *   - { ...defaultObj } 创建对象的浅拷贝（类比 Java 的 new Object(obj)）
 *   - marketData.value?.teamStatistic ?? { ... } 使用可选链 + 空值合并
 *   - 【类比后端】相当于 Optional.ofNullable(data).orElse(defaultValue)
 */
const teamStatistic = computed(() => marketData.value?.teamStatistic ?? {
  allTeamCount: 0,
  allTeamCompleteCount: 0,
  allTeamUserCount: 0
})

/**
 * 拼团列表
 * 【类比后端】相当于 getTeamList()，如果数据未加载则返回空数组
 */
const teamList = computed(() => marketData.value?.teamList ?? [])

// =============================================
// 五、工具方法
// =============================================

/**
 * 格式化价格显示（保留两位小数）
 *
 * 【类比后端】相当于 Java 的：
 *   public String formatPrice(double price) {
 *       return String.format("%.2f", price);
 *   }
 *
 * @param price 价格数字
 * @returns 格式化后的字符串，如 "59.00"
 */
function formatPrice(price: number): string {
  return price.toFixed(2)
  // 【前端概念】Number.toFixed(2) 保留两位小数
  // 【类比后端】相当于 String.format("%.2f", price) 或 DecimalFormat
}

// =============================================
// 六、数据加载（类比后端 Service 调用）
// =============================================

/**
 * 加载首页数据
 *
 * 【类比后端】相当于 @PostConstruct 初始化方法，或 Controller 调用 Service 获取数据：
 *   @PostConstruct
 *   public void init() {
 *       try {
 *           this.marketData = marketIndexService.queryGroupBuyMarketConfig(request);
 *       } catch (Exception e) {
 *           this.errorMsg = "数据加载失败";
 *       } finally {
 *           this.loading = false;
 *       }
 *   }
 *
 * 【前端概念】async/await 异步函数
 *   - async 标记函数为异步函数（类比 Java 的 CompletableFuture 或 @Async）
 *   - await 等待 Promise 完成（类比 future.get()，但不阻塞主线程）
 *   - 异步操作不会阻塞 UI 渲染，用户依然可以看到 loading 动画
 *
 * 【try/catch/finally 结构】
 *   - 与 Java 完全一致：try 尝试执行，catch 捕获异常，finally 无论成败都执行
 *   - loading.value = false 放在 finally 中，确保无论成功还是失败都关闭 loading
 */
async function loadData() {
  // 开始加载：设置 loading 状态为 true，清空错误信息
  loading.value = true
  error.value = ''

  try {
    // 【关键调用】调用 API 函数获取首页数据
    // 【类比后端】相当于：
    //   GoodsMarketRequestDTO request = new GoodsMarketRequestDTO();
    //   request.setUserId(userStore.getUsername());
    //   request.setGoodsId("goods123");
    //   GoodsMarketResponseDTO response = marketIndexService.queryGroupBuyMarketConfig(request);
    //
    // await 会等待 Promise 完成（后端响应返回），期间不阻塞 UI
    // 返回值直接是 GoodsMarketResponseDTO（响应拦截器已经解包了 ApiResponse 外层）
    const data = await queryGroupBuyMarketConfig({
      userId: userStore.username || 'guest',  // 当前用户名，未登录则为 'guest'
      source: 's01',                           // 来源
      channel: 'c01',                          // 渠道
      goodsId: '9890001'                       // 商品ID
    })

    // 请求成功：将数据赋值给响应式变量
    // 【重要】赋值后，所有依赖 marketData 的 computed 属性自动重算
    //         模板中使用这些属性的部分也会自动重新渲染
    // 【类比后端】相当于 model.addAttribute("marketData", response);
    marketData.value = data

  } catch (err) {
    // 请求失败：记录错误信息
    // 【类比后端】相当于 catch (Exception e) { log.error("加载失败", e); }
    console.error('加载首页数据失败:', err)
    error.value = '数据加载失败，请稍后重试'

  } finally {
    // 无论成功还是失败，都关闭 loading 状态
    // 【类比后端】相当于 finally { this.loading = false; }
    loading.value = false
  }
}

// =============================================
// 七、事件处理方法（类比 Controller 的 @PostMapping 方法）
// =============================================

/**
 * 处理"参与拼团"按钮点击（来自 TeamCard 子组件的事件）
 *
 * 【前端概念】子组件 TeamCard 通过 emit('joinTeam', teamInfo) 发送事件
 * 父组件 HomeView 通过 @joinTeam="handleJoinTeam" 监听并处理
 *
 * 【类比后端】
 *   - 相当于 Spring MVC 的 @EventHandler 或 Servlet 的 ActionListener
 *   - 子组件 = 事件发布者（Publisher），父组件 = 事件订阅者（Subscriber）
 *   - 这是"观察者模式"（Observer Pattern）在前端的应用
 *
 * @param team 被点击的拼团数据（TeamInfo 类型）
 */
function handleJoinTeam(team: TeamInfo) {
  // 【路由跳转】跳转到详情页，并通过 query 参数传递 teamId
  // 【类比后端】相当于 response.sendRedirect("/detail?teamId=" + team.getTeamId())
  //
  // 【前端概念】router.push() 编程式导航（用 JS 代码控制路由跳转）
  //   - 与 <router-link to="/detail"> 效果相同，但可以在事件处理函数中动态调用
  //   - query 参数会拼接到 URL：/detail?teamId=xxx&activityId=yyy
  //   - 【类比后端】相当于 RedirectView("/detail").addAttribute("teamId", xxx)
  router.push({
    path: '/detail',
    query: {
      teamId: team.teamId,
      activityId: String(team.activityId),
      goodsId: '9890001'
    }
  })
}

/**
 * 处理"单独购买"按钮点击
 *
 * 【前端概念】单独购买 = 开新团（teamId 为空）
 * 【类比后端】相当于点击"立即购买"后 servlet 转发到详情页，不传 teamId
 */
function handleSoloBuy() {
  // 跳转到详情页，不传 teamId（表示开新团）
  router.push({
    path: '/detail',
    query: {
      goodsId: '9890001',
      activityId: String(marketData.value?.activityId || 0)
      // 注意：没有 teamId，后端会创建新团
    }
  })
}

/**
 * 处理"开团购买"按钮点击
 *
 * 【前端概念】与"单独购买"类似，跳转到详情页开新团
 * 两个按钮的区别主要是 UI 文案和位置不同
 */
function handleNewTeam() {
  router.push({
    path: '/detail',
    query: {
      goodsId: '9890001',
      activityId: String(marketData.value?.activityId || 0)
    }
  })
}

/**
 * 处理"我的订单"按钮点击
 *
 * 【前端概念】直接跳转到订单列表页
 * 【类比后端】相当于 response.sendRedirect("/orders")
 */
function goToOrders() {
  router.push({ path: '/orders' })
}

/**
 * 处理退出登录
 *
 * 【前端概念】调用 userStore.logout() 清除用户状态，然后跳转到登录页
 * 【类比后端】
 *   - 相当于 HttpSession session.invalidate();  // 清除 Session
 *   - 然后 response.sendRedirect("/login");     // 重定向到登录页
 */
function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push({ name: 'login' })
}

// =============================================
// 八、轮播图自动切换逻辑
// =============================================

/**
 * 启动轮播定时器
 *
 * 【前端概念】setInterval() 每隔指定毫秒数重复执行回调
 * 【类比后端】
 *   - 相当于 Java 的 ScheduledExecutorService.scheduleAtFixedRate()
 *   - 每 3 秒切换一次轮播图（3000 毫秒）
 *
 * 【为什么保存 timerId？】
 *   - 组件销毁时必须清除定时器，否则定时器继续运行会导致内存泄漏
 *   - 【类比后端】就像 @PreDestroy 中要 executorService.shutdown()
 */
function startCarousel() {
  carouselTimerId = setInterval(() => {
    // 切换到下一张图：(当前索引 + 1) % 总数 = 循环切换 0 → 1 → 2 → 0 → ...
    // 【类比后端】相当于 Java 的 (index + 1) % list.size()
    currentSlide.value = (currentSlide.value + 1) % slides.length
  }, 3000)  // 3000 毫秒 = 3 秒
}

/**
 * 停止轮播定时器
 * 【类比后端】相当于 executorService.shutdown(); timerTask.cancel();
 */
function stopCarousel() {
  if (carouselTimerId !== null) {
    clearInterval(carouselTimerId)
    carouselTimerId = null
  }
}

// =============================================
// 九、生命周期钩子（类比 Spring Bean 的生命周期回调）
// =============================================

/**
 * 组件挂载完成后执行（页面第一次显示时）
 *
 * 【前端概念】onMounted() 是 Vue 3 的生命周期钩子
 * 在组件被插入到 DOM 后自动调用，适合做初始化操作：
 *   - 调用 API 加载数据
 *   - 启动定时器
 *   - 绑定事件监听器
 *
 * 【类比后端】
 *   - 相当于 Spring Bean 的 @PostConstruct 注解方法
 *   - 或者 Servlet 的 init() 方法
 *   - 在 Bean 创建完成、所有依赖注入完成后执行一次
 *
 * 【执行时机】
 *   - 组件第一次渲染到页面后调用（只调用一次）
 *   - 此时 template 已经转换为真实 DOM，可以安全地操作 DOM 或发起请求
 */
onMounted(() => {
  // 【类比后端】相当于 @PostConstruct 方法：
  //   @PostConstruct
  //   public void init() {
  //       loadData();        // 1. 加载初始数据
  //       startCarousel();   // 2. 启动轮播定时器
  //   }
  loadData()       // 调用 API 获取首页数据
  startCarousel()  // 启动轮播图自动切换
})

/**
 * 组件销毁前执行（页面切换、组件被移除时）
 *
 * 【前端概念】onUnmounted() 是 Vue 3 的生命周期钩子
 * 在组件从 DOM 中移除前自动调用，适合做清理操作：
 *   - 清除定时器（防止内存泄漏）
 *   - 解除事件监听
 *   - 取消未完成的 API 请求
 *
 * 【类比后端】
 *   - 相当于 Spring Bean 的 @PreDestroy 注解方法
 *   - 或者 Servlet 的 destroy() 方法
 *   - 在 Bean 被销毁前执行清理工作
 *
 * 【为什么必须清除定时器？】
 *   - setInterval 创建的定时器是全局的，不会因为组件销毁而自动停止
 *   - 如果不清除，定时器继续执行，尝试更新已不存在的组件 → 内存泄漏
 *   - 【类比后端】就像线程池中的定时任务，应用关了但任务线程还在跑
 */
onUnmounted(() => {
  // 【类比后端】相当于 @PreDestroy 方法：
  //   @PreDestroy
  //   public void cleanup() {
  //       if (carouselTimer != null) {
  //           carouselTimer.cancel();  // 取消定时任务
  //           carouselTimer = null;
  //       }
  //   }
  stopCarousel()  // 清除轮播定时器，防止内存泄漏
})
</script>

<style scoped>
/**
 * 【前端概念】<style scoped> 定义组件的私有 CSS 样式
 * scoped 表示这些样式只对当前组件生效，不会影响其他组件
 * 【类比后端】相当于 Java 的 private 字段，只在本类内部可见
 *
 * 如果不加 scoped，样式是全局的，可能"污染"其他组件的样式
 * 【类比后端】不加 scoped ≈ 所有字段都是 public static，全局可见
 */

/* ====== 全局页面容器 ====== */
.home-page {
  min-height: 100vh;
  /* 【前端概念】vh = viewport height，100vh = 屏幕高度的 100% */
  /* 【类比后端】相当于 CSS 的 height: 100%，但 vh 不受父元素影响 */
  background: #f5f7fa;
  padding-bottom: 80px;
  /* 底部留出操作栏的高度，防止内容被遮挡 */
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ====== 顶部导航栏 ====== */
.top-nav {
  position: sticky;
  /* 【前端概念】sticky 定位：滚动时固定在顶部（类似手机 APP 的顶部导航） */
  /* 【类比后端】相当于 CSS position: fixed 的改良版，初始按正常流排列 */
  top: 0;
  z-index: 100;
  /* 【前端概念】z-index 控制层叠顺序，数值越大越在上层 */
  /* 【类比后端】相当于 Java 的 Swing 组件的 setZOrder() */
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 56px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  /* 【前端概念】backdrop-filter 对背景进行模糊处理（毛玻璃效果） */
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo {
  font-size: 24px;
}

.nav-title {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, #409EFF, #f56c6c);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  /* 【前端概念】渐变文字效果：将背景渐变裁剪到文字形状 */
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.username {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

/* ====== Loading 加载状态 ====== */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60vh;
  gap: 16px;
  color: #909399;
}

.loading-icon {
  font-size: 40px;
  animation: spin 1s linear infinite;
  /* 【前端概念】CSS animation 定义旋转动画 */
  /* 【类比后端】相当于 Java 的 TimerTask 定时执行旋转动作 */
}

@keyframes spin {
  /* 【前端概念】@keyframes 定义动画的关键帧 */
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}

/* ====== 错误状态 ====== */
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
  color: #f56c6c;
  font-size: 16px;
}

/* ====== 主内容区域 ====== */
.main-content {
  max-width: 640px;
  /* 【前端概念】max-width 限制最大宽度，大屏时居中显示 */
  /* 【类比后端】相当于 CSS 的 container 约束，类似 Bootstrap 的 container */
  margin: 0 auto;
  /* 【前端概念】margin: 0 auto 水平居中（auto 自动计算左右 margin） */
  padding: 16px;
}

/* ====== 轮播图区域 ====== */
.carousel-section {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.carousel-wrapper {
  position: relative;
  height: 200px;
}

.carousel-slide {
  position: absolute;
  inset: 0;
  /* 【前端概念】inset: 0 等同于 top:0; right:0; bottom:0; left:0; 铺满父容器 */
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.6s ease;
  /* 【前端概念】transition 定义属性变化时的过渡动画效果 */
  /* 【类比后端】相当于 CSS 的动画，类似 Java 的 AnimationTimer */
}

.carousel-slide.active {
  opacity: 1;
}

.slide-content {
  text-align: center;
  color: white;
}

.slide-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 8px;
}

.slide-title {
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 4px 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.slide-desc {
  font-size: 14px;
  margin: 0;
  opacity: 0.9;
}

/* 轮播指示器（小圆点） */
.carousel-indicators {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
}

.indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: all 0.3s ease;
}

.indicator.active {
  background: white;
  width: 20px;
  border-radius: 4px;
}

/* ====== 商品信息区域 ====== */
.goods-section {
  background: white;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.goods-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.goods-title {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  margin: 0;
}

.price-area {
  background: linear-gradient(135deg, #fff5f5, #fff0f0);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 16px;
  margin-bottom: 8px;
}

.original-price {
  font-size: 14px;
  color: #909399;
  text-decoration: line-through;
  /* 【前端概念】text-decoration: line-through 给文字加删除线 */
}

.group-price {
  font-size: 24px;
  font-weight: 700;
  color: #f56c6c;
}

.save-row {
  display: flex;
  align-items: center;
}

/* 统计行 */
.stats-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #409EFF;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.stat-divider {
  color: #DCDFE6;
  font-size: 16px;
}

/* ====== 拼团列表区域 ====== */
.team-list-section {
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px 0;
}

.team-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* 空列表状态 */
.empty-teams {
  text-align: center;
  padding: 40px 20px;
  background: white;
  border-radius: 12px;
  color: #909399;
}

.empty-hint {
  font-size: 13px;
  color: #C0C4CC;
  margin-top: 8px;
}

/* ====== 底部操作栏 ====== */
.bottom-bar {
  position: fixed;
  /* 【前端概念】fixed 定位：相对于浏览器窗口固定，不随页面滚动 */
  /* 【类比后端】相当于 CSS 的 position: sticky 的"绝对固定"版本 */
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 0;
  padding: 12px 16px;
  padding-bottom: calc(12px + env(safe-area-inset-bottom, 0px));
  /* 【前端概念】env(safe-area-inset-bottom) 适配 iPhone 底部安全区域 */
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(10px);
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.08);
  z-index: 100;
  max-width: 640px;
  margin: 0 auto;
}

.action-btn {
  flex: 1;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.action-btn:first-child {
  border-radius: 24px 0 0 24px;
}

.action-btn:last-child {
  border-radius: 0 24px 24px 0;
}

.btn-price {
  font-size: 13px;
  opacity: 0.9;
}

.solo-btn {
  background: #fff;
  border: 2px solid #f56c6c;
  color: #f56c6c;
}

.solo-btn:hover {
  background: #fff5f5;
}

.team-btn {
  background: linear-gradient(135deg, #409EFF, #66b1ff);
  border: none;
}

/* ====== 响应式适配（小屏幕） ====== */
@media (max-width: 480px) {
  .goods-title {
    font-size: 18px;
}

  .group-price {
    font-size: 20px;
  }

  .slide-title {
    font-size: 20px;
  }
}
</style>
