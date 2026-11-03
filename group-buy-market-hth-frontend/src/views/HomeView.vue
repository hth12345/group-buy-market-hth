<!--
  HomeView.vue - 拼团商城首页（活动列表页）
  ============================
  【前端概念】这是登录后的主页面，展示所有可见的拼团活动卡片列表
  【类比后端】
    - 相当于后端的首页 Controller：MarketIndexController
    - @GetMapping("/") → 渲染首页模板（类似 ModelAndview("home"))
    - 页面加载时调用 queryAllActivities() 获取所有活动数据（类比 @PostConstruct 初始化）

  【页面结构】
    1. 顶部导航栏：拼团商城 Logo | 用户名 | 订单入口 | 退出按钮
    2. 加载状态 / 错误状态 / 空状态
    3. 活动卡片列表：使用 v-for 循环渲染每个活动卡片（el-card）
       【重要改动】每个活动卡片内嵌套商品列表，使用 v-for 双层循环
       - 外层 v-for：遍历活动列表（activities）
       - 内层 v-for：遍历每个活动下的商品列表（activity.goods）
       - 【类比后端】相当于 Java 的嵌套循环：
         for (AllActivitiesResponseDTO activity : activities) {
             for (ActivityGoods goods : activity.getGoods()) {
                 // 渲染商品卡片
             }
         }
    4. 点击商品的"参团"按钮跳转到活动详情页（/detail?activityId=xxx&goodsId=xxx）

  【组件依赖关系】（类比后端的 @Autowired 依赖注入）
    HomeView（父组件）
      └── Element Plus 组件（el-button, el-tag, el-card, el-empty 等）
-->

