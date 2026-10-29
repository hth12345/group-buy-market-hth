<!--
  CountdownTimer.vue - 倒计时组件
  ================================
  【前端概念】这是一个"子组件"，专门负责倒计时显示，可以被任何需要倒计时的地方复用
  【类比后端】
    - 相当于后端的一个工具类方法（如 DateUtils.formatCountdown()）
    - 但前端组件是"活的"——它会自己每秒更新显示（后端方法只能返回一次结果）
    - 类比 Swing/AWT 的 Timer 或者 JavaFX 的 Timeline，定时刷新 UI

  【组件功能】
    - 接收一个截止时间（endTime），实时显示剩余时间
    - 每秒自动更新（用 setInterval 定时器）
    - 倒计时结束显示"已过期"
    - 组件销毁时自动清除定时器（防止内存泄漏）

  【使用方式】（在父组件中）
    <CountdownTimer endTime="2025-12-31 23:59:59" />
-->

<template>
  <!--
    【前端概念】template 是组件的 HTML 结构
    【类比后端】相当于 JSP/Thymeleaf 模板，定义页面长什么样

    【v-if 条件渲染】
    - v-if="isExpired" 表示只有 isExpired 为 true 时才渲染这个 span
    - 【类比后端】相当于 JSTL 的 <c:if test="${isExpired}"> 或 Thymeleaf 的 th:if
    - 与 v-else 配合使用，类似 if-else 分支
  -->
  <span class="countdown-timer" :class="{ expired: isExpired }">
    <template v-if="isExpired">
      <!-- 倒计时已结束：显示过期提示 -->
      <el-tag type="info" size="small" effect="plain">已过期</el-tag>
    </template>
    <template v-else>
      <!-- 倒计时进行中：显示剩余时间 -->
      <!--
        【前端概念】mustache 语法 {{ }} 用于在模板中显示变量的值
        【类比后端】相当于 Thymeleaf 的 th:text="${countdown}" 或 JSP 的 ${countdown}
        这里显示 countdown 变量的值，如 "02:15:30"
      -->
      <el-tag type="warning" size="small" effect="dark" round>
        ⏱ {{ countdown }}
      </el-tag>
    </template>
  </span>
</template>

<script setup lang="ts">
/**
 * 倒计时组件的逻辑部分
 *
 * 【前端概念】<script setup lang="ts"> 是 Vue 3 的组合式 API 写法
 * 【类比后端】
 *   - 相当于 Java 类的实现部分（class CountdownTimer { ... }）
 *   - template 是"视图层"（长什么样），script 是"逻辑层"（怎么工作）
 *   - 类比后端的 MVC：template ≈ View，script ≈ Controller
 *
 * 【setup 语法糖】
 *   - <script setup> 是 Vue 3 的语法糖，简化组件定义
 *   - 不需要写 export default { setup() { ... } } 这种冗长写法
 *   - 在 setup 中定义的变量/函数会自动暴露给 template 使用
 *   - 【类比后端】相当于 Lombok 的 @Data，自动生成 getter/setter 暴露字段
 */

import { ref, computed, onUnmounted, watch } from 'vue'
// 【前端概念】从 vue 中导入响应式 API
// 【类比后端】相当于 import static java.util.concurrent.TimeUnit.*;
// - ref:       创建响应式变量（值变化 → 页面自动更新）
// - computed:  创建计算属性（依赖变化 → 自动重新计算）
// - onUnmounted: 生命周期钩子（组件销毁时执行，类比 @PreDestroy）
// - watch:     监听器（监听某个值的变化并执行回调）

import { ElTag } from 'element-plus'
// 【前端概念】导入 Element Plus 的标签组件，用于显示倒计时
// 【类比后端】相当于导入一个 UI 组件库的 Tag 控件

// =============================================
// 一、定义组件输入（Props）
// =============================================

