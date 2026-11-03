# 拼团商城前端 · 零基础学习指南

> 面向 Java 后端开发者的 Vue 3 前端入门指南

---

## 一、项目概述

### 这是什么项目

拼团商城前端，对接 Java 后端（`group-buy-market-hth`），实现拼团开团、参团、锁单、支付、退单等完整业务流程的前端页面。

### 技术栈一览

| 技术 | 作用 | 后端类比 |
|---|---|---|
| **Vue 3** | UI 框架，组件化开发 | Spring Framework |
| **TypeScript** | 带类型的 JavaScript | Java 强类型检查 |
| **Vite** | 构建工具 + 开发服务器 | Maven + DevTools |
| **Element Plus** | UI 组件库（按钮/表格/弹窗） | Bootstrap / LayUI |
| **Pinia** | 全局状态管理 | HttpSession / ThreadLocal |
| **Axios** | HTTP 请求库 | RestTemplate / HttpClient |
| **Vue Router** | 路由管理 | @RequestMapping 映射 |

### 如何启动

```bash
cd group-buy-market-hth-frontend
npm install        # 安装依赖（类比 mvn install）
npm run dev        # 启动开发服务器，访问 http://localhost:5173
```

> 需要后端服务运行在 `localhost:8091`，Vite 会自动代理 `/api` 请求。

### 目录结构

```
group-buy-market-hth-frontend/
├── public/              # 静态资源（不经过编译，类比 src/main/resources/static）
├── src/
│   ├── api/             # API 接口封装（类比 Service 层调用 DAO）
│   │   ├── types.ts     #   类型定义（类比 DTO 类）
│   │   ├── request.ts   #   Axios 封装（类比 HttpClientUtil）
│   │   ├── index.ts     #   首页接口（类比 IMarketIndexService）
│   │   └── trade.ts     #   交易接口（类比 IMarketTradeService）
│   ├── components/      # 可复用组件（类比公共 UI 组件/模板片段）
│   ├── router/          # 路由配置（类比 @RequestMapping 集合）
│   ├── stores/          # 状态管理（类比 Session/ThreadLocal）
│   ├── utils/           # 工具函数（类比 XxxUtils 工具类）
│   ├── views/           # 页面组件（类比 JSP/Thymeleaf 页面）
│   ├── App.vue          # 根组件（类比 Layout 模板）
│   └── main.ts          # 入口文件（类比 @SpringBootApplication）
├── package.json         # 依赖配置（类比 pom.xml）
└── vite.config.ts       # 构建配置（类比 Maven 插件配置）
```

---

## 二、前端 vs 后端 概念对照表

| 后端概念 (Java/Spring) | 前端概念 (Vue) | 本项目中的例子 |
|---|---|---|
| `@SpringBootApplication` | `main.ts` | 应用启动入口，创建 app 并注册插件 |
| `@Controller` + `@RequestMapping` | `router/index.ts` | URL → 页面组件映射 |
| DTO 类 | `api/types.ts` | 请求/响应数据结构定义 |
| `RestTemplate` / `HttpClient` | `api/request.ts`（Axios） | HTTP 请求工具 |
| `@ControllerAdvice` 全局异常 | `request.ts` 响应拦截器 | 统一错误处理 |
| `Filter` / `HandlerInterceptor` | `router.beforeEach` 路由守卫 | 请求拦截/权限校验 |
| `HttpSession` / `ThreadLocal` | `stores/user.ts`（Pinia） | 全局用户状态 |
| `@Service` 业务逻辑 | `components/*.vue` | 可复用 UI 逻辑 |
| JSP / Thymeleaf 模板 | `views/*.vue` | 页面模板 |
| `pom.xml` | `package.json` | 依赖管理 |
| Maven | npm | 包管理工具 |
| `application.yml` | `vite.config.ts` | 项目配置（端口、代理） |
| `@PostConstruct` | `onMounted()` | 组件初始化（页面加载后执行） |
| `@PreDestroy` | `onUnmounted()` | 组件销毁清理（清除定时器等） |

