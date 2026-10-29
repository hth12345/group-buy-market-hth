<!--
  TeamCard.vue - 拼团卡片组件
  ============================
  【前端概念】这是一个"可复用组件"，每个拼团用一张卡片展示，列表中有几个团就渲染几个
  【类比后端】
    - 相当于后端的一个 VO 对象渲染成 HTML 片段
    - 或者理解为 JSP 中 <jsp:include> 引入的模板片段，循环渲染每一条拼团数据
    - 类比 Thymeleaf 的 th:fragment，定义一次、多处引用

  【组件功能】
    - 接收 props: teamInfo（一条拼团数据，类型 TeamInfo）
    - 展示：团长用户名（脱敏）、剩余人数、倒计时
    - 提供"参与拼团"按钮，点击后通过 emit 通知父组件

  【使用方式】（在父组件 HomeView.vue 中）
    <TeamCard
      v-for="team in teamList"
      :key="team.teamId"
      :teamInfo="team"
      @joinTeam="handleJoinTeam"
    />
-->

<template>
  <!--
    【前端概念】el-card 是 Element Plus 提供的卡片容器组件
    【类比后端】相当于 Bootstrap 的 <div class="card">，一个带阴影圆角的容器
    shadow="hover" 表示鼠标悬停时显示阴影效果（增加交互感）
  -->
  <el-card class="team-card" shadow="hover">
    <!--
      【前端概念】Flexbox 弹性布局，让内部元素水平/垂直排列
      【类比后端】相当于 CSS 的 display: flex，类似 Java Swing 的 BoxLayout
    -->
    <div class="team-card-content">

      <!-- ====== 左侧：拼团信息区域 ====== -->
      <div class="team-info">
        <!--
          第一行：团长头像 + 用户名（脱敏）
          【前端概念】头像用 el-avatar 组件，显示用户名首字母
        -->
        <div class="team-leader">
          <el-avatar :size="36" class="leader-avatar">
            {{ leaderInitial }}
          </el-avatar>
          <span class="leader-name">{{ maskedUsername }}</span>
          <!--
            【v-if 条件渲染】
            如果剩余人数 > 0，显示"还差X人"的标签
            【类比后端】相当于 <c:if test="${remainCount > 0}">
          -->
          <el-tag v-if="remainCount > 0" type="warning" size="small" effect="light" round>
            还差 {{ remainCount }} 人
          </el-tag>
          <el-tag v-else type="success" size="small" effect="light" round>
            ✓ 已成团
          </el-tag>
        </div>

        <!-- 第二行：倒计时 -->
        <div class="team-countdown">
          <span class="countdown-label">
            <!--
              【前端概念】直接引用子组件 <CountdownTimer>
              【类比后端】相当于在 JSP 中 <jsp:include page="countdown.jsp">
              或者 Java 中调用另一个类的方法：countdownTimer.render(endTime)
              传入 :endTime="teamInfo.validEndTime" 作为参数
            -->
            <CountdownTimer :endTime="teamInfo.validEndTime" />
          </span>
          <span class="time-range">
            {{ formatTimeRange(teamInfo.validStartTime, teamInfo.validEndTime) }}
          </span>
        </div>

        <!-- 第三行：拼团进度条 -->
        <div class="team-progress">
          <el-progress
            :percentage="progressPercent"
            :stroke-width="8"
            :color="progressColor"
            :show-text="false"
          />
          <!--
            【前端概念】el-progress 是 Element Plus 的进度条组件
            :percentage 绑定进度百分比（0-100）
            :stroke-width 进度条高度（像素）
            :color 进度条颜色（可以动态改变）
            :show-text="false" 隐藏默认文字，用自定义文字代替
          -->
          <span class="progress-text">
            {{ teamInfo.completeCount }}/{{ teamInfo.targetCount }} 人
          </span>
        </div>
      </div>

      <!-- ====== 右侧：操作按钮区域 ====== -->
      <div class="team-action">
        <!--
          【前端概念】@click 绑定点击事件处理器
          【类比后端】
            - 相当于 HTML 的 onclick="handleClick()"
            - 或者 Java Swing 的 button.addActionListener(e -> handleJoinTeam())
            - 点击按钮时调用 handleJoin() 方法

          【emit 事件传递】
            - 点击后调用 handleJoin()，内部通过 emit('joinTeam', teamInfo) 通知父组件
            - 【类比后端】相当于 Java 的事件发布/监听模式（Observer 模式）
            - 子组件是"事件发布者"，父组件是"事件监听者"
            - 父组件通过 @joinTeam="handleXxx" 监听这个事件
        -->
        <el-button
          v-if="remainCount > 0"
          type="primary"
          size="default"
          class="join-btn"
          @click="handleJoin"
        >
          参与拼团
        </el-button>
        <el-button
          v-else
          type="success"
          size="default"
          class="join-btn"
          disabled
        >
          已成团
        </el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
