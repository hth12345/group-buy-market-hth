<!--
  LoginView.vue - 登录页面
  ========================
  【前端概念】Vue 单文件组件（SFC）= 一个 .vue 文件包含 模板 + 逻辑 + 样式
  【类比后端】
    - <template>  → Thymeleaf/JSP 页面模板（定义 HTML 结构）
    - <script>    → Controller 中的业务方法（定义数据和行为）
    - <style>     → CSS 样式文件（定义视觉外观）

  【Vue 模板语法速查】（类比后端概念）
    - {{ }}         → 插值表达式，显示变量值（类比 Thymeleaf 的 ${变量}）
    - v-model       → 双向绑定：输入框内容 ↔ 变量 自动同步（类比 @RequestParam 自动绑定参数）
    - @click        → 点击事件绑定（类比 @PostMapping 的方法映射）
    - v-if / v-else → 条件渲染（类比 if/else 判断）
    - :disabled     → 动态属性绑定（类比 th:disabled="${条件}"）
    - @keyup.enter  → 键盘回车事件（类比表单的 onsubmit）
-->

<template>
  <!--
    登录页外层容器
    【CSS 概念】使用 flex 布局实现垂直水平居中
    类比后端：类似 CSS 的 display:table + margin:auto 居中方案，但更现代
  -->
  <div class="login-page">
    <!--
      背景装饰层 - 纯视觉效果，增加页面层次感
      类比后端的静态资源文件，不参与业务逻辑
    -->
    <div class="login-bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>

    <!--
      登录卡片容器
      【Element Plus】el-card 组件 = 一个带阴影的卡片容器
      类比后端：类似 Bootstrap 的 card 组件，或者一个带 border 的 div
    -->
    <el-card class="login-card" shadow="always">
      <!--
        顶部标题区域
        包含 Logo 图标和系统名称
      -->
      <div class="login-header">
        <!-- 
          图标：使用 Element Plus 的图标组件
          【类比后端】相当于 <img> 标签或 iconfont 图标
          el-icon 是 Element Plus 的图标包装组件
        -->
        <div class="logo-wrapper">
          <el-icon :size="42" color="#e6522c">
            <ShoppingCartFull />
          </el-icon>
        </div>
        <h1 class="login-title">拼团商城</h1>
        <p class="login-subtitle">Group Buy Market</p>
      </div>

      <!--
        登录表单
        【Element Plus】
          - el-form：表单容器，提供校验、布局等功能（类比 <form> 标签增强版）
          - el-form-item：表单项，包含标签和控件（类比 Bootstrap 的 form-group）
        
        【属性说明】
          - :model="form" 绑定表单数据对象（类比 @ModelAttribute 绑定表单到对象）
          - label-width="0" 标签宽度为 0（这里不需要左侧标签）
      -->
      <el-form
        :model="form"
        label-width="0"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <!--
          用户名输入框
          【Element Plus】el-input 组件 = 增强版 <input> 标签
          
          【v-model 双向绑定详解】（核心概念！）
            - 单向绑定：{{ username }} → 只显示变量值
            - 双向绑定：v-model="username" → 变量变→输入框变，输入框变→变量也变
            - 类比后端：@RequestParam 自动把请求参数绑定到方法参数
              但 v-model 是实时的，每输入一个字符都会同步
          
          【prefix-icon 前缀图标】
            在输入框左侧显示图标，纯装饰作用
        -->
        <el-form-item>
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
            clearable
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <!--
          登录按钮
          【Element Plus】
            - type="primary"：主色调按钮（橙色，与项目主题一致）
            - size="large"：大尺寸
            - :loading="loading"：登录中显示加载动画（类比按钮变成转圈图标）
            - :disabled="!form.username.trim()"：用户名为空时禁用（类比 disabled 属性）
            - @click="handleLogin"：点击触发登录方法（类比 @PostMapping("/login")）
          
          【前端概念】:disabled 的 : 前缀
            - 普通 disabled="true" → 传字符串 "true"
            - 加冒号 :disabled="true" → 传布尔值 true（v-bind 缩写）
            - 类比 Thymeleaf：th:disabled="${条件}" 也是动态求值
        -->
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            :disabled="!form.username.trim()"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!--
        底部提示信息
        【前端概念】v-if 条件渲染：只有条件为 true 时才显示
        类比后端：if (errorMessage != null) { 显示错误信息; }
      -->
      <div class="login-tips" v-if="errorMsg">
        <el-alert
          :title="errorMsg"
          type="warning"
          show-icon
          :closable="false"
        />
      </div>

      <!-- 底部说明文字 -->
      <div class="login-footer">
        <p>输入任意用户名即可体验系统功能</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
