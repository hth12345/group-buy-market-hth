/**
 * router/index.ts - 路由配置
 * ==========================
 * 【前端概念】路由 = URL 路径与页面组件的映射关系
 * 【类比后端】相当于 Controller 中的 @RequestMapping 注解
 *   - @GetMapping("/home") → { path: '/home', component: HomeView }
 *   - 前端路由不刷新页面，通过 JS 动态切换组件（单页应用 SPA）
 * 
 * 【关键概念】
 * - path: URL 路径（类比 @RequestMapping 的 value）
 * - component: 对应的页面组件（类比 Controller 方法返回的视图名）
 * - meta: 路由元信息（类比自定义注解，可以附加额外配置）
 * - beforeEnter: 路由守卫（类比拦截器/过滤器 Interceptor/Filter）
 */
import { createRouter, createWebHistory } from 'vue-router'

// 创建路由实例
// 【类比后端】相当于创建 DispatcherServlet，管理所有 URL 到处理器的映射
const router = createRouter({
  // 使用 HTML5 History 模式（URL 没有 # 号，更美观）
  // 类比：普通 URL 是 /home，Hash 模式是 /#/home
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      // 懒加载：访问这个路由时才加载组件代码（类比 Spring 的懒加载 Bean）
      // 好处：首屏只加载必要的代码，其他页面按需加载，提升性能
      component: () => import('../views/LoginView.vue'),
      meta: { requiresAuth: false }  // 不需要登录就能访问
    },
    {
      path: '/',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
      meta: { requiresAuth: true }   // 需要登录才能访问
    },
    {
      path: '/detail',
      name: 'detail',
      component: () => import('../views/DetailView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/pay',
      name: 'pay',
      component: () => import('../views/PayView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/orders',
      name: 'orders',
      component: () => import('../views/OrdersView.vue'),
      meta: { requiresAuth: true }
    }
  ]
})

/**
 * 全局前置守卫 - 每次路由跳转前执行
 * 【类比后端】相当于 Spring MVC 的 HandlerInterceptor.preHandle()
 * 检查用户是否已登录，未登录则重定向到登录页
 * 
 * @param to - 目标路由（要去哪里，类比 HttpServletRequest 的目标 Handler）
 * @param from - 来源路由（从哪来）
 * @param next - 放行函数（调用 next() 继续，类比 chain.doFilter()）
 */
router.beforeEach((to, _from, next) => {
  // 从 localStorage 获取登录状态
  // 【类比后端】localStorage 类似浏览器的本地存储，类似后端的 Cookie/Session
  const isLoggedIn = localStorage.getItem('username')
  
  // 情况1：目标路由需要登录，且用户未登录 → 拦截到登录页
  // 【类比后端】相当于 Filter 中 if (!session && requiresAuth) { response.sendRedirect("/login"); return; }
  if (to.meta.requiresAuth && !isLoggedIn) {
    // 重定向到登录页，并记录原本要去的页面（登录后跳回来）
    // 【类比后端】类似 response.sendRedirect("/login?redirect=" + request.getRequestURI())
    next({ name: 'login', query: { redirect: to.fullPath } })
    return  // return 防止继续执行后续逻辑
  }
  
  // 情况2：用户已登录，但访问登录页 → 直接跳转首页
  // 【类比后端】已登录用户不需要再看登录页，类似已认证用户访问 /login 自动重定向到首页
  if (to.name === 'login' && isLoggedIn) {
    next({ name: 'home' })
    return
  }
  
  // 情况3：其他所有情况 → 正常放行
  // 【类比后端】chain.doFilter(request, response) 放行
  next()
})

export default router
