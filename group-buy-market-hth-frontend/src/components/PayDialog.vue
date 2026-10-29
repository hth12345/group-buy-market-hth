<!--
  PayDialog.vue - 支付弹窗组件（可复用）
  ========================================
  【组件职责】
    展示支付弹窗，包含订单信息、模拟二维码、确认/取消按钮。
    这是一个"可复用组件"，可以在多个页面中使用。

  【类比后端】
    相当于后端的一个"公共 UI 组件" / "工具类"：
    - 封装了支付弹窗的完整逻辑
    - 通过 props 接收外部传入的数据（类比方法参数）
    - 通过 emit 向外部发送事件（类比方法返回值 / 回调函数）

  【可复用组件的核心概念】
    1. Props（属性）：父组件传给子组件的数据（只读）
       - 类比后端：方法的参数 public void show(String title, String content)
       - 父组件使用：<PayDialog :title="订单支付" :orderInfo="order" />

    2. Emit（事件）：子组件向父组件发送消息
       - 类比后端：回调函数 / 监听器
       - 子组件触发：emit('confirm', payload)
       - 父组件监听：<PayDialog @confirm="handleConfirm" />

    3. v-model（双向绑定）：
       - 类比后端：getter + setter 的组合
       - 父组件：<PayDialog v-model:visible="showDialog" />
       - 等价于：<PayDialog :modelValue="showDialog" @update:modelValue="showDialog = $event" />
       - 子组件修改：emit('update:modelValue', false)

  【使用示例】
    在父组件中：
    <template>
      <PayDialog
        v-model:visible="isDialogVisible"
        :orderInfo="currentOrder"
        @confirm="handlePayConfirm"
        @cancel="handlePayCancel"
      />
    </template>

    <script setup>
    import { ref } from 'vue'
    import PayDialog from './PayDialog.vue'

    const isDialogVisible = ref(false)
    const currentOrder = ref({ orderId: 'xxx', payPrice: 99.00 })

    function openPayDialog() {
      isDialogVisible.value = true
    }

    function handlePayConfirm() {
      console.log('用户确认支付')
      isDialogVisible.value = false
    }

    function handlePayCancel() {
      console.log('用户取消支付')
      isDialogVisible.value = false
    }
    </script>
-->