/**
 * <script setup> 部分 - 登录页的业务逻辑
 * ========================================
 * 【类比后端】相当于 LoginController 类中的方法
 *   - 定义数据（类比 Controller 的字段）
 *   - 定义方法（类比 Controller 的方法）
 *   - 调用 Service（类比 @Autowired 注入服务）
 *
 * 【Vue 3 Composition API 说明】
 * - <script setup> 是 Vue 3 的语法糖（Composition API）
 * - 这里定义的变量和函数，模板中可以直接使用，不需要 return
 * - 类比 Spring 的 @Component 自动注册，不用手动配置
 *
 * 【类比后端对照表】
 *   import xxx from 'yyy'   → import xxx（Java 的 import 语句）
 *   ref('初始值')            → private String xxx = "初始值"（响应式字段）
 *   function xxx() { }      → public void xxx() { }（方法）
 *   onMounted(() => { })    → @PostConstruct 初始化方法
 */

// ==================== 导入依赖 ====================

/**
 * Vue 3 核心 API
 * - ref：创建响应式变量（值变化时页面自动更新）
 * - onMounted：组件挂载完成后执行（类比 @PostConstruct）
 */
import { ref, reactive, onMounted } from 'vue'

/**
 * Vue Router 路由 API
 * - useRouter：获取路由实例，用于编程式导航（类比 HttpServletResponse.sendRedirect）
 * - useRoute：获取当前路由信息（类比 HttpServletRequest 获取请求参数）
 *
 * 【编程式导航 vs 声明式导航】
 * - 声明式：<router-link to="/home"> 点击跳转（类比 <a href="/home">）
 * - 编程式：router.push('/home') 代码中跳转（类比 response.sendRedirect("/home")）
 */
import { useRouter, useRoute } from 'vue-router'

/**
 * Element Plus 图标组件
 * - User：用户图标，用在输入框前缀
 * - ShoppingCartFull：购物车图标，用在 Logo 区域
 */
import { User, ShoppingCartFull } from '@element-plus/icons-vue'

/**
 * 导入用户状态管理 Store
 * 【类比后端】相当于 @Autowired UserService userService;
 * 通过 useUserStore() 获取全局共享的用户状态
 */
import { useUserStore } from '../stores/user'

// ==================== 初始化实例 ====================

/**
 * 获取各种实例（类比 @Autowired 注入）
 *
 * 【注意】这些 Hook 函数必须在 <script setup> 顶层调用
 * 不能在函数内部或条件语句中调用（类比 @Autowired 只能用在类字段/构造器上）
 */
const router = useRouter()    // 路由实例（类比 HttpServletResponse，用于跳转）
const route = useRoute()      // 当前路由信息（类比 HttpServletRequest，获取参数）
const userStore = useUserStore()  // 用户状态 Store（类比 @Autowired UserService）

// ==================== 响应式数据（类比类的字段） ====================

/**
 * 表单数据对象
 * 【类比后端】相当于 LoginDTO / LoginForm 表单对象
 *   public class LoginForm {
 *     private String username;
 *   }
 *
 * reactive() 和 ref() 的区别：
 * - ref：包装单个值，需要 .value 访问
 * - reactive：包装对象，直接访问属性
 * 这里用 reactive 包装表单对象更合适（多字段场景）
 */
const form = reactive({
  username: ''  // 用户名，与 el-input 的 v-model 双向绑定
})

