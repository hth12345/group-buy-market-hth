/**
 * env.d.ts - Vue 单文件组件的类型声明
 * ====================================
 * 【前端概念】TypeScript 默认不认识 .vue 文件，需要告诉它 .vue 文件的类型
 * 【类比后端】相当于 Java 中的接口声明，告诉编译器"这个类型的对象有哪些方法"
 * 
 * 这个文件的作用：
 * 当你 import Xxx from './Xxx.vue' 时，TypeScript 不会报错说"找不到模块"
 */

/// <reference types="vite/client" />

// 声明 .vue 文件的模块类型
// 【类比后端】相当于告诉编译器：所有 .vue 文件都是 Vue 组件类型
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}