---

## 三、按页面顺序的学习路线

### 第1站：登录页 (`LoginView.vue`)

**涉及概念**：Vue 组件结构（template/script/style）、v-model 双向绑定、ref 响应式变量、路由跳转

**关键代码解析**：

```typescript
// 1. ref() 创建响应式变量 —— 变量变化时页面自动更新
const form = reactive({ username: '' })
```
> `reactive` 类似 Java 的 `new HashMap()`，但数据变化时 UI 会自动刷新。

```html
<!-- 2. v-model 双向绑定 —— 输入框内容和变量实时同步 -->
<el-input v-model="form.username" placeholder="请输入用户名" />
```
> 类比 `@RequestParam` 自动绑定参数，但 v-model 是实时的。

```typescript
// 3. 路由跳转 —— 登录成功后跳到首页
router.push({ name: 'home' })
```
> 类比 `response.sendRedirect("/home")`，但不刷新页面。

```typescript
// 4. Pinia Store 登录 —— 保存用户名到全局状态 + localStorage
userStore.login(form.username)
```
> 类比 `session.setAttribute("username", name)` + Redis 持久化。

**与后端的关联**：对应后端 `LoginController`，管理用户 Session。本项目简化了后端校验，用 localStorage 模拟 Session。

**动手练习**：
1. 修改登录页标题「拼团商城」和主题色
2. 添加密码输入框（`el-input` + `type="password"`）
3. 添加"记住我"功能（延长 localStorage 有效期）

---

### 第2站：商品首页 (`HomeView.vue`)

**涉及概念**：`onMounted` 生命周期、API 调用、`v-for` 列表渲染、`computed` 计算属性、组件引入

**关键代码解析**：

```typescript
// 1. onMounted —— 页面加载完成后自动执行（类比 @PostConstruct）
onMounted(async () => {
  await loadData()  // 调用后端接口获取拼团数据
})
```

```typescript
// 2. API 调用 —— 传入参数，获取响应数据
const data = await queryGroupBuyMarketConfig({
  userId: userStore.username,
  source: 'APP', channel: 'wechat', goodsId: 'goods001'
})
```
> 类比 `restTemplate.postForObject(url, request, Response.class)`。

```html
<!-- 3. v-for 列表渲染 —— 循环渲染拼团卡片（类比 <c:forEach>） -->
<TeamCard v-for="team in teamList" :key="team.teamId" :teamInfo="team" />
```

```typescript
// 4. computed 计算属性 —— 基于已有数据派生新值（有缓存）
const sortedTeamList = computed(() =>
  [...teamList.value].sort((a, b) => a.completeCount - b.completeCount)
)
```
> 类比 Java 的 `list.stream().sorted(...).collect(toList())`，但数据变化时自动重新计算。

**与后端的关联**：对应 `IMarketIndexService.queryGroupBuyMarketConfig()`，返回商品信息和拼团列表。

**动手练习**：
1. 修改轮播图颜色或添加第4张
2. 在拼团列表中添加排序功能（按剩余人数排序）
3. 添加搜索框过滤拼团

---

### 第3站：商品详情 (`DetailView.vue`)

**涉及概念**：路由参数获取、`async/await` 异步编程、按钮防重复提交

**关键代码解析**：

```typescript
// 1. 路由参数获取 —— 从 URL 获取上一页传来的数据
const route = useRoute()
const goodsInfo = reactive({
  teamId: route.query.teamId || '',
  activityId: Number(route.query.activityId) || 0,
  goodsId: route.query.goodsId || 'goods001',
})
```
> 类比 `@RequestParam("teamId") String teamId`。

```typescript
// 2. async/await —— 等待异步操作完成（类比 CompletableFuture.get()）
const result = await lockMarketPayOrder({ ... })
```