/**
 * TeamCard 组件的逻辑部分
 *
 * 【前端概念】<script setup lang="ts"> 是 Vue 3 组合式 API 的语法糖
 * 【类比后端】相当于 Java 类的实现部分（字段 + 方法）
 *
 * 组件内部结构：
 *   1. defineProps  → 定义输入参数（类比构造器参数 / @Autowired 注入）
 *   2. defineEmits  → 定义输出事件（类比 Observer 模式的 EventPublisher）
 *   3. computed     → 计算属性（类比 getter 方法，有缓存）
 *   4. methods      → 普通方法（类比 Java 的 public/private 方法）
 */

import { computed } from 'vue'
// 【前端概念】引入 Vue 的 computed 函数（创建计算属性）
// 【类比后端】相当于引入 @Cacheable 注解，依赖不变时不重新计算

import { ElCard, ElAvatar, ElTag, ElButton, ElProgress } from 'element-plus'
// 【前端概念】引入 Element Plus 的 UI 组件
// 【类比后端】相当于 import javax.swing.*; 引入 GUI 组件库

import CountdownTimer from './CountdownTimer.vue'
// 【前端概念】引入上面定义的倒计时子组件
// 【类比后端】相当于 @Autowired 注入一个 Bean，或者 import 一个工具类

import type { TeamInfo } from '@/api/types'
// 【前端概念】引入 TeamInfo 类型（只用于类型检查，编译后会被擦除）
// 【类比后端】相当于 import DTO 类，但只在方法签名的参数类型上使用

// =============================================
// 一、定义组件输入（Props）
// =============================================

/**
 * 【前端概念】defineProps<TeamInfo>() 声明这个组件需要什么数据
 * 【类比后端】
 *   - 相当于 Java 构造器参数：public TeamCard(TeamInfo teamInfo) { this.teamInfo = teamInfo; }
 *   - 或者 Spring MVC 的 @ModelAttribute TeamInfo teamInfo（框架自动注入数据）
 *   - 父组件传入的数据会"注入"到 props 中
 *
 * 【重要】props 是只读的（readonly）
 *   - 类比 Java 的 final 字段：public final TeamInfo teamInfo
 *   - 组件内部不能修改 props 的值（Vue 会警告）
 *   - 这是"单向数据流"设计：父 → 子 通过 props，子 → 父 通过 emit
 *   - 【类比后端】Request 对象的属性是只读的，不能在 Servlet 里修改请求参数
 */
const props = defineProps<{
  teamInfo: TeamInfo  // 拼团数据（来自父组件传入的一条 TeamInfo 记录）
}>()

// =============================================
// 二、定义组件事件输出（Emits）
// =============================================