<template>
  <!--
    【前端概念】<template> 定义页面的 HTML 结构
    【类比后端】相当于 JSP/Thymeleaf 的模板文件，定义页面长什么样
    注意：template 中只能有一个根元素（这里用 .home-page 作为根）
  -->
  <div class="home-page">

    <!-- ========================================== -->
    <!-- 一、顶部导航栏（保留不变）                    -->
    <!-- ========================================== -->
    <!--
      【前端概念】固定的顶部导航，使用 Flexbox 水平排列
      【类比后端】相当于 JSP 的 header.jsp 被 include 到每个页面
    -->
    <header class="top-nav">
      <div class="nav-left">
        <span class="logo">&#x1F6D2;</span>
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
          &#x1F4CB; 我的订单
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
      <p>正在加载活动列表...</p>
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
      <p class="error-icon">&#x1F635;</p>
      <p class="error-text">{{ error }}</p>
      <el-button type="primary" @click="loadActivities">重新加载</el-button>
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

      <!-- ====== 4.1 页面标题区 ====== -->
      <section class="page-header">
        <h1 class="page-title">&#x1F3AF; 精选好物拼团</h1>
        <el-tag type="danger" effect="dark" round size="large">限时特惠</el-tag>
      </section>

      <!-- ====== 4.2 活动卡片列表 ====== -->
      <!--
        【前端概念】活动列表容器，纵向排列活动卡片
        【类比后端】
          - 外层 v-for 遍历 activities 数组，为每个活动生成一个活动卡片
          - 相当于 JSTL 的 <c:forEach items="${activities}" var="activity">
          - 或 Thymeleaf 的 th:each="activity : ${activities}"
      -->
      <div v-if="activities.length > 0" class="activity-list">
        <!--
          【外层 v-for 渲染活动卡片】
          - v-for="activity in activities" 遍历活动列表
          - :key="activity.activityId" 用 activityId 作为唯一标识（类比数据库主键）
          - 【类比后端】相当于 Java 的外层循环：
            for (AllActivitiesResponseDTO activity : activities) { ... }
        -->
        <div
          v-for="activity in activities"
          :key="activity.activityId"
          class="activity-card-wrapper"
        >
          <!--
            el-card 活动卡片容器
            shadow="hover" 鼠标悬停时显示阴影（el-card 内置效果）
          -->
          <el-card shadow="hover" class="activity-card">
            <!-- ====== 活动头部信息 ====== -->
            <div class="activity-header">
              <!--
                活动名称
                【前端概念】{{ activity.activityName }} 显示活动名称
                【类比后端】相当于 ${activity.activityName} 在 Thymeleaf 中
              -->
              <h3 class="activity-name">{{ activity.activityName }}</h3>
              <!-- 活动状态标签 -->
              <el-tag
                :type="getActivityStatusType(activity.status)"
                size="small"
                effect="dark"
              >
                {{ getActivityStatusText(activity.status) }}
              </el-tag>
            </div>

            <!-- ====== 活动详情信息行 ====== -->
            <!--
              【前端概念】展示活动的关键参数：成团人数、有效时长、次数限制
              【类比后端】相当于 DTO 字段的格式化展示
            -->
            <div class="activity-meta">
              <!--
                el-tag 标签展示活动属性
                - target: 拼团目标人数（如 5 人成团）
                - validTime: 拼团时长（分钟转小时展示）
                - takeLimitCount: 每人限参团次数
              -->
              <el-tag size="small" type="primary" effect="plain">
                &#x1F3AF; {{ activity.target }}人成团
              </el-tag>
              <el-tag size="small" type="success" effect="plain">
                &#x23F0; 有效{{ formatValidTime(activity.validTime) }}
              </el-tag>
              <el-tag size="small" type="warning" effect="plain">
                &#x1F464; 每人限{{ activity.takeLimitCount }}次
              </el-tag>
              <!--
                拼团方式标签
                groupType: 0=自动成团, 1=达成目标拼团
                【前端概念】三元运算符根据 groupType 显示不同文字
                【类比后端】相当于 Java 的 groupType == 0 ? "自动成团" : "达标成团"
              -->
              <el-tag size="small" effect="plain">
                {{ activity.groupType === 0 ? '自动成团' : '达标成团' }}
              </el-tag>
            </div>

            <!-- ====== 活动时间信息 ====== -->
            <!--
              【前端概念】格式化显示活动起止时间 + 剩余时间
              - formatDateTime(): 将 ISO 时间格式化为易读形式
              - getRemainingTime(): 计算 endTime 与当前时间的差值
              - 【类比后端】相当于 Java 的：
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(activity.getStartTime())
                Duration.between(LocalDateTime.now(), activity.getEndTime())
            -->
            <div class="activity-time">
              <span class="time-text">
                &#x1F4C5; {{ formatDateTime(activity.startTime) }} 至 {{ formatDateTime(activity.endTime) }}
              </span>
              <!--
                剩余时间标签
                - 只有活动状态为"生效"(status=1)时才显示剩余时间
                - 【前端概念】v-if 条件渲染：只在活动生效时显示倒计时
              -->
              <el-tag
                v-if="activity.status === 1"
                size="small"
                :type="getRemainingTimeType(activity.endTime)"
                effect="dark"
                round
              >
                {{ getRemainingTime(activity.endTime) }}
              </el-tag>
            </div>

            <!-- ====== 商品列表（内层 v-for 嵌套循环） ====== -->
            <!--
              【核心改动 - 嵌套 v-for 循环】
              【前端概念】
                - 这是本次改造的重点：在 activity 卡片内部，再用一个 v-for 遍历 goods 数组
                - 外层 v-for: activity in activities → 遍历活动
                - 内层 v-for: goods in activity.goods → 遍历活动下的商品
                - 【类比后端】相当于 Java 的嵌套循环：
                  for (AllActivitiesResponseDTO activity : activities) {
                      for (ActivityGoods goods : activity.getGoods()) {
                          // 渲染每个商品
                      }
                  }
                - 这就是"一对多"关系在前端的典型渲染模式
            -->
            <div class="goods-list">
              <div class="goods-list-title">商品列表</div>
              <div class="goods-grid">
                <!--
                  【内层 v-for 渲染商品卡片】
                  - v-for="goods in activity.goods" 遍历当前活动下的商品列表
                  - :key="goods.goodsId" 用 goodsId 作为唯一标识
                  - 【类比后端】相当于内层循环：
                    for (ActivityGoods goods : activity.getGoods()) { ... }

                  【前端概念】嵌套 v-for 中的事件处理
                  - @click="goToDetail(activity, goods)" 同时传递活动和商品信息
                  - 需要 activity 是因为跳转时要传 activityId
                  - 需要 goods 是因为跳转时要传 goodsId 和展示商品信息
                -->
                <div
                  v-for="goods in activity.goods"
                  :key="goods.goodsId"
                  class="goods-item"
                >
                  <!-- 商品名称 -->
                  <div class="goods-name">{{ goods.goodsName }}</div>
                  <!--
                    商品价格
                    【前端概念】.toFixed(2) 保留两位小数
                    【类比后端】相当于 Java 的 String.format("%.2f", goods.getOriginalPrice())
                  -->
                  <div class="goods-price">&#xA5;{{ goods.originalPrice.toFixed(2) }}</div>
                  <!--
                    参团按钮
                    - @click.stop 阻止事件冒泡（不触发父元素的活动卡片点击）
                    - 跳转到详情页，传递 activityId 和 goodsId
                  -->
                  <el-button
                    type="primary"
                    size="small"
                    round
                    @click.stop="goToDetail(activity, goods)"
                  >
                    &#x1F680; 参团
                  </el-button>
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <!-- ====== 4.3 空状态（没有活动时显示） ====== -->
      <!--
        【el-empty 组件】Element Plus 的空状态组件，展示一个"暂无数据"的图标和文字
        【类比后端】相当于 if (list.isEmpty()) { 显示空状态提示 }
        description 属性设置空状态的文字描述
      -->
      <el-empty
        v-else
        description="暂无可用活动"
      />
    </main>
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
 * 本组件职责（改造后）：
 *   1. 页面加载时调用 queryAllActivities() 获取所有活动列表（类比 @PostConstruct）
 *   2. 用双层 v-for 嵌套循环渲染活动卡片和商品列表
 *      - 外层遍历活动（activities）
 *      - 内层遍历商品（activity.goods）
 *      - 【类比后端】嵌套 for 循环处理一对多关系
 *   3. 点击商品的"参团"按钮跳转到详情页，传递 activityId 和 goodsId
 *   4. 提供时间格式化、剩余时间计算等工具方法
 */

