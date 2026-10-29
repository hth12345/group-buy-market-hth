/**
 * request.ts - Axios 请求封装
 * =============================
 *
 * 【前端概念】Axios 是什么？
 * Axios 是前端最流行的 HTTP 请求库，用来向后端发送网络请求（GET/POST/PUT/DELETE 等）
 * 【类比后端】
 *   - 相当于 Java 的 HttpClient / OkHttp / RestTemplate
 *   - 你在后端写 Feign Client 或 RestTemplate.getForObject() 做的事情，前端就用 Axios
 *
 * 【为什么要封装？】
 * 如果不封装，每个接口调用都要写一堆重复代码：
 *   axios.post('/api/xxx', data, { headers: { 'Content-Type': 'application/json' } })
 *     .then(res => { if (res.data.code === '0000') { ... } else { alert(res.data.info) } })
 *     .catch(err => { console.error(err); alert('网络错误') })
 *
 * 封装后，调用方只需关心业务：
 *   const res = await request.post('/api/xxx', data)
 *   // res 直接就是 data 部分，错误自动处理
 *
 * 【类比后端】
 *   - 封装 Axios ≈ 封装一个统一的 HttpClientUtil 工具类
 *   - 拦截器 ≈ Servlet Filter / Spring HandlerInterceptor
 *     请求拦截器 = 在请求发出前统一加 Header（类比 Filter 的 pre-filter）
 *     响应拦截器 = 在响应回来后统一处理错误（类比 Filter 的 post-filter）
 *   - 统一错误处理 ≈ @ControllerAdvice + @ExceptionHandler 全局异常处理
 */

import axios from 'axios'           // 引入 Axios 库（类比 import java.net.http.HttpClient）
import { ElMessage } from 'element-plus' // 引入 Element Plus 消息提示组件（类比后端的弹窗/Toast）
import type { ApiResponse } from './types' // 引入统一响应类型

// =============================================
// 一、创建 Axios 实例（类比创建 HttpClient 实例并配置默认参数）
// =============================================

/**
 * 创建 Axios 实例
 * 【类比后端】相当于 new HttpClient.Builder().baseUrl("http://xxx").build()
 *
 * baseURL: 所有请求的基础 URL（类比 FeignClient 的 url 属性）
 *   - 开发环境：Vite 代理会将 /api 转发到 http://localhost:8091
 *   - 所以这里写 /api 即可，不需要写完整地址
 *
 * timeout: 请求超时时间（类比 HttpClient 的连接超时/读取超时设置）
 *   - 超过 15 秒没响应就自动报错，避免用户一直等待
 */
const request = axios.create({
  baseURL: '/api',    // 基础路径，所有请求都会自动加上这个前缀
  timeout: 15000,     // 超时时间 15 秒（单位：毫秒）
})

// =============================================
// 二、请求拦截器（类比 Servlet Filter 的 pre-filter 阶段）
// =============================================

/**
 * 请求拦截器：在每个请求发出之前自动执行
 * 【类比后端】
 *   - 相当于 Spring MVC 的 HandlerInterceptor.preHandle()
 *   - 或者 Servlet Filter 中 chain.doFilter() 之前的逻辑
 *   - 在这里统一添加请求头，不用每个接口手动加
 */
request.interceptors.request.use(
  (config) => {
    // config 就是这次请求的配置对象（URL、请求头、请求体等）

    // 统一设置 Content-Type 为 JSON 格式
    // 【类比后端】相当于 @RequestBody 注解要求请求体是 JSON 格式
    // 如果不设置，Axios 默认也会加，但显式设置更清晰
    if (!config.headers['Content-Type']) {
      config.headers['Content-Type'] = 'application/json'
    }

    // 【可扩展】这里还可以加 Token 认证头，类比后端 JWT Filter：
    // const token = localStorage.getItem('token')
    // if (token) config.headers['Authorization'] = `Bearer ${token}`

    return config  // 必须返回 config，否则请求会被中断
  },
  (error) => {
    // 请求配置出错（极少发生）
    return Promise.reject(error)  // 将错误继续向下传递
  }
)

