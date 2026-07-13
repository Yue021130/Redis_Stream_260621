import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * axios 实例。
 *
 * - baseURL 指向后端地址
 * - 响应拦截器统一处理后端 ApiResponse<T> 结构
 * - 直接返回 response.data.data，调用方无需再拆包
 */
const request = axios.create({
  // 使用相对路径，开发时由 Vite 代理转发到后端；生产环境可配置为真实域名
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器：可在这里加 loading、token 等
request.interceptors.request.use(
  (config) => config,
  (error) => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端统一返回 { code, message, data }
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
  },
  (error) => {
    const message = error.message || '网络错误'
    // 避免连续弹出多个错误提示
    if (!error.config?.__noErrorTip) {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export default request