// =============================================
// 一、导入依赖（类比 Java 的 import 语句）
// =============================================

import { ref, onMounted } from 'vue'
// 【前端概念】从 vue 中导入响应式 API 和生命周期钩子
// 【类比后端】相当于 import javax.annotation.PostConstruct; 等
//
// 各 API 说明：
// - ref:          创建响应式变量（类比 AtomicReference，修改值触发页面重渲染）
// - onMounted:    生命周期钩子，组件挂载到 DOM 后执行（类比 @PostConstruct）

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

import { queryAllActivities } from '@/api/index'
// 【前端概念】引入查询所有活动的 API 函数
// 【类比后端】相当于 @Autowired IMarketIndexService marketIndexService;
// 调用 queryAllActivities() 就是调用后端接口获取所有活动列表

import type { AllActivitiesResponseDTO, ActivityGoods, QueryAllActivitiesRequestDTO } from '@/api/types'
// 【前端概念】引入类型定义（只用于类型检查，编译后擦除）
// 【类比后端】相当于 import DTO 类，用于声明变量类型
// 【重要改动】新增 ActivityGoods 类型，用于内层 v-for 循环中的商品数据

// =============================================
// 二、获取路由和 Store 实例（类比依赖注入）
// =============================================

/**
 * 获取路由实例
 * 【类比后端】相当于获取 HttpServletResponse 对象用于重定向
 *   - HttpServletResponse response = ...;
 *   - response.sendRedirect("/detail?activityId=xxx");
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
// 三、响应式状态（类比 Java 类的成员变量）
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
 * 活动列表数据
 * 【类比后端】相当于 private List<AllActivitiesResponseDTO> activities;
 *   - 存储从后端接口获取的所有活动数据
 *   - 初始值为空数组 []，API 调用成功后赋值
 *
 * 【泛型说明】
 *   - ref<AllActivitiesResponseDTO[]>([])
 *   - [] 表示初始值是空数组
 *   - 【类比后端】相当于 List<AllActivitiesResponseDTO> activities = new ArrayList<>();
 *
 * 【嵌套数据结构】
 *   - 每个 AllActivitiesResponseDTO 内部包含 goods: ActivityGoods[]
 *   - 形成一对多的嵌套结构：Activity 1:N Goods
 *   - 【类比后端】相当于 Java 的聚合关系（Aggregation）
 */
