/**
 * stores/user.ts - 用户状态管理
 * =============================
 * 【前端概念】Pinia Store = 全局共享的数据仓库，任何组件都能读取/修改这里的数据
 * 【类比后端】
 *   - 相当于一个全局的 @Component + @Scope("singleton") 的 Java Bean
 *   - 或者理解为后端的 ThreadLocal / HttpSession，存储当前用户信息
 *   - state   → Java 类的字段（成员变量）
 *   - getters → Java 类的 getter 方法 / 计算属性（不修改数据，只返回派生值）
 *   - actions → Java 类的方法（可以修改 state，可以包含异步操作）
 *
 * 【为什么需要 Pinia】
 * 1. 组件间共享数据：登录用户名需要在多个页面显示（首页、订单页等）
 * 2. 数据持久化：配合 localStorage，刷新页面后数据不丢失
 *    （类比后端的 Session 持久化到 Redis）
 * 3. 响应式：数据变化时，使用它的页面自动更新
 *    （类比 WebSocket 推送，数据变了前端自动刷新）
 *
 * 【Pinia 4.x 用法说明】
 * - defineStore() 第一个参数是 store 的唯一 id（类比 Spring Bean 的 name）
 * - 使用 Setup Store 风格（函数式），更贴近 Vue 3 Composition API
 */

import { ref, computed } from 'vue'    // Vue 3 响应式 API
import { defineStore } from 'pinia'    // Pinia 的核心函数

/**
 * 定义用户 Store
 *
 * 【类比后端】相当于创建一个 UserService 单例服务：
 *   - ref() 定义的变量 → 类的私有字段
 *   - computed() 定义的值 → 类的 getter 方法
 *   - 普通函数 → 类的 public 方法
 *
 * defineStore 的 Setup Store 风格（推荐）：
 *   第一个参数 'user' 是 store id，必须唯一
 *   第二个参数是一个 setup 函数，返回需要暴露给外部的 state/getters/actions
 */
export const useUserStore = defineStore('user', () => {
  // ==================== State（状态/数据） ====================

  /**
   * 用户名 - 响应式状态
   * 【类比后端】相当于 private String username; 字段
   *
   * 【前端概念】ref() 是 Vue 3 的响应式数据包装器
   * - ref('') 创建一个初始值为空字符串的响应式引用
   * - 在 JS 中需要通过 .value 访问/修改（如 username.value = '张三'）
   * - 在 template 中会自动解包，不需要 .value
   *
   * 为什么用 ref 而不是普通变量？
   * - 普通变量赋值后，页面不会自动更新
   * - ref 包装后，值变化时所有使用它的地方自动重新渲染
   * - 类比后端的 Observable/EventBus，数据变了通知所有订阅者
   */
  const username = ref('')

  // ==================== Getters（计算属性/派生状态） ====================

  /**
   * 是否已登录 - 计算属性
   * 【类比后端】相当于 public boolean isLoggedIn() { return username != null; }
   *
   * 【前端概念】computed() 是计算属性，基于其他响应式数据派生出新值
   * - 只有依赖的数据变化时才会重新计算（有缓存，不会每次都算）
   * - 类比后端的 getter 方法，但更智能（自动缓存）
   */
  const isLoggedIn = computed(() => !!username.value)

  // ==================== Actions（操作/方法） ====================

  /**
   * 登录 - 保存用户名
   * 【类比后端】相当于 login(String username) 方法：
   *   1. 将用户信息存入 HttpSession（这里存到 localStorage）
   *   2. 更新内存中的状态（这里更新 ref）
   *
   * @param name 用户名
   *
   * 【localStorage 说明】
   * - localStorage 是浏览器提供的本地存储 API
   * - 数据以键值对形式存储，永久保存（除非手动清除）
   * - 类比后端的 Cookie，但容量更大（5MB vs 4KB），且不会随请求发送
   */
  function login(name: string) {
    // 1. 存入浏览器本地存储（持久化，刷新页面不丢失）
    // 【类比】session.setAttribute("username", name)
    localStorage.setItem('username', name)
    // 2. 更新响应式状态（所有使用 username 的组件自动更新）
    // 【类比】this.username = name（但 ref 需要用 .value）
    username.value = name
  }

  /**
   * 登出 - 清除用户信息
   * 【类比后端】相当于 session.invalidate() + 清空 ThreadLocal
   */
  function logout() {
    // 1. 清除本地存储
    localStorage.removeItem('username')
    // 2. 重置响应式状态
    username.value = ''
  }

  /**
   * 从本地存储恢复状态
   * 【类比后端】相当于应用重启时从 Redis/数据库恢复 Session 数据
   *
   * 【使用场景】
   * 用户刷新页面后，Pinia 中的 state 会丢失（因为内存被清空）
   * 但 localStorage 中的数据还在，所以需要在应用启动时从中恢复
   * 这个方法在 main.ts 中调用
   */
  function initFromStorage() {
    const stored = localStorage.getItem('username')
    if (stored) {
      username.value = stored
    }
  }

  // ==================== 返回值 ====================

  /**
   * 返回需要暴露给外部使用的内容
   * 【类比后端】相当于类的 public 接口
   * 没有在 return 中暴露的内容，外部无法访问（私有）
   */
  return {
    username,        // state: 用户名
    isLoggedIn,      // getter: 是否已登录
    login,           // action: 登录
    logout,          // action: 登出
    initFromStorage  // action: 从存储恢复
  }
})