/**
 * 【前端概念】defineEmits() 声明这个组件会触发哪些事件
 * 【类比后端】
 *   - 相当于 Java 的 Observer 模式 / 事件发布机制
 *   - 或者 Spring 的 ApplicationEventPublisher.publishEvent()
 *   - 子组件通过 emit('joinTeam', data) 向父组件发送事件
 *   - 父组件通过 @joinTeam="handler" 监听并处理
 *
 * 【事件名称约定】
 *   - 使用 camelCase 命名（如 joinTeam）
 *   - 在模板中使用时写成 kebab-case（如 @join-team）
 *   - Vue 会自动转换两种命名风格
 *
 * 【类型定义】
 *   - (payload: TeamInfo) => void 表示这个事件会携带一个 TeamInfo 类型的参数
 *   - 【类比后端】相当于 Java 事件类：public class JoinTeamEvent extends ApplicationEvent {
 *       private final TeamInfo teamInfo;  // 事件携带的数据
 *     }
 */
const emit = defineEmits<{
  (e: 'joinTeam', payload: TeamInfo): void
  // 定义一个名为 joinTeam 的事件，携带 TeamInfo 数据
  // 父组件监听：@joinTeam="(team) => handleJoin(team)"
}>()

// =============================================
// 三、计算属性（类比 Java 的 getter 方法，带缓存）
// =============================================

/**
 * 团长用户名脱敏显示
 *
 * 【前端概念】对用户名进行隐私处理，只显示首字和尾字，中间用 * 代替
 * 【类比后端】
 *   - 相当于 Java 的：
 *     String masked = username.charAt(0) + "***" + username.charAt(username.length()-1);
 *   - 或者后端工具类 DesensitizeUtil.maskName("张三") → "张*三"
 *
 * 【示例】
 *   "张三"     → "张*"
 *   "李四丰"   → "李*丰"
 *   "王"       → "王*"
 */
const maskedUsername = computed(() => {
  const name = props.teamInfo.userId  // 从 props 获取团长用户ID
  if (!name) return '匿名用户'
  if (name.length <= 1) return name + '*'
  // 取第一个字 + * + 最后一个字
  return name.charAt(0) + '*' + name.charAt(name.length - 1)
})

/**
 * 团长用户名首字母（用于头像显示）
 *
 * 【类比后端】相当于 Java 的：
 *   char initial = userId.isEmpty() ? '?' : Character.toUpperCase(userId.charAt(0));
 */
const leaderInitial = computed(() => {
  const name = props.teamInfo.userId
  return name ? name.charAt(0).toUpperCase() : '?'
})

/**
 * 剩余需要的人数
 *
 * 【类比后端】相当于 Java 的：
 *   int remain = teamInfo.getTargetCount() - teamInfo.getCompleteCount();
 *   return Math.max(0, remain);  // 不能为负数
 *
 * 如果已成团或超额，返回 0
 */
const remainCount = computed(() => {
  const diff = props.teamInfo.targetCount - props.teamInfo.completeCount
  return Math.max(0, diff)
})

/**
 * 拼团进度百分比（0-100）
 *
 * 【类比后端】相当于 Java 的：
 *   double percent = (double) completeCount / targetCount * 100;
 *   return Math.min(100, Math.round(percent));
 *
 * 用于 el-progress 进度条组件
 */
const progressPercent = computed(() => {
  if (props.teamInfo.targetCount === 0) return 0  // 防止除以 0（类比 Java 的防御性编程）
  const percent = (props.teamInfo.completeCount / props.teamInfo.targetCount) * 100
  return Math.min(100, Math.round(percent))  // 最大不超过 100
})

/**
 * 进度条颜色（根据进度动态变化）
 *
 * 【类比后端】相当于 Java 的：
 *   public String getProgressColor() {
 *       if (percent >= 100) return "#67C23A";  // 绿色（成功）
 *       if (percent >= 50)  return "#E6A23C";  // 橙色（进行中）
 *       return "#409EFF";                       // 蓝色（刚开始）
 *   }
 */
const progressColor = computed(() => {
  if (progressPercent.value >= 100) return '#67C23A'  // 绿色：已成团
  if (progressPercent.value >= 50) return '#E6A23C'   // 橙色：过半
  return '#409EFF'                                      // 蓝色：刚开始
})