const activities = ref<AllActivitiesResponseDTO[]>([])

// =============================================
// 四、数据加载（类比后端 Service 调用）
// =============================================

/**
 * 加载活动列表数据
 *
 * 【类比后端】相当于 @PostConstruct 初始化方法：
 *   @PostConstruct
 *   public void init() {
 *       try {
 *           QueryAllActivitiesRequestDTO request = new QueryAllActivitiesRequestDTO();
 *           request.setUserId(userStore.getUsername());
 *           this.activities = marketIndexService.queryAllActivities(request);
 *       } catch (Exception e) {
 *           this.errorMsg = "加载活动列表失败";
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
async function loadActivities() {
  // 开始加载：设置 loading 状态为 true，清空错误信息和活动列表
  loading.value = true
  error.value = ''

  try {
    // 【关键调用】构造请求参数
    // 【类比后端】相当于：
    //   QueryAllActivitiesRequestDTO request = new QueryAllActivitiesRequestDTO();
    //   request.setUserId(userStore.getUsername());
    //   request.setSource("s01");
    //   request.setChannel("c01");
    const request: QueryAllActivitiesRequestDTO = {
      userId: userStore.username || 'guest',  // 当前用户名，未登录则为 'guest'
      source: 's01',                           // 来源（可选参数，传默认值）
      channel: 'c01'                           // 渠道（可选参数，传默认值）
    }

    // 【关键调用】调用 API 函数获取所有活动列表
    // 【类比后端】相当于：
    //   List<AllActivitiesResponseDTO> result = marketIndexService.queryAllActivities(request);
    //
    // await 会等待 Promise 完成（后端响应返回），期间不阻塞 UI
    // 返回值是 AllActivitiesResponseDTO[]（响应拦截器已经解包了 ApiResponse 外层）
    //
    // 【嵌套数据结构说明】
    // 每个 AllActivitiesResponseDTO 对象内部包含 goods 字段：
    // {
    //   activityId: 100123,
    //   activityName: "双11精选好物拼团",
    //   goods: [                    ← 嵌套的商品列表（一对多关系）
    //     { goodsId: "9890001", goodsName: "...", originalPrice: 99.00 },
    //     { goodsId: "9890002", goodsName: "...", originalPrice: 79.00 }
    //   ]
    // }
    const response = await queryAllActivities(request)

    // 请求成功：将数据赋值给响应式变量
    // 【重要】赋值后，模板中 v-for="activity in activities" 会自动重新渲染
    // 内层的 v-for="goods in activity.goods" 也会自动渲染每个活动下的商品
    // 【类比后端】相当于 model.addAttribute("activities", response);
    activities.value = response

  } catch (err) {
    // 请求失败：记录错误信息，显示友好的错误提示
    // 【类比后端】相当于 catch (Exception e) { log.error("加载失败", e); }
    console.error('加载活动列表失败:', err)
    error.value = '加载活动列表失败，请稍后重试'

    // 【前端概念】ElMessage.error() 弹出错误提示消息
    // 【类比后端】相当于 response.sendErrorMessage("加载失败")
    ElMessage.error('加载活动列表失败')

  } finally {
    // 无论成功还是失败，都关闭 loading 状态
    // 【类比后端】相当于 finally { this.loading = false; }
    loading.value = false
  }
}

// =============================================
// 五、事件处理方法（类比 Controller 的 @PostMapping 方法）
// =============================================

/**
 * 跳转到活动详情页
 *
 * 【前端概念】router.push() 编程式导航（用 JS 代码控制路由跳转）
 *   - 与 <router-link to="/detail"> 效果相同，但可以在事件处理函数中动态调用
 *   - query 参数会拼接到 URL：/detail?activityId=xxx&goodsId=xxx
 *   - 【类比后端】相当于 RedirectView("/detail").addAttribute("activityId", xxx)
 *
 * 【重要改动】现在需要同时传递 activityId 和 goodsId
 * - 因为一个活动下有多个商品，用户点击的是具体商品的"参团"按钮
 * - 所以跳转时需要知道：哪个活动（activityId）+ 哪个商品（goodsId）
 *
 * @param activity 被点击的活动数据（AllActivitiesResponseDTO 类型）
 * @param goods    被点击的商品数据（ActivityGoods 类型）
 */