// =============================================
// 三、响应拦截器（类比 @ControllerAdvice 全局异常处理）
// =============================================

/**
 * 响应拦截器：在后端响应返回后、业务代码处理前自动执行
 * 【类比后端】
 *   - 相当于 @ControllerAdvice + @ExceptionHandler 统一处理响应
 *   - 或者 Feign 的 ErrorDecoder 统一处理异常响应
 *
 * 为什么要在这里处理？
 *   - 后端返回 { code: "0000", data: {...} }，业务代码只关心 data
 *   - 如果 code 不是 "0000"，说明业务出错，直接弹窗提示，不需要每个接口都判断
 *   - 网络错误（断网、超时）也在这里统一提示
 */
request.interceptors.response.use(
  (response) => {
    // 【成功响应】HTTP 状态码 2xx 会进入这里
    // response.data 就是后端返回的 JSON 对象（类型为 ApiResponse<T>）
    const res = response.data as ApiResponse<any>

    // 判断业务状态码：后端约定 "0000" 表示成功
    // 【类比后端】相当于 Response.SUCCESS_CODE.equals(res.code)
    if (res.code === '0000') {
      // 业务成功：直接返回 data 部分（解包，让调用方拿到的是纯业务数据）
      // 这样调用方拿到的就是 data 字段的内容，不需要再 res.data.data
      return res.data
    } else {
      // 业务失败：后端返回了错误码（如参数校验失败、库存不足等）
      // 用 Element Plus 的 Message 组件弹出错误提示（类比后端的 response.sendError()）
      ElMessage.error(res.info || '操作失败')
      // 返回 Promise.reject，让调用方的 catch 能捕获到这个错误
      // 【类比后端】相当于 throw new BusinessException(res.code, res.info)
      return Promise.reject(new Error(res.info || '操作失败'))
    }
  },
  (error) => {
    // 【网络错误】HTTP 状态码非 2xx（如 404、500）或网络断开、超时
    // 【类比后端】相当于 HttpClient 抛出 IOException / TimeoutException

    let message = '网络请求失败'  // 默认错误提示

    if (error.response) {
      // 服务器有响应，但状态码是 4xx/5xx
      // 【类比后端】相当于收到了 HttpServletResponse 的错误状态
      switch (error.response.status) {
        case 400:
          message = '请求参数错误（400）'
          break
        case 401:
          message = '未授权，请重新登录（401）'
          break
        case 403:
          message = '拒绝访问（403）'
          break
        case 404:
          message = '请求资源不存在（404）'
          break
        case 500:
          message = '服务器内部错误（500）'
          break
        default:
          message = `请求失败（${error.response.status}）`
      }
    } else if (error.code === 'ECONNABORTED') {
      // 请求超时（超过了上面设置的 15 秒）
      // 【类比后端】相当于 SocketTimeoutException
      message = '请求超时，请稍后重试'
    } else if (!navigator.onLine) {
      // 浏览器断网
      message = '网络连接已断开，请检查网络'
    }

    // 弹出错误提示（类比后端的 log.error() + 返回错误响应给客户端）
    ElMessage.error(message)

    // 将错误继续向下传递，让调用方的 .catch() 也能处理
    return Promise.reject(error)
  }
)

// =============================================
// 四、导出封装好的 Axios 实例
// =============================================

/**
 * 导出 request 实例，供其他 API 文件使用
 * 【类比后端】相当于把 HttpClientUtil 注册为 Spring Bean，其他 Service 可以 @Autowired 注入
 *
 * 使用方式：
 *   import request from '@/api/request'
 *   request.post('/v1/gbm/xxx', data)  // 发送 POST 请求
 *   request.get('/v1/gbm/xxx')         // 发送 GET 请求
 */
export default request