// =============================================
// 四、工具方法
// =============================================

/**
 * 格式化时间范围显示
 *
 * 【类比后端】相当于 Java 的日期格式化工具方法：
 *   public String formatTimeRange(String start, String end) { ... }
 *
 * @param startTime 开始时间字符串
 * @param endTime   结束时间字符串
 * @returns 格式化后的显示文本
 */
function formatTimeRange(startTime: string, endTime: string): string {
  // 简单处理：只显示日期部分（截取前10位，即 yyyy-MM-dd）
  // 【类比后端】相当于 Java 的 startTime.substring(0, 10)
  const start = startTime ? startTime.substring(0, 16).replace('T', ' ') : '--'
  const end = endTime ? endTime.substring(0, 16).replace('T', ' ') : '--'
  return `${start} ~ ${end}`
}

/**
 * 处理"参与拼团"按钮点击
 *
 * 【前端概念】通过 emit 向父组件发送事件，传递当前拼团数据
 * 【类比后端】
 *   - 相当于 Java 的 eventPublisher.publishEvent(new JoinTeamEvent(teamInfo))
 *   - 或者 Swing 的 fireActionPerformed(new ActionEvent(this, ...))
 *   - 子组件本身不处理业务逻辑，只是"通知"父组件：有人点了参团按钮
 *   - 父组件收到通知后决定做什么（跳转详情页、弹窗确认等）
 *
 * 【设计模式】这是典型的"观察者模式"（Observer Pattern）
 *   - 子组件 = 被观察者（Subject），状态变化时通知观察者
 *   - 父组件 = 观察者（Observer），监听子组件事件并处理
 *   - 【类比后端】Spring 的 ApplicationEvent + ApplicationListener
 */
function handleJoin() {
  // 触发 joinTeam 事件，将当前拼团数据传给父组件
  emit('joinTeam', props.teamInfo)
}
</script>

<style scoped>
/**
 * 【前端概念】<style scoped> 定义组件的私有 CSS 样式
 * scoped 确保这些样式只作用于当前组件，不会"污染"其他组件
 * 【类比后端】相当于 Java 的 private 字段，只在本类内部可见
 *
 * 如果不加 scoped，样式是全局的，可能影响其他同名 class 的元素
 */

/* 拼团卡片整体样式 */
.team-card {
  margin-bottom: 12px;
  border-radius: 12px;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

/* 鼠标悬停时卡片微微上移（增加交互反馈感） */
.team-card:hover {
  transform: translateY(-2px);
}

/* 卡片内部使用 Flexbox 水平布局 */
.team-card-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

/* 左侧信息区域 */
.team-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 团长信息行：头像 + 名字 + 标签 */
.team-leader {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 团长头像背景色 */
.leader-avatar {
  background: linear-gradient(135deg, #409EFF, #67C23A);
  color: white;
  font-weight: bold;
  font-size: 14px;
}

/* 团长名称 */
.leader-name {
  font-weight: 500;
  font-size: 14px;
  color: #303133;
}

/* 倒计时行 */
.team-countdown {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #909399;
}

.countdown-label {
  display: inline-flex;
  align-items: center;
}

/* 时间范围文本 */
.time-range {
  font-size: 11px;
  color: #C0C4CC;
}

/* 进度条行 */
.team-progress {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 进度条容器 */
.team-progress :deep(.el-progress) {
  flex: 1;
}

/* 进度文字 */
.progress-text {
  font-size: 12px;
  color: #606266;
  white-space: nowrap;
  min-width: 40px;
}

/* 右侧操作区域 */
.team-action {
  flex-shrink: 0;
}

/* 参团按钮样式 */
.join-btn {
  min-width: 90px;
  font-weight: 500;
}

/* 响应式：小屏幕时改为垂直布局 */
@media (max-width: 480px) {
  .team-card-content {
    flex-direction: column;
    align-items: stretch;
  }

  .team-action {
    align-self: flex-end;
  }
}
</style>
