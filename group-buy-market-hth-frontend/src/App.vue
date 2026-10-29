<!--
  App.vue - 根组件
  ================
  【前端概念】这是整个应用的"外壳"，所有页面都渲染在这个组件内部
  【类比后端】相当于一个统一的 Layout 模板，类似 Thymeleaf 的 layout:fragment
  
  <router-view /> 是路由出口，当前路由匹配的页面组件会渲染在这里
  类比：Controller 根据 URL 路径选择不同的 Handler，router-view 根据路径选择不同的 View

  Vue 单文件组件（.vue）包含三部分：
  - <template>：HTML 模板（类比 Thymeleaf/JSP 页面）
  - <script>：JS/TS 逻辑（类比 Controller 中的业务代码）
  - <style>：CSS 样式（类比 CSS 文件）
-->
<template>
  <!-- 
    整个应用的外壳
    所有页面内容都会渲染在 <router-view /> 的位置
    【类比后端】类似 Spring MVC 中 DispatcherServlet 根据 URL 选择不同 Controller
    这里 router 根据 URL 选择不同 View 组件渲染到 router-view 位置
  -->
  <div id="app">
    <router-view />
  </div>
</template>

<script setup lang="ts">
/**
 * <script setup> 是 Vue 3 的语法糖
 * 【类比后端】相当于一个 Java 类，里面写字段和方法
 * setup 语法糖让你不用写 return，模板中可以直接使用这里定义的变量和函数
 * lang="ts" 表示使用 TypeScript（带类型检查的 JavaScript）
 * 
 * 目前根组件不需要额外的逻辑，所有业务逻辑在各个页面组件中
 */
</script>

<style>
/* 全局样式重置 - 清除浏览器默认样式，保证各浏览器显示一致 */
* {
  margin: 0;           /* 外边距归零（类比 CSS reset） */
  padding: 0;          /* 内边距归零 */
  box-sizing: border-box;  /* 宽高包含 border 和 padding（更直观的尺寸计算） */
}

body {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', Arial, sans-serif;
  background-color: #f5f5f5;  /* 浅灰背景色 */
  color: #333;                 /* 深灰文字色 */
}
</style>
