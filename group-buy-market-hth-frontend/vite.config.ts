/**
 * vite.config.ts - Vite 构建工具配置
 * ===================================
 * 【前端概念】Vite 是前端项目的构建工具，负责启动开发服务器、编译代码、打包部署
 * 【类比后端】相当于 Maven 的 pom.xml + Spring Boot DevTools 的组合体
 *   - pom.xml 定义项目依赖和构建方式
 *   - DevTools 提供热重载的开发环境
 * 
 * 【关键配置说明】
 * - server.proxy: 开发环境的 API 代理，解决浏览器跨域问题
 *   前端运行在 localhost:5173，后端运行在 localhost:8091
 *   浏览器不允许跨域请求，所以让 Vite 代理转发，就像 Nginx 反向代理
 */
import { defineConfig } from 'vite'      // Vite 的配置定义函数
import vue from '@vitejs/plugin-vue'     // Vue 3 单文件编译插件

export default defineConfig({
  // 插件配置：告诉 Vite 如何编译 .vue 文件
  // 【类比后端】相当于 Maven 中的 plugin 配置，指定编译器如何处理特定类型的文件
  plugins: [vue()],

  // 开发服务器配置
  server: {
    port: 5173,  // 前端开发服务器端口（类比 Tomcat 的 8080）
    proxy: {
      // 拼团后端代理
      // 例如：http://localhost:5173/api/v1/gbm/... → http://localhost:8091/api/v1/gbm/...
      // 【类比后端】相当于 Nginx 的 location /api { proxy_pass http://group-buy-backend; }
      '/api': {
        target: 'http://localhost:8091',  // 拼团后端服务地址
        changeOrigin: true,                // 修改请求头中的 Origin，让后端认为是同源请求
      },
      // 支付后端代理
      // 例如：http://localhost:5173/pay-api/v1/alipay/... → http://localhost:8080/api/v1/alipay/...
      // 【类比后端】相当于 Nginx 的 location /pay-api { proxy_pass http://pay-backend/api; }
      '/pay-api': {
        target: 'http://localhost:8080',  // 支付后端服务地址
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/pay-api/, '/api'),  // 重写路径：/pay-api → /api
      }
    }
  },

  // 路径解析配置
  resolve: {
    // 路径别名：@ 代表 src 目录（类比 Java 的包路径简写）
    // 使用：import Xxx from '@/views/Xxx.vue' 而不是 '../../../views/Xxx.vue'
    alias: {
      '@': '/src'
    }
  }
})
