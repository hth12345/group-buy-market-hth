<!--
  LoginView.vue - 登录页面（重构版）
  ========================
  【重构说明】按照 docs/ui/html/login.html 参考设计重构
  - 渐变紫色背景（#667eea → #764ba2）
  - 浮动标签输入框（label 在 focus 时上移）
  - 简洁卡片式设计

  【前端概念】Vue 单文件组件（SFC）= 模板 + 逻辑 + 样式
  【类比后端】
    - <template>  → JSP/Thymeleaf 页面模板
    - <script>    → Controller 业务方法
    - <style>     → CSS 样式文件
-->

<template>
  <div class="container">
    <!--
      登录表单
      【前端概念】@submit.prevent 阻止表单默认提交行为（页面刷新）
      【类比后端】相当于 <form onsubmit="event.preventDefault(); handleLogin();">
    -->
    <form class="login-form" @submit.prevent="handleLogin">
      <h2>欢迎登录 - 拼团商城</h2>

      <!--
        用户名输入框组 - 浮动标签效果
        【前端概念】CSS :focus-within 伪类 + ~ 兄弟选择器实现浮动标签
        - 输入框获焦或有内容时，label 向上浮动
        - 【类比后端】相当于 CSS 的交互效果，类似前端动画
      -->
      <div class="input-group">
        <input
          type="text"
          id="username"
          v-model="form.username"
          required
        />
        <label for="username">用户名</label>
      </div>

      <!--
        密码输入框组（预留，当前系统不需要密码）
        【注意】当前是演示系统，只输入用户名即可登录
        如果需要密码，可以取消注释并添加相应逻辑
      -->
      <!--
      <div class="input-group">
        <input
          type="password"
          id="password"
          v-model="form.password"
          required
        />
        <label for="password">密码</label>
      </div>
      -->

      <!--
        登录按钮
        【前端概念】type="submit" 提交表单
        :disabled 禁用状态（用户名为空时禁用）
      -->
      <button type="submit" :disabled="!form.username.trim() || loading">
        {{ loading ? '登录中...' : '登录' }}
      </button>

      <!--
        错误信息
        【前端概念】v-if 条件渲染，有错误时才显示
        【类比后端】相当于 if (errorMsg != null) { 显示错误; }
      -->
      <p class="error-message" v-if="errorMsg">{{ errorMsg }}</p>
    </form>
  </div>
</template>

<script setup lang="ts">
/**
 * <script setup> 部分 - 登录页业务逻辑
 * 【类比后端】相当于 LoginController 类
 */

import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'

// 获取路由和 Store 实例
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 表单数据
const form = reactive({
  username: ''
})

// 加载状态
const loading = ref(false)

// 错误信息
const errorMsg = ref('')

// 组件挂载后检查是否已登录
onMounted(() => {
  if (userStore.isLoggedIn) {
    router.push({ name: 'home' })
  }
})

/**
 * 处理登录
 * 【类比后端】相当于 @PostMapping("/login") 方法
 */
async function handleLogin() {
  const name = form.username.trim()

  if (!name) {
    errorMsg.value = '请输入用户名'
    return
  }

  loading.value = true
  errorMsg.value = ''

  try {
    userStore.login(name)
    const redirectPath = (route.query.redirect as string) || '/'
    router.push(redirectPath)
  } catch (error) {
    errorMsg.value = '登录失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/**
 * <style scoped> - 登录页样式（按照 login.css 重构）
 * 【前端概念】scoped 表示样式只对当前组件生效
 * 【类比后端】相当于 private 修饰符，只在当前类中有效
 */

/* 导入 Google 字体（与参考设计一致） */
@import url("https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;500;600&display=swap");

/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/*
  页面容器 - 渐变背景 + 居中布局
  【前端概念】linear-gradient 线性渐变
  【类比后端】相当于 CSS 的背景色设置，但是渐变色
*/
.container {
  font-family: "Poppins", sans-serif;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

/*
  登录表单卡片
  【前端概念】白色卡片 + 圆角 + 阴影
*/
.login-form {
  background-color: #ffffff;
  padding: 40px;
  border-radius: 10px;
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.1);
  width: 400px;
  max-width: 90%;
}

/* 表单标题 */
.login-form h2 {
  text-align: center;
  color: #333;
  margin-bottom: 30px;
}

/*
  输入框组 - 浮动标签效果
  【前端概念】position: relative 用于定位 label
  【类比后端】相当于 CSS 的定位机制，为子元素提供参考点
*/
.input-group {
  position: relative;
  margin-bottom: 30px;
}

/* 输入框样式 */
.input-group input {
  width: 100%;
  padding: 10px 0;
  font-size: 16px;
  color: #333;
  border: none;
  border-bottom: 1px solid #ddd;
  outline: none;
  background: transparent;
  transition: 0.3s;
}

/*
  浮动标签 - 初始位置
  【前端概念】label 覆盖在输入框上方
*/
.input-group label {
  position: absolute;
  top: 0;
  left: 0;
  padding: 10px 0;
  font-size: 16px;
  color: #666;
  pointer-events: none;
  transition: 0.3s;
}

/*
  浮动标签 - 获焦或有内容时上移
  【前端概念】~ 兄弟选择器 + :focus / :valid 伪类
  - input:focus ~ label → 输入框获焦时，label 上移
  - input:valid ~ label → 输入框有内容时，label 上移
  - 【类比后端】相当于 CSS 的状态切换，类似 Java 的事件监听
*/
.input-group input:focus ~ label,
.input-group input:valid ~ label {
  top: -20px;
  font-size: 12px;
  color: #667eea;
}

/* 输入框获焦时底部边框变色 */
.input-group input:focus {
  border-bottom: 2px solid #667eea;
}

/* 登录按钮样式 */
button {
  width: 100%;
  padding: 12px;
  background: #667eea;
  color: #fff;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 16px;
  transition: 0.3s;
}

/* 按钮悬停效果 */
button:hover {
  background: #764ba2;
}

/* 按钮禁用状态 */
button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 错误信息样式 */
.error-message {
  color: #ff3860;
  font-size: 14px;
  text-align: center;
  margin-top: 10px;
}
</style>