function goToDetail(activity: AllActivitiesResponseDTO, goods: ActivityGoods) {
  // 【路由跳转】跳转到详情页，通过 query 参数传递活动ID和商品ID
  // 【类比后端】相当于 response.sendRedirect("/detail?activityId=" + activityId + "&goodsId=" + goodsId)
  router.push({
    name: 'detail',  // 使用路由名称跳转（在 router/index.ts 中定义 name: 'detail'）
    query: {
      activityId: String(activity.activityId),  // 活动ID（转为字符串，与路由参数格式一致）
      goodsId: goods.goodsId,                    // 商品ID（来自内层 v-for 循环的商品数据）
      // 以下参数可选，方便详情页直接使用（避免再次请求后端）
      goodsName: goods.goodsName,                // 商品名称
      originalPrice: String(goods.originalPrice) // 商品原价（转为字符串）
    }
  })
}

/**
 * 处理"我的订单"按钮点击（保留不变）
 *
 * 【前端概念】直接跳转到订单列表页
 * 【类比后端】相当于 response.sendRedirect("/orders")
 */
function goToOrders() {
  router.push({ path: '/orders' })
}

/**
 * 处理退出登录（保留不变）
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
// 六、工具方法（类比后端的工具类 / Helper 方法）
// =============================================

/**
 * 格式化日期时间
 *
 * 【前端概念】Date 对象 + 手动格式化
 * - new Date(isoString) 将 ISO 8601 字符串转为 Date 对象
 * - 手动拼接 yyyy-MM-dd HH:mm 格式
 * - 【类比后端】相当于 Java 的：
 *   DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
 *       .format(LocalDateTime.parse(isoString))
 *
 * @param isoString ISO 8601 格式的时间字符串，如 "2026-11-01T00:00:00"
 * @returns 格式化后的字符串，如 "2026-11-01 00:00"
 */
function formatDateTime(isoString: string): string {
  if (!isoString) return '--'
  const date = new Date(isoString)
  // 检查日期是否有效（Invalid Date 的 getTime() 返回 NaN）
  if (isNaN(date.getTime())) return '--'
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')  // 月份从 0 开始，需要 +1
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

/**
 * 格式化拼团有效时长
 *
 * 【前端概念】分钟转小时/天的格式化
 * - 小于 60 分钟 → 显示 "XX分钟"
 * - 小于 1440 分钟（24小时）→ 显示 "X小时"
 * - 大于等于 1440 分钟 → 显示 "X天"
 * - 【类比后端】相当于 Java 的 Duration 格式化
 *
 * @param minutes 分钟数
 * @returns 格式化后的时长描述
 */
function formatValidTime(minutes: number): string {
  if (minutes < 60) {
    return `${minutes}分钟`
  } else if (minutes < 1440) {
    const hours = Math.floor(minutes / 60)
    return `${hours}小时`
  } else {
    const days = Math.floor(minutes / 1440)
    return `${days}天`
  }
}

/**
 * 计算剩余时间
 *
 * 【前端概念】时间差计算
 * - new Date(endTime).getTime() 获取结束时间的毫秒时间戳
 * - Date.now() 获取当前时间的毫秒时间戳
 * - 差值转为可读的"剩余X天X小时"格式
 * - 【类比后端】相当于 Java 的：
 *   Duration remaining = Duration.between(LocalDateTime.now(), activity.getEndTime());
 *   remaining.toDays() / remaining.toHours()
 *
 * @param endTime 活动结束时间（ISO 8601 格式）
 * @returns 剩余时间描述字符串
 */
function getRemainingTime(endTime: string): string {
  if (!endTime) return ''
  const end = new Date(endTime).getTime()
  const now = Date.now()
  const diff = end - now  // 毫秒差值

  // 已过期
  if (diff <= 0) return '已过期'

  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))

  if (days > 0) {
    return `剩余${days}天${hours}小时`
  } else if (hours > 0) {
    return `剩余${hours}小时${minutes}分钟`
  } else {
    return `剩余${minutes}分钟`
  }
}