```typescript
// 3. 防重复提交 —— 用 ref 控制按钮状态（类比后端的幂等性控制）
const isLocking = ref(false)
async function handleLockOrder() {
  if (isLocking.value) return  // 防止重复点击
  isLocking.value = true
  try { ... } finally { isLocking.value = false }
}
```
> 类比后端分布式锁防重复提交的前端版本。

**与后端的关联**：对应 `TradeController.lockMarketPayOrder()`，创建订单并锁定库存。

**动手练习**：
1. 修改订单确认区域布局
2. 添加数量选择器（`el-input-number`）
3. 添加优惠券输入框

---

### 第4站：支付确认 (`PayView.vue`)

**涉及概念**：Element Plus 弹窗组件、localStorage 持久化、多个 API 串联调用

**关键代码解析**：

```typescript
// 1. 从路由参数获取订单信息
const orderInfo = reactive({
  orderId: route.query.orderId || '',
  outTradeNo: route.query.outTradeNo || '',
  payPrice: Number(route.query.payPrice) || 0,
})
```

```typescript
// 2. 支付确认 —— 调用结算接口 + 更新本地订单状态
async function handlePayConfirm() {
  await settlementMarketPayOrder({ ... })   // 调用后端结算
  updateOrderStatus(orderInfo.outTradeNo, '已支付')  // 更新本地
  router.push({ name: 'home' })             // 跳回首页
}
```
> 多个异步操作串联：结算 → 更新本地 → 跳转，类比后端的事务链。

```typescript
// 3. 退单 —— 调用退单接口 + 更新本地状态
async function handleRefund() {
  await refundMarketPayOrder({ ... })
  updateOrderStatus(orderInfo.outTradeNo, '已退单')
}
```

**与后端的关联**：对应 `TradeController` 的结算和退单接口，完整走通支付→结算/退单链路。

**动手练习**：
1. 修改支付弹窗样式
2. 添加支付倒计时（超时自动关闭）
3. 添加支付成功动画效果

---

### 第5站：我的订单 (`OrdersView.vue`)

**涉及概念**：数组过滤/排序、条件渲染、综合应用前几站所学

**关键代码解析**：

```typescript
// 1. 数组过滤 —— 按状态筛选订单（类比 SQL WHERE）
const filteredOrders = computed(() =>
  activeFilter.value === '全部'
    ? orderList.value
    : orderList.value.filter(o => o.status === activeFilter.value)
)
```
> 类比 `orders.stream().filter(o -> o.getStatus().equals(status)).collect(toList())`。

```html
<!-- 2. 条件渲染 —— 有订单显示列表，无订单显示空状态 -->
<div v-if="filteredOrders.length > 0">...</div>
<el-empty v-else description="暂无订单" />
```

```typescript
// 3. 综合调用 —— 退单操作涉及 API 调用 + 本地状态更新 + UI 刷新
async function handleRefund(order: OrderRecord) {
  await refundMarketPayOrder({ userId: userStore.username, outTradeNo: order.outTradeNo, ... })
  updateOrderStatus(order.outTradeNo, '已退单')
  loadOrders()  // 重新加载列表
}
```

**与后端的关联**：综合了查询、退单等多个后端接口，是前端最接近真实业务的页面。

**动手练习**：
1. 添加订单统计（总金额、订单数）
2. 添加分页功能（`el-pagination`）
3. 添加订单导出功能（生成 CSV 下载）

---

## 四、核心概念速查

### 响应式系统

| 概念 | 一句话解释 | 后端类比 | 本项目使用位置 |
|---|---|---|---|
| `ref()` | 包装基本类型的响应式数据 | `private` 字段 + 通知机制 | `stores/user.ts` L56 |
| `reactive()` | 包装对象的响应式数据 | POJO 字段 + 通知机制 | `views/HomeView.vue` |
| `computed()` | 基于响应式数据派生新值（有缓存） | getter 方法 | `stores/user.ts` L68 |
| `watch()` | 监听数据变化执行副作用 | `@EventListener` | `components/CountdownTimer.vue` |