/**
 * 加载状态
 * 【类比后端】相当于一个 boolean 标志位，表示是否正在处理请求
 * true 时按钮显示 loading 动画，防止重复提交
 */
const loading = ref(false)

/**
 * 错误信息
 * 为空时不显示（v-if="errorMsg"），有值时显示警告提示
 */
const errorMsg = ref('')

// ==================== 生命周期（类比 Spring 生命周期） ====================

/**
 * onMounted - 组件挂载完成后执行
 * 【类比后端】相当于 @PostConstruct 注解的方法，在 Bean 初始化完成后执行
 *
 * 【使用场景】
 * 如果用户已登录（localStorage 有 username），自动跳转到首页
 * 避免已登录用户看到登录页面
 */
onMounted(() => {
  // 检查是否已登录（类比 if (session.getAttribute("username") != null)）
  if (userStore.isLoggedIn) {
    // 已登录，直接跳转到首页
    router.push({ name: 'home' })
  }
})

// ==================== 方法（类比 Controller 的方法） ====================

/**
 * 处理登录
 * 【类比后端】相当于：
 *   @PostMapping("/login")
 *   public String login(@RequestParam String username, HttpSession session) {
 *     session.setAttribute("username", username);
 *     return "redirect:/home";
 *   }
 *
 * 【async/await 异步说明】（重要概念！）
 * - async 标记函数为异步函数（类比 @Async 注解）
 * - await 等待异步操作完成（类比 Future.get() 阻塞等待结果）
 * - 前端很多操作是异步的（网络请求、定时器等），需要用 async/await 处理
 */
async function handleLogin() {
  // 1. 去除用户名两端空格
  const name = form.username.trim()

  // 2. 前端校验（类比 @Valid + @NotBlank 参数校验）
  if (!name) {
    errorMsg.value = '请输入用户名'
    return
  }

  // 3. 开始登录，设置 loading 状态（防止重复点击）
  loading.value = true
  errorMsg.value = ''

  try {
    // 4. 调用 userStore 的 login 方法
    // 【类比后端】userService.login(username);
    // 注意：这里是同步操作（存 localStorage），实际项目中可能是异步的网络请求
    userStore.login(name)

    // 5. 登录成功，跳转到目标页面
    // 【类比后端】return "redirect:" + redirectUrl;
    // route.query.redirect 获取 URL 中的 redirect 查询参数
    // 如果有 redirect 参数则跳转到该页面，否则跳转到首页
    const redirectPath = (route.query.redirect as string) || '/'
    router.push(redirectPath)

  } catch (error) {
    // 6. 登录失败，显示错误信息
    // 【类比后端】model.addAttribute("error", "登录失败");
    errorMsg.value = '登录失败，请重试'
  } finally {
    // 7. 无论成功失败，都关闭 loading 状态
    // 【类比后端】finally { lock.unlock(); } 资源释放
    loading.value = false
  }
}
</script>

<style scoped>
/**
 * <style scoped> - 登录页样式
 * ============================
 * 【前端概念】CSS 样式定义组件的视觉外观
 *
 * 【scoped 关键字】（重要！）
 * - scoped 表示这些样式只对当前组件生效，不会影响其他组件
 * - 类比后端：相当于 private 修饰符，只在当前类中有效
 * - 如果不加 scoped，样式是全局的，可能污染其他组件
 * - 原理：Vue 会给当前组件的 DOM 添加唯一属性（data-v-xxx），CSS 选择器也会加上这个属性
 *
 * 【CSS 基础语法速查】
 * - .class { }     → 类选择器（类比 Java 的类名）
 * - #id { }        → ID 选择器
 * - 选择器1 选择器2  → 后代选择器（嵌套关系）
 * - 选择器1.选择器2  → 同时满足两个条件
 * - & :hover       → 伪类（鼠标悬停状态）
 *
 * 【单位说明】
 * - px：像素（绝对单位）
 * - rem：相对根元素字体大小（响应式单位）
 * - %：相对父元素的百分比
 * - vh/vw：相对视口高度/宽度的百分比（1vh = 视口高度的 1%）
 */

