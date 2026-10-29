/**
 * main.ts - 应用入口文件
 * ======================
 * 【前端概念】这是整个前端应用的启动入口，相当于后端的 SpringBootApplication 主类
 * 【类比后端】
 *   - createApp() → new SpringApplication().run() 启动应用
 *   - app.use(Router) → @Import 引入路由模块
 *   - app.use(Pinia) → @Import 引入状态管理
 *   - app.use(ElementPlus) → 引入 UI 组件库（类比引入一套现成的页面模板）
 *   - app.mount('#app') → 将 Vue 应用挂载到 index.html 的 #app 元素上
 */
import { createApp } from 'vue'           // Vue 3 的应用创建函数（类比 SpringApplication）
import { createPinia } from 'pinia'       // Pinia 状态管理（类比后端的 Session/ThreadLocal）
import ElementPlus from 'element-plus'    // Element Plus UI 组件库
import 'element-plus/dist/index.css'      // Element Plus 样式
import * as ElementPlusIconsVue from '@element-plus/icons-vue'  // 图标库
import App from './App.vue'               // 根组件（类比 Spring 的根容器 ApplicationContext）
import router from './router'             // 路由配置（类比 Spring MVC 的 @RequestMapping 集合）
import { useUserStore } from './stores/user'  // 用户状态 Store（类比 @Autowired UserService）

// 创建 Vue 应用实例（类比 SpringApplication 实例）
const app = createApp(App)

// 创建 Pinia 实例（类比创建一个新的 SessionManager）
// 【注意】必须先创建再 use，因为后面需要用它来获取 Store
const pinia = createPinia()

// 注册插件（类比 Spring Boot 的自动配置 @EnableXxx）
app.use(pinia)              // 启用状态管理（全局共享数据，类比 Redis 缓存）
app.use(router)             // 启用路由（URL → 页面组件的映射）
app.use(ElementPlus)        // 启用 UI 组件库（按钮、表格、表单等现成组件）

/**
 * 从 localStorage 恢复用户登录状态
 * 【类比后端】相当于应用启动时从 Redis/数据库恢复 Session 数据
 *
 * 【为什么需要这一步】
 * Pinia 中的 state 存储在内存中，页面刷新后内存被清空，数据会丢失
 * 但 localStorage 中的数据是持久化的（类比 Redis 持久化）
 * 所以需要在应用启动时，从 localStorage 恢复到 Pinia 中
 *
 * 【执行时机】
 * 在 app.mount() 之前执行，确保组件渲染时数据已经恢复
 * 类比 Spring：相当于在 ApplicationRunner 中做初始化，在接收请求前完成
 */
const userStore = useUserStore()   // 获取用户 Store 实例（必须在 pinia 注册到 app 之后）
userStore.initFromStorage()        // 从 localStorage 恢复用户名

// 注册所有图标（全局注册，任何组件都能用）
// 【类比后端】相当于把图标组件注册到 Spring 容器中，任何地方都能 @Autowired 注入使用
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 挂载应用到 DOM（类比 Spring Boot 启动完成后监听端口，开始接收请求）
// '#app' 对应 index.html 中的 <div id="app"></div>
app.mount('#app')