### 生命周期

| 概念 | 一句话解释 | 后端类比 | 本项目使用位置 |
|---|---|---|---|
| `onMounted()` | 组件渲染到页面后执行 | `@PostConstruct` | `views/HomeView.vue` |
| `onUnmounted()` | 组件从页面移除时执行 | `@PreDestroy` | `components/CountdownTimer.vue`（清除定时器） |

### 模板语法

| 概念 | 一句话解释 | 后端类比 | 本项目使用位置 |
|---|---|---|---|
| `v-model` | 双向绑定（变量↔输入框） | `@RequestParam` 参数绑定 | `views/LoginView.vue` |
| `v-for` | 循环渲染列表 | `<c:forEach>` / `th:each` | `views/HomeView.vue` |
| `v-if` / `v-else` | 条件渲染 | `<c:if>` / `th:if` | `views/HomeView.vue` |
| `@click` | 绑定点击事件 | `onclick` 事件处理 | 所有页面 |
| `:class` | 动态绑定 CSS 类名 | `th:class` | `components/CountdownTimer.vue` |

### 组件通信

| 概念 | 一句话解释 | 后端类比 | 本项目使用位置 |
|---|---|---|---|
| `defineProps` | 父→子传数据 | 方法参数 | `components/TeamCard.vue` |
| `defineEmits` | 子→父发事件 | 回调函数 | `components/TeamCard.vue` |
| `v-model:xxx` | 父子双向绑定 | getter + setter | `components/PayDialog.vue` |

### 路由

| 概念 | 一句话解释 | 后端类比 | 本项目使用位置 |
|---|---|---|---|
| `useRouter()` | 获取路由实例（用于跳转） | `HttpServletResponse.sendRedirect()` | `views/LoginView.vue` |
| `useRoute()` | 获取当前路由信息（用于读参数） | `HttpServletRequest.getParameter()` | `views/DetailView.vue` |
| `router.push()` | 编程式导航跳转 | `redirect:` | 所有页面 |
| `beforeEach` | 全局前置守卫 | `HandlerInterceptor.preHandle()` | `router/index.ts` L68 |

### 状态管理 (Pinia)

| 概念 | 一句话解释 | 后端类比 | 本项目使用位置 |
|---|---|---|---|
| `defineStore()` | 定义全局 Store | `@Component` 单例 Bean | `stores/user.ts` |
| `state`（ref） | Store 中的数据 | 类的私有字段 | `stores/user.ts` L56 |
| `getters`（computed） | 派生状态（只读） | getter 方法 | `stores/user.ts` L68 |
| `actions`（函数） | 修改 state 的方法 | setter / 业务方法 | `stores/user.ts` L85 |

### HTTP 请求

| 概念 | 一句话解释 | 后端类比 | 本项目使用位置 |
|---|---|---|---|
| `axios.create()` | 创建请求实例 | `HttpClient.newBuilder().build()` | `api/request.ts` L48 |
| `async/await` | 异步代码同步写法 | `CompletableFuture.get()` | `views/DetailView.vue` |
| 拦截器 | 请求/响应的统一处理 | `Filter` + `@ControllerAdvice` | `api/request.ts` L64/L102 |

---

## 五、面试指南

### 1 分钟项目介绍

> "这是一个拼团商城的前端项目，用 Vue 3 + TypeScript 开发。我负责了登录、商品首页、商品详情、支付确认、订单管理五个核心页面的开发。前端通过 Axios 对接 Java 后端，实现了完整的拼团业务流程——开团、参团、锁单、支付、退单。项目用 Pinia 做全局状态管理，Vue Router 做页面路由和权限守卫，Element Plus 做 UI 组件。"

### 常见面试问题

**Q1：为什么选择 Vue 3 而不是 Vue 2？**
> Vue 3 的 Composition API 让逻辑复用更方便（组合式函数），TypeScript 支持更好，性能更优（Proxy 响应式）。