/**
 * 根据剩余时间返回标签类型（颜色）
 *
 * 【前端概念】根据条件返回不同的 el-tag type 属性
 * - 剩余不足 1 天 → "danger"（红色，表示紧急）
 * - 剩余不足 3 天 → "warning"（橙色，表示注意）
 * - 其他 → "success"（绿色，表示充裕）
 * - 【类比后端】相当于 Java 的 if-else 返回不同枚举值
 *
 * @param endTime 活动结束时间
 * @returns el-tag 的 type 属性值
 */
function getRemainingTimeType(endTime: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  if (!endTime) return 'info'
  const diff = new Date(endTime).getTime() - Date.now()
  if (diff <= 0) return 'info'
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days < 1) return 'danger'
  if (days < 3) return 'warning'
  return 'success'
}

/**
 * 获取活动状态的文字描述
 *
 * 【前端概念】数字枚举转文字
 * - status: 0=创建, 1=生效, 2=过期, 3=废弃
 * - 【类比后端】相当于 Java 的枚举或 switch-case：
 *   switch (activity.getStatus()) {
 *       case 0: return "创建中";
 *       case 1: return "进行中";
 *       ...
 *   }
 *
 * @param status 活动状态数字
 * @returns 状态文字描述
 */
function getActivityStatusText(status: number): string {
  const statusMap: Record<number, string> = {
    0: '创建中',
    1: '进行中',
    2: '已过期',
    3: '已废弃'
  }
  return statusMap[status] || '未知'
}

/**
 * 获取活动状态的标签颜色类型
 *
 * 【前端概念】数字枚举对应不同的 el-tag type
 * - 0=创建(info), 1=生效(success), 2=过期(warning), 3=废弃(danger)
 * - 【类比后端】相当于根据状态枚举返回不同的样式标识
 *
 * @param status 活动状态数字
 * @returns el-tag 的 type 属性值
 */
function getActivityStatusType(status: number): '' | 'success' | 'warning' | 'danger' | 'info' {
  const typeMap: Record<number, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    0: 'info',
    1: 'success',
    2: 'warning',
    3: 'danger'
  }
  return typeMap[status] || 'info'
}

// =============================================
// 七、生命周期钩子（类比 Spring Bean 的生命周期回调）
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
 */
onMounted(() => {
  // 【类比后端】相当于 @PostConstruct 方法：
  //   @PostConstruct
  //   public void init() {
  //       loadActivities();  // 1. 加载所有活动列表
  //   }
  loadActivities()  // 调用 API 获取所有活动列表
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
  /* 【类比后端】相当于 CSS 的 height: 100%，但 vh 不受父元素的影响 */
  background: #f5f7fa;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ====== 顶部导航栏（保持不变） ====== */
.top-nav {
  position: sticky;
  /* 【前端概念】sticky 定位：滚动时固定在顶部（类似手机 APP 的顶部导航） */
  /* 【类比后端】相当于 CSS position: fixed 的改良版，初始按正常流排列 */
  top: 0;
  z-index: 100;
  /* 【前端概念】z-index 控制层叠顺序，数值越大越在上层 */
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

/* ====== Loading 加载状态（保持不变） ====== */
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

/* ====== 错误状态（保持不变） ====== */
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
  max-width: 900px;
  /* 【改动】适配卡片列表，需要足够宽度 */
  margin: 0 auto;
  /* 【前端概念】margin: 0 auto 水平居中（auto 自动计算左右 margin） */
  padding: 20px 16px;
}

/* ====== 页面标题区 ====== */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 0 4px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin: 0;
}

/* ====== 活动列表容器（纵向排列活动卡片） ====== */
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  /* 【前端概念】flex 纵向排列，每个活动卡片之间间隔 20px */
}