/* ==================== 页面容器 ==================== */

/**
 * 登录页外层容器 - 全屏居中布局
 *
 * 【Flex 布局详解】（CSS 最强大的布局方案）
 * display: flex → 启用弹性盒子布局
 * justify-content: center → 主轴（水平）居中
 * align-items: center → 交叉轴（垂直）居中
 *
 * 类比后端：类似 Java 的 BorderLayout + Component.CENTER
 * 但 Flex 更强大，一行代码实现完美居中
 */
.login-page {
  min-height: 100vh;                    /* 最小高度 = 视口高度（全屏） */
  display: flex;                        /* 启用 Flex 弹性布局 */
  justify-content: center;              /* 水平居中 */
  align-items: center;                  /* 垂直居中 */
  /* 渐变背景：从深蓝到深紫，营造科技感 */
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  position: relative;                   /* 相对定位（为子元素的绝对定位提供参考点） */
  overflow: hidden;                     /* 隐藏溢出内容（防止装饰元素超出屏幕） */
}

/* ==================== 背景装饰 ==================== */

/**
 * 背景装饰层 - 浮动圆形，增加视觉层次感
 * 纯装饰，不参与业务逻辑
 */
.login-bg-decoration {
  position: absolute;                   /* 绝对定位（相对于 .login-page） */
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;                 /* 不响应鼠标事件（让点击穿透到下层） */
}

/* 装饰圆形 - 通用样式 */
.circle {
  position: absolute;                   /* 绝对定位 */
  border-radius: 50%;                   /* 圆角 50% = 正圆形 */
  opacity: 0.08;                        /* 透明度 8%（非常淡的背景效果） */
}

/* 圆形1 - 左上角大圆 */
.circle-1 {
  width: 400px;
  height: 400px;
  background: #e6522c;                  /* 主题橙色 */
  top: -100px;                          /* 部分超出屏幕（被 overflow:hidden 裁切） */
  left: -100px;
}

/* 圆形2 - 右下角中圆 */
.circle-2 {
  width: 300px;
  height: 300px;
  background: #f5a623;                  /* 金黄色 */
  bottom: -80px;
  right: -80px;
}

/* 圆形3 - 右上角小圆 */
.circle-3 {
  width: 200px;
  height: 200px;
  background: #ffffff;                  /* 白色 */
  top: 20%;
  right: 10%;
}

/* ==================== 登录卡片 ==================== */

/**
 * 登录卡片样式
 * 【Element Plus】el-card 自带基础样式，这里覆盖/增强
 *
 * 【CSS 覆盖 Element Plus 样式的技巧】
 * - Element Plus 组件的样式可以通过 CSS 变量或直接覆盖来修改
 * - 这里直接覆盖关键属性，保持其他默认样式
 */
.login-card {
  width: 420px !important;              /* 固定宽度 420px（!important 覆盖组件默认样式） */
  border-radius: 16px !important;       /* 圆角 16px（更圆润） */
  border: none !important;              /* 去掉边框 */
  background: rgba(255, 255, 255, 0.95) !important;  /* 半透明白色背景 */
  backdrop-filter: blur(20px);          /* 毛玻璃效果（背景模糊） */
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3) !important;  /* 大阴影，增加立体感 */
  padding: 20px 10px;                   /* 内边距 */
  z-index: 1;                           /* 层级：在装饰层之上 */
  /* 入场动画 */
  animation: cardFadeIn 0.6s ease-out;  /* 淡入 + 上移动画 */
}

/* 卡片入场动画 - 从下方淡入 */
@keyframes cardFadeIn {
  from {
    opacity: 0;                         /* 初始完全透明 */
    transform: translateY(30px);        /* 初始向下偏移 30px */
  }
  to {
    opacity: 1;                         /* 最终完全不透明 */
    transform: translateY(0);           /* 最终回到原位 */
  }
}

/* ==================== 头部区域 ==================== */

.login-header {
  text-align: center;                   /* 文字居中 */
  margin-bottom: 32px;                  /* 底部间距 */
}