<template>
  <!--
    【Element Plus 组件】el-dialog 弹窗
    - v-model="dialogVisible"：控制弹窗显示/隐藏（双向绑定）
    - title：弹窗标题
    - width：弹窗宽度
    - :close-on-click-modal="false"：点击遮罩层不关闭弹窗
    - :close-on-press-escape="false"：按 ESC 不关闭弹窗
    - custom-class：自定义 CSS 类名（用于覆盖默认样式）

    【前端概念】v-model 双向绑定
    - v-model="dialogVisible" 等价于：
      :modelValue="dialogVisible" + @update:modelValue="dialogVisible = $event"
    - 当弹窗关闭时，Element Plus 会 emit('update:modelValue', false)
    - 然后 dialogVisible 自动变为 false

    【类比后端】
    - props（属性）→ 方法的输入参数
    - emit（事件）→ 方法的返回值 / 回调函数
    - v-model → getter + setter 的组合
  -->
  <el-dialog
    :model-value="visible"
    title="💳 支付确认"
    width="480px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    custom-class="pay-dialog"
    @update:model-value="handleClose"
  >
    <!--
      【前端概念】插槽（slot）
      - el-dialog 内部有预定义的插槽位置（header, default, footer）
      - 我们在 default 位置插入自定义内容
      - 类比后端：模板引擎的 layout:fragment，预留内容区域
    -->

    <!-- ==================== 弹窗内容区域 ==================== -->
    <div class="dialog-content">
      <!-- 订单信息展示 -->
      <div class="order-info">
        <div class="info-row">
          <span class="info-label">订单号：</span>
          <span class="info-value">{{ orderInfo?.orderId || '--' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">组队ID：</span>
          <span class="info-value">{{ orderInfo?.teamId || '--' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">商品名称：</span>
          <span class="info-value">{{ orderInfo?.goodsName || '拼团商品' }}</span>
        </div>
      </div>

      <!-- 金额大字展示 -->
      <div class="amount-section">
        <div class="amount-label">支付金额</div>
        <div class="amount-value">
          ¥{{ Number(orderInfo?.payPrice || 0).toFixed(2) }}
        </div>
      </div>

      <!-- 模拟二维码区域 -->
      <div class="qrcode-section">
        <div class="qrcode-box">
          <!--
            【前端概念】SVG / 图标占位
            实际项目中这里会替换为真实的二维码图片
            可以使用 qrcode 库生成，或从后端获取二维码 URL
          -->
          <div class="qrcode-placeholder">
            <el-icon :size="60" color="#409eff">
              <Iphone />
            </el-icon>
            <p>扫码支付</p>
          </div>
        </div>
        <p class="qrcode-tip">请使用微信或支付宝扫描二维码</p>
      </div>
    </div>

    <!--
      【Element Plus 组件】el-dialog 的 footer 插槽
      - #footer 是具名插槽，用于自定义弹窗底部按钮区域
      - 类比后端：模板引擎的 <div layout:fragment="footer">
    -->
    <template #footer>
      <div class="dialog-footer">
        <!--
          【Element Plus 组件】el-button 按钮
          - @click="handleCancel"：点击时触发 cancel 事件
          - @click="handleConfirm"：点击时触发 confirm 事件
        -->
        <el-button
          type="danger"
          size="large"
          @click="handleCancel"
          class="footer-btn"
        >
          取消支付
        </el-button>
        <el-button
          type="primary"
          size="large"
          @click="handleConfirm"
          class="footer-btn"
        >
          确认已支付
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
/**
 * PayDialog 组件逻辑
 * ====================
 *
 * 【组件通信方式】
 * 1. Props（父 → 子）：父组件通过属性传递数据给子组件
 *    - 类比后端：方法参数 public void showDialog(String title, OrderInfo info)
 *    - 子组件只读，不能修改 props 的值（单向数据流）
 *
 * 2. Emit（子 → 父）：子组件通过事件通知父组件
 *    - 类比后端：回调函数 / 监听器模式
 *    - 子组件：emit('confirm', data)  → 触发事件
 *    - 父组件：@confirm="handleConfirm" → 监听事件
 *
 * 3. v-model（双向绑定）：
 *    - 本质是 props + emit 的组合
 *    - props: modelValue（父传子）
 *    - emit: update:modelValue（子传父）
 *    - 类比后端：getter + setter
 *
 * 【defineProps 和 defineEmits】
 * - defineProps：Vue 3 编译宏，用于声明组件接收的 props
 *   - 类比后端：方法的参数列表
 *   - 不需要 import，Vue 自动提供
 *
 * - defineEmits：Vue 3 编译宏，用于声明组件可以触发的事件
 *   - 类比后端：方法可以抛出的异常类型（throws 子句）
 *   - 不需要 import，Vue 自动提供
 */

// ==================== 导入依赖 ====================

/**
 * 导入 Element Plus 图标
 */
import { Iphone } from '@element-plus/icons-vue'

// ==================== Props 定义 ====================

/**
 * 【前端概念】defineProps 定义组件属性
 *
 * 定义组件接收的 props（父组件传入的数据）
 * 类比后端：方法的参数列表
 *
 * 语法说明：
 * - defineProps<{ propName: type }>() 是 TypeScript 类型声明
 * - 不需要运行时声明，Vue 编译器会自动处理
 *
 * 使用方式：
 * <PayDialog
 *   v-model:visible="showDialog"   ← 对应 props: visible
 *   :orderInfo="order"              ← 对应 props: orderInfo
 * />
 */
defineProps<{
  /**
   * 控制弹窗显示/隐藏
   * - 父组件通过 v-model:visible 传入
   * - 等价于 :visible="true" + @update:visible="..."
   * - 类比后端：boolean isVisible 参数
   */
  visible: boolean

  /**
   * 订单信息对象
   * - 包含 orderId, teamId, payPrice, goodsName 等字段
   * - 父组件通过 :orderInfo 传入
   * - 类比后端：OrderDTO orderInfo 参数
   *
   * 注意：类型声明中使用了 | null，表示该 props 可以为 null
   * （可选属性用 ? 表示，但这里我们要求必须传入，只是值可能为 null）
   */
  orderInfo: {
    orderId?: string
    teamId?: string
    payPrice?: number
    goodsName?: string
  } | null
}>()

// ==================== Emits 定义 ====================

/**
 * 【前端概念】defineEmits 定义组件事件
 *
 * 定义组件可以触发的事件（子组件通知父组件）
 * 类比后端：回调函数 / 监听器
 *
 * 语法说明：
 * - defineEmits<{ (e: 'eventName', payload: Type): void }>()
 * - 声明了三个事件：
 *   1. 'update:visible'：用于 v-model:visible 双向绑定
 *   2. 'confirm'：用户点击"确认已支付"
 *   3. 'cancel'：用户点击"取消支付"
 */
const emit = defineEmits<{
  /**
   * 更新 visible 状态（v-model 必需）
   * - 当弹窗关闭时，触发此事件通知父组件
   * - 父组件接收到后更新自己的 visible 状态
   * - 类比后端：setter 方法 setVisible(false)
   *
   * @param eventName 事件名 'update:visible'
   * @param value 新的 visible 值（true/false）
   */
  (e: 'update:visible', value: boolean): void

  /**
   * 确认支付事件
   * - 用户点击"确认已支付"按钮时触发
   * - 父组件监听此事件执行支付逻辑
   * - 类比后端：回调函数 onConfirm()
   */
  (e: 'confirm'): void

  /**
   * 取消支付事件
   * - 用户点击"取消支付"按钮时触发
   * - 父组件监听此事件执行取消逻辑
   * - 类比后端：回调函数 onCancel()
   */
  (e: 'cancel'): void
}>()

// ==================== 方法 ====================

/**
 * 处理弹窗关闭
 *
 * 【前端概念】v-model 自定义实现
 * - 当 el-dialog 关闭时，会触发 @update:model-value 事件
 * - 我们在这个方法中 emit('update:visible', false) 通知父组件
 * - 父组件接收到后更新自己的 visible 状态，完成双向绑定
 *
 * 类比后端：
 * - 相当于 setter 方法：public void setVisible(boolean visible)
 * - 父组件调用：dialog.setVisible(false) → 触发关闭
 */
function handleClose(value: boolean) {
  emit('update:visible', value)
}

/**
 * 处理确认支付
 *
 * 用户点击"确认已支付"按钮时调用
 * 触发 confirm 事件通知父组件
 *
 * 类比后端：
 * - 相当于按钮的 onclick 事件处理器
 * - 调用回调函数：listener.onConfirm()
 */
function handleConfirm() {
  emit('confirm')
}

/**
 * 处理取消支付
 *
 * 用户点击"取消支付"按钮时调用
 * 触发 cancel 事件通知父组件
 *
 * 类比后端：
 * - 相当于按钮的 onclick 事件处理器
 * - 调用回调函数：listener.onCancel()
 */
function handleCancel() {
  emit('cancel')
}
</script>

<style scoped>
/**
 * 【前端概念】<style scoped>
 * - scoped 表示样式只作用于当前组件
 * - 类比后端的命名空间隔离
 */

/* ==================== 弹窗内容区域 ==================== */
.dialog-content {
  padding: 10px 0;
}

/* ==================== 订单信息展示 ==================== */
.order-info {
  background: #f8faff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px dashed #e2e8f0;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 14px;
  color: #718096;
  font-weight: 500;
}

.info-value {
  font-size: 14px;
  color: #2d3748;
  font-weight: 600;
}

/* ==================== 金额展示区域 ==================== */
.amount-section {
  text-align: center;
  padding: 24px 0;
  background: linear-gradient(135deg, #fff5f5 0%, #fff 100%);
  border-radius: 8px;
  margin-bottom: 20px;
}

.amount-label {
  font-size: 14px;
  color: #718096;
  margin-bottom: 8px;
}

.amount-value {
  font-size: 38px;
  font-weight: 800;
  color: #c53030;
  letter-spacing: 2px;
  text-shadow: 0 2px 4px rgba(197, 48, 48, 0.1);
}

/* ==================== 二维码区域 ==================== */
.qrcode-section {
  text-align: center;
  padding: 20px 0;
}

.qrcode-box {
  display: inline-block;
  padding: 20px;
  border: 2px dashed #cbd5e0;
  border-radius: 12px;
  background: #f7fafc;
}

.qrcode-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.qrcode-placeholder p {
  margin: 0;
  font-size: 14px;
  color: #4a5568;
  font-weight: 500;
}

.qrcode-tip {
  margin-top: 12px;
  font-size: 12px;
  color: #a0aec0;
}

/* ==================== 底部按钮区域 ==================== */
.dialog-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding-top: 8px;
}

.footer-btn {
  min-width: 120px;
  font-weight: 600;
}

/* ==================== Element Plus 组件样式覆盖 ==================== */
/*
  【前端概念】:deep() 深度选择器
  - scoped 样式默认不影响子组件内部元素
  - :deep(.el-xxx) 可以穿透 scoped 限制，修改 Element Plus 组件内部样式
  - 类比后端：Override 覆盖第三方组件的默认样式
*/
:deep(.pay-dialog) {
  border-radius: 16px;
}

:deep(.pay-dialog .el-dialog__header) {
  background: linear-gradient(135deg, #f8faff 0%, #f0f5ff 100%);
  padding: 16px 20px;
  border-bottom: 1px solid #e8ecf4;
}

:deep(.pay-dialog .el-dialog__title) {
  font-size: 18px;
  font-weight: 700;
  color: #1a202c;
}
</style>