/* ====== 活动卡片外层容器 ====== */
.activity-card-wrapper {
  /* 每个活动卡片的包装器 */
}

/* ====== 活动卡片样式 ====== */
.activity-card {
  border-radius: 12px;
  /* 【前端概念】border-radius 圆角效果 */
  transition: all 0.3s ease;
  /* 【前端概念】transition 定义属性变化时的过渡动画效果 */
}

.activity-card:hover {
  /* 【前端概念】:hover 伪类：鼠标悬停时触发 */
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

/* ====== 活动头部信息（名称 + 状态标签） ====== */
.activity-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.activity-name {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
  margin: 0;
}

/* ====== 活动详情信息行（标签组） ====== */
.activity-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
  /* 【前端概念】flex-wrap: wrap 允许标签换行显示（防止一行排不下） */
}

/* ====== 活动时间信息 ====== */
.activity-time {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 8px;
}

.time-text {
  font-size: 13px;
  color: #606266;
}

/* ====== 商品列表区域（嵌套 v-for 渲染的商品网格） ====== */
.goods-list {
  border-top: 1px solid #ebeef5;
  /* 【前端概念】border-top 上边框分隔线，区分活动信息和商品列表 */
  padding-top: 16px;
}

.goods-list-title {
  font-size: 14px;
  font-weight: 600;
  color: #909399;
  margin-bottom: 12px;
}

/* ====== 商品网格布局（CSS Grid） ====== */
.goods-grid {
  display: grid;
  /* 【前端概念】CSS Grid 布局：将子元素排列成网格 */
  /* 【类比后端】相当于 Java 的 GridLayout（网格布局管理器） */
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  /* 【关键属性解释】
     - repeat(auto-fill, ...) 自动填充列数（屏幕宽就排多列，窄就排少列）
     - minmax(200px, 1fr) 每列最小 200px，最大平分剩余空间
     - 【类比后端】相当于 CSS 的响应式布局，自动适配不同屏幕尺寸
  */
  gap: 12px;
  /* 【前端概念】gap 设置网格间距（行间距和列间距） */
}

/* ====== 单个商品卡片样式 ====== */
.goods-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 12px;
  background: #fafbfc;
  border-radius: 10px;
  border: 1px solid #ebeef5;
  transition: all 0.2s ease;
  /* 【前端概念】transition 过渡动画，悬停时有平滑变化效果 */
}

.goods-item:hover {
  /* 【前端概念】:hover 伪类 + transform 轻微上浮效果 */
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #409EFF;
}

.goods-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  text-align: center;
  /* 【前端概念】text-align: center 文字居中 */
}

.goods-price {
  font-size: 20px;
  font-weight: 700;
  color: #f56c6c;
  /* 【前端概念】红色大字突出显示价格 */
  margin-bottom: 12px;
}

/* ====== 响应式适配（小屏幕） ====== */
@media (max-width: 480px) {
  /* 【前端概念】@media 媒体查询：屏幕宽度 <= 480px 时应用这些样式 */
  /* 【类比后端】相当于 if (screenWidth <= 480) { 应用移动端样式 } */
  .page-title {
    font-size: 18px;
  }

  .goods-grid {
    grid-template-columns: 1fr;
    /* 小屏幕时商品网格只显示一列 */
  }

  .goods-item {
    padding: 12px 8px;
  }

  .activity-time {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