/**
 * 【前端概念】defineProps() 定义组件接收的外部输入
 * 【类比后端】
 *   - 相当于 Java 构造器的参数：public CountdownTimer(String endTime) { ... }
 *   - 或者 Spring 的 @Value("${endTime}") 注入配置值
 *   - 父组件通过 <CountdownTimer endTime="xxx" /> 传入值
 *
 * 【重要特性】Props 是只读的（readonly）
 *   - 类比 Java 的 final 字段：一旦传入，组件内部不能修改
 *   - 这是 Vue 的单向数据流设计：父 → 子 通过 props，子 → 父 通过 emit
 *   - 【类比后端】相当于 Request 对象的属性是只读的，你不能在 Servlet 里改请求参数
 *
 * 【类型定义】
 *   - endTime: string 表示这个 prop 必须是字符串类型
 *   - 如果父组件传了非字符串，TypeScript 编译时会报错（类比 Java 编译期类型检查）
 */
const props = defineProps<{
  endTime: string  // 倒计时截止时间，格式如 "2025-12-31T23:59:59" 或 "HH:mm:ss"
}>()

// =============================================
// 二、响应式状态（类比 Java 类的成员变量）
// =============================================

/**
 * 剩余秒数 - 响应式变量
 * 【类比后端】相当于 private int remainingSeconds; 成员变量
 *
 * 【为什么用 ref 而不是普通变量？】
 *   - let remaining = 100;  → 普通变量，赋值后页面不会自动更新
 *   - const remaining = ref(100);  → 响应式变量，赋值后页面自动重新渲染
 *   - 【类比后端】普通变量 ≈ 没有 getter 的字段（外部感知不到变化）
 *              ref ≈ 有 PropertyChangeListener 的字段（变化时通知所有监听者）
 *
 * 【ref 的使用规则】
 *   - 在 script 中：必须用 .value 访问/修改（如 remaining.value = 50）
 *   - 在 template 中：自动解包，不需要 .value（如 {{ remaining }}）
 *   - 这就像 Java 的 AtomicReference<Integer>，取值要 .get()，设值要 .set()
 */
const remaining = ref<number>(0)

/**
 * 定时器 ID - 用于后续清除
 * 【类比后端】相当于 ScheduledFuture<?> future; 保存定时任务引用以便取消
 *
 * 为什么保存定时器 ID？
 *   - setInterval 会返回一个 ID，用来标识这个定时任务
 *   - 组件销毁时必须清除定时器，否则会"内存泄漏"
 *   - 【类比后端】就像 @PreDestroy 中要 executorService.shutdown() 一样
 */
let timerId: ReturnType<typeof setInterval> | null = null

// =============================================
// 三、工具函数（类比 Java 的 private 方法）
// =============================================

/**
 * 解析截止时间字符串为剩余秒数
 *
 * 【类比后端】相当于一个 private 工具方法：
 *   private int parseRemainingSeconds(String endTime) { ... }
 *
 * 支持两种格式：
 *   1. 完整日期时间："2025-12-31T23:59:59" → 计算与当前时间的差值
 *   2. 纯时间格式："02:15:30" → 直接转换为秒数（2小时15分30秒 = 8130秒）
 *
 * @param endTime 截止时间字符串
 * @returns 剩余秒数（如果已过期返回 0）
 */
function parseEndTime(endTime: string): number {
  // 情况1：包含 T 或 - 的完整日期时间格式
  if (endTime.includes('T') || endTime.includes('-')) {
    // 【前端概念】new Date() 创建 JavaScript 日期对象
    // 【类比后端】相当于 Java 的 LocalDateTime.parse(endTime)
    const endTimestamp = new Date(endTime).getTime()  // 转为毫秒时间戳
    const nowTimestamp = Date.now()  // 当前时间的毫秒时间戳（类比 System.currentTimeMillis()）
    const diffSeconds = Math.floor((endTimestamp - nowTimestamp) / 1000)
    // Math.floor 向下取整，类比 Java 的 (int)(diff / 1000)
    return Math.max(0, diffSeconds)  // 如果已过期（负数），返回 0
    // 【类比后端】Math.max(0, x) 相当于 Java 的 Math.max(0, x)，取两者中较大的
  }

  // 情况2：纯时间格式 HH:mm:ss（如 "02:15:30"）
  if (endTime.includes(':')) {
    // 【前端概念】split(':') 按冒号分割字符串，返回数组
    // 【类比后端】相当于 Java 的 endTime.split(":")
    const parts = endTime.split(':')
    if (parts.length === 3) {
      const hours = parseInt(parts[0]) || 0    // 小时部分
      const minutes = parseInt(parts[1]) || 0  // 分钟部分
      const seconds = parseInt(parts[2]) || 0  // 秒数部分
      // 【类比后端】parseInt() 相当于 Java 的 Integer.parseInt()
      // || 0 表示如果解析失败（NaN），则使用默认值 0（类比 Java 的 Optional.orElse(0)）
      return hours * 3600 + minutes * 60 + seconds
      // 1小时 = 3600秒，1分钟 = 60秒
    }
  }

  // 格式不匹配，返回 0
  return 0
}