**Q2：前端如何保证接口安全？**
> 路由守卫做页面级权限控制；Axios 拦截器统一添加 Token 头；响应拦截器统一处理 401 跳转登录。

**Q3：v-model 的实现原理？**
> 本质是 `:value` + `@input` 的语法糖。子组件通过 props 接收值，通过 emit('update:modelValue') 通知父组件更新。

**Q4：Pinia 和 Vuex 的区别？**
> Pinia 是 Vue 官方推荐的状态管理，API 更简洁，支持 Composition API 风格，TypeScript 类型推导更好，没有 mutations。

**Q5：Axios 拦截器做了什么？**
> 请求拦截器统一设置 Content-Type 和 Token；响应拦截器解包业务数据（只返回 data 部分），统一处理错误码和网络异常。

**Q6：路由守卫的执行时机？**
> `beforeEach` 在每次路由跳转前执行，检查目标路由是否需要登录（meta.requiresAuth），未登录则重定向到登录页。

**Q7：组件之间如何通信？**
> 父子组件用 props/emit；跨层级用 Pinia Store；本项目中首页和 TeamCard 用 props 传拼团数据，用 emit 传点击事件。

**Q8：如何处理异步操作？**
> 使用 async/await 配合 try/catch，比 Promise 链式调用更直观。加载状态用 ref 控制，错误信息也存入 ref 展示在页面上。

**Q9：computed 和 watch 的区别？**
> computed 是派生值（有缓存，依赖不变就不重算），适合数据转换；watch 是副作用（监听变化后执行操作），适合调接口、操作 DOM。

**Q10：项目中遇到的难点？**
> 前后端联调时的数据格式对齐（TypeScript 类型定义要和后端 DTO 严格对应）；localStorage 数据同步（多页面共享订单数据需要统一管理工具函数）。

### 全栈视角讲述

> "作为后端开发者做前端项目，让我更理解了前后端协作的完整链路。比如前端的路由守卫对应后端的拦截器，前端的 Axios 拦截器对应后端的 @ControllerAdvice，前端的 Pinia 对应后端的 Session。这种对照让我在设计后端 API 时，会更多考虑前端的使用体验。"

### 简历写法

```
拼团商城前端 | Vue 3 + TypeScript + Element Plus
- 独立完成 5 个核心页面开发，实现拼团业务全流程前端交互
- 封装 Axios 统一请求层（拦截器 + 类型定义），对接 Java 后端 4 个核心接口
- 使用 Pinia 管理全局用户状态，配合 localStorage 实现登录态持久化
- 实现路由守卫做页面级权限控制，未登录自动跳转登录页
```

---

## 六、进阶学习建议

### Vue 3 进阶
- **组合式函数（Composables）**：把组件逻辑抽成可复用函数，类比后端的 Service 抽取
- **自定义指令**：如 `v-focus`、`v-loading`，类比后端的自定义注解
- **Teleport / Suspense**：高级组件特性，处理弹窗和异步组件

### 工程化
- **ESLint + Prettier**：代码风格统一（类比后端的 Checkstyle）
- **Husky + lint-staged**：提交前自动检查（类比 Git Hook）
- **CI/CD**：GitHub Actions 自动构建部署（类比 Jenkins）

### 实战扩展
- **WebSocket**：实时推送拼团状态变化（类比后端 RabbitMQ 推送）
- **ECharts**：数据可视化大屏（展示拼团统计）
- **移动端适配**：rem/vw 方案，让页面在手机端也能良好展示

### 推荐资源
- [Vue 3 官方文档（中文）](https://cn.vuejs.org/)
- [Pinia 官方文档](https://pinia.vuejs.org/)
- [TypeScript 入门教程](https://github.com/xcatliu/typescript-tutorial)
- B站搜索「Vue 3 黑马程序员」或「Vue 3 尚硅谷」