/* Logo 图标容器 */
.logo-wrapper {
  display: inline-flex;                 /* 行内 Flex 容器 */
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border-radius: 50%;                   /* 圆形背景 */
  background: linear-gradient(135deg, #fff5f0, #ffe8d6);  /* 浅橙色渐变 */
  margin-bottom: 16px;                  /* 与标题的间距 */
  box-shadow: 0 4px 16px rgba(230, 82, 44, 0.15);  /* 淡橙色阴影 */
}

/* 标题文字 */
.login-title {
  font-size: 28px;                      /* 字号 28px */
  font-weight: 700;                     /* 粗体 */
  color: #1a1a2e;                       /* 深色文字 */
  margin: 0 0 4px 0;                    /* 间距调整 */
  letter-spacing: 2px;                  /* 字间距 2px（更舒展） */
}

/* 副标题 */
.login-subtitle {
  font-size: 13px;                      /* 字号 13px */
  color: #999;                          /* 灰色 */
  letter-spacing: 3px;                  /* 字间距较大（英文更有设计感） */
  text-transform: uppercase;            /* 英文大写 */
  margin: 0;
}

/* ==================== 表单区域 ==================== */

.login-form {
  margin-top: 8px;                      /* 顶部间距 */
}

/* 登录按钮 */
.login-btn {
  width: 100%;                          /* 宽度 100%（占满卡片宽度） */
  letter-spacing: 8px;                  /* 字间距（"登 录" 两字之间有空隙） */
  font-size: 16px;                      /* 字号 */
  border-radius: 8px !important;        /* 圆角 */
  height: 44px !important;              /* 固定高度 */
  /* 按钮渐变背景 */
  background: linear-gradient(135deg, #e6522c, #f07048) !important;
  border: none !important;              /* 无边框 */
  transition: all 0.3s ease;            /* 过渡动画（hover 时平滑变化） */
}

/* 按钮悬停效果 */
.login-btn:hover {
  transform: translateY(-2px);          /* 向上偏移 2px（浮起效果） */
  box-shadow: 0 6px 20px rgba(230, 82, 44, 0.4);  /* 橙色阴影 */
}

/* 按钮按下效果 */
.login-btn:active {
  transform: translateY(0);             /* 回到原位 */
}

/* ==================== 提示信息 ==================== */

.login-tips {
  margin-top: 16px;                     /* 顶部间距 */
}

/* ==================== 底部说明 ==================== */

.login-footer {
  text-align: center;                   /* 文字居中 */
  margin-top: 24px;                     /* 顶部间距 */
  padding-top: 16px;                    /* 内顶部间距 */
  border-top: 1px solid #f0f0f0;        /* 顶部细线分隔 */
}

.login-footer p {
  font-size: 12px;                      /* 小字号 */
  color: #bbb;                          /* 浅灰色 */
  margin: 0;
}

/* ==================== 输入框增强样式 ==================== */

/*
 * 深度选择器 :deep()
 * 【前端概念】scoped 样式无法直接影响子组件内部的 DOM
 * :deep() 可以穿透 scoped 限制，修改子组件内部样式
 * 类比后端：相当于反射（Reflection）访问私有字段
 */
:deep(.el-input__wrapper) {
  border-radius: 8px;                   /* 输入框圆角 */
  box-shadow: 0 0 0 1px #dcdfe6 inset;  /* 边框颜色 */
  transition: all 0.3s ease;            /* 过渡动画 */
}

/* 输入框聚焦时的样式 */
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #e6522c inset;  /* 聚焦时边框变为主题橙色 */
}

/* ==================== 响应式适配 ==================== */

/*
 * 媒体查询 - 小屏幕适配
 * 【前端概念】@media 根据屏幕尺寸应用不同样式
 * 类比后端的策略模式：不同条件使用不同策略
 */
@media (max-width: 480px) {
  .login-card {
    width: calc(100vw - 40px) !important;  /* 屏幕宽度 - 40px 边距 */
    margin: 0 20px;                        /* 左右各 20px 边距 */
  }
}
</style>