/**
 * 格式化剩余秒数为 HH:mm:ss 显示格式
 *
 * 【类比后端】相当于 Java 的 String.format("%02d:%02d:%02d", h, m, s)
 *
 * @param totalSeconds 总秒数
 * @returns 格式化后的时间字符串，如 "02:15:30"
 */
function formatTime(totalSeconds: number): string {
  const hours = Math.floor(totalSeconds / 3600)         // 计算小时数
  const minutes = Math.floor((totalSeconds % 3600) / 60) // 计算分钟数（去掉小时后）
  const seconds = totalSeconds % 60                       // 计算秒数（去掉分钟后）

  // 【前端概念】String.padStart(2, '0') 在字符串左侧补零到指定长度
  // 【类比后端】相当于 Java 的 String.format("%02d", hours)
  // 例如：5 → "05"，12 → "12"
  return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
  // 【前端概念】`${}` 是 ES6 模板字符串语法，在字符串中嵌入变量值
  // 【类比后端】相当于 Java 的 String.format() 或 StringBuilder 拼接
}

// =============================================
// 四、计算属性（类比 Java 的 getter 方法）
// =============================================

/**
 * 倒计时显示文本 - 计算属性
 *
 * 【前端概念】computed() 创建计算属性，基于其他响应式数据派生出新值
 * 【类比后端】
 *   - 相当于 Java 的 getter 方法：public String getCountdown() { return formatTime(remaining); }
 *   - 但比普通 getter 更智能：有缓存机制
 *   - 只有 remaining.value 变化时才会重新调用 formatTime()
 *   - 如果 remaining 没变，多次访问 countdown 返回的是缓存值（不会重复计算）
 *
 * 【为什么用 computed 而不是函数？】
 *   - computed: 有缓存，依赖不变就不重新计算（性能更好）
 *   - 函数: 每次调用都重新计算（即使依赖没变）
 *   - 【类比后端】computed ≈ 带 @Cacheable 注解的方法，函数 ≈ 普通方法
 */
const countdown = computed(() => formatTime(remaining.value))

/**
 * 是否已过期 - 计算属性
 *
 * 【类比后端】相当于 Java 的：public boolean isExpired() { return remaining <= 0; }
 * 当 remaining 变为 0 或负数时，这个值自动变为 true，模板中的 v-if 会自动响应
 */
const isExpired = computed(() => remaining.value <= 0)

// =============================================
// 五、定时器管理（类比 Java 的 ScheduledExecutorService）
// =============================================

/**
 * 启动倒计时定时器
 *
 * 【前端概念】setInterval(callback, interval) 每隔指定毫秒数重复执行回调函数
 * 【类比后端】
 *   - 相当于 Java 的 ScheduledExecutorService.scheduleAtFixedRate()
 *   - setInterval(fn, 1000) ≈ scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS)
 *   - 返回一个定时器 ID，用于后续 clearInterval() 取消定时任务
 *
 * 【为什么要每秒更新？】
 *   - remaining 是 ref 响应式变量
 *   - 每次 remaining.value 变化，依赖它的 countdown 计算属性自动重新计算
 *   - countdown 变了，模板中 {{ countdown }} 的显示自动更新
 *   - 这就是"响应式"的魔力：数据驱动视图更新，不需要手动操作 DOM
 */
function startTimer() {
  // 先清除可能存在的旧定时器（防止重复启动导致多个定时器并行）
  // 【类比后端】相当于在启动新线程前先 shutdown 旧的线程池
  stopTimer()

  // 如果已经过期，不需要启动定时器
  if (remaining.value <= 0) return

  // 启动每秒执行一次的定时器
  // 【类比后端】相当于：
  //   scheduler.scheduleAtFixedRate(() -> {
  //       remaining.set(remaining.get() - 1);
  //       if (remaining.get() <= 0) stopTimer();
  //   }, 0, 1, TimeUnit.SECONDS);
  timerId = setInterval(() => {
    if (remaining.value > 0) {
      remaining.value--  // 每秒减 1（ref 变量用 .value 修改）
      // 【重要】这一行修改会触发连锁反应：
      // remaining 变化 → countdown 重新计算 → 模板中 {{ countdown }} 更新 → 页面刷新显示
    }
    if (remaining.value <= 0) {
      stopTimer()  // 倒计时结束，清除定时器
    }
  }, 1000)  // 1000 毫秒 = 1 秒
}

/**
 * 停止并清除定时器
 *
 * 【前端概念】clearInterval(timerId) 清除 setInterval 创建的定时器
 * 【类比后端】
 *   - 相当于 executorService.shutdown() 或 timerTask.cancel()
 *   - 如果不清除，定时器会一直运行，即使组件已经销毁了
 *   - 这就是"内存泄漏"——组件没了但定时器还在跑，浪费资源
 */
function stopTimer() {
  if (timerId !== null) {
    clearInterval(timerId)  // 清除定时器
    timerId = null           // 清空引用，方便垃圾回收
    // 【类比后端】相当于 timerTask.cancel(); timerTask = null;
  }
}

// =============================================
// 六、生命周期钩子（类比 Spring Bean 的生命周期回调）
// =============================================

/**
 * 监听 props.endTime 变化，重新计算剩余时间并启动定时器
 *
 * 【前端概念】watch() 监听响应式数据的变化，变化时执行回调
 * 【类比后端】
 *   - 相当于 Java 的 PropertyChangeListener 或 Spring 的 @EventListener
 *   - 当 endTime 这个"配置值"变化时，自动重新初始化倒计时
 *
 * 【为什么需要 watch？】
 *   - 父组件可能会更新传给子组件的 props（比如切换了拼团）
 *   - endTime 变了，需要重新解析并启动新的倒计时
 */
watch(
  () => props.endTime,  // 监听的目标：props.endTime 的变化
  (newEndTime) => {     // 回调函数：当 endTime 变化时执行
    // 【类比后端】相当于 onChangeEndTime(String newEndTime) 方法
    remaining.value = parseEndTime(newEndTime)  // 重新解析截止时间
    startTimer()  // 重新启动定时器
  },
  { immediate: true }  // 立即执行一次（组件创建时就解析初始值）
  // 【类比后端】immediate: true 相当于 @PostConstruct，组件初始化时立即执行
)

/**
 * 组件销毁时的清理操作
 *
 * 【前端概念】onUnmounted() 是 Vue 3 的生命周期钩子，在组件从 DOM 中移除时调用
 * 【类比后端】
 *   - 相当于 Spring Bean 的 @PreDestroy 注解方法
 *   - 或者 Servlet 的 destroy() 方法
 *   - 用于清理资源：关闭连接、停止定时器、释放内存
 *
 * 【为什么必须清除定时器？】
 *   - 组件销毁后，它绑定的 DOM 元素已经被移除
 *   - 但 setInterval 创建的定时器是全局的，不会因为组件销毁而自动停止
 *   - 如果不清除，定时器继续执行，尝试更新一个已不存在的组件 → 内存泄漏
 *   - 【类比后端】就像线程池中运行的任务，线程池关了但任务线程还在跑
 *   - 所以必须在 @PreDestroy 中手动 shutdown
 */
onUnmounted(() => {
  // 【类比后端】相当于 @PreDestroy 方法：
  //   @PreDestroy
  //   public void cleanup() {
  //       if (scheduler != null) {
  //           scheduler.shutdown();
  //           scheduler = null;
  //       }
  //   }
  stopTimer()
})
</script>

<style scoped>
/**
 * 【前端概念】<style scoped> 定义组件的私有样式
 * scoped 表示这些 CSS 只对当前组件生效，不会影响其他组件
 * 【类比后端】相当于 Java 的 private 字段，只在本类内部可见
 *
 * 如果不加 scoped，样式是全局的，可能污染其他组件
 * 【类比后端】不加 scoped ≈ 所有字段都是 public static，全局可见
 */

/* 倒计时容器样式 */
.countdown-timer {
  display: inline-flex;
  align-items: center;
}

/* 过期状态的样式 */
.countdown-timer.expired {
  opacity: 0.6;
}
</style>
