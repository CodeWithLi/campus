import { useUserStore } from '@/stores/user'
import axios from 'axios'
import router from '@/router'
import { ElMessage } from 'element-plus'

// 开发环境建议走 Vite 代理，避免浏览器 CORS 预检（OPTIONS）被 403 拦截
// 生产环境可通过 VITE_API_BASE 覆盖为完整域名
const baseURL = import.meta.env.VITE_API_BASE || '/admin'

// 创建 axios 实例：统一配置 baseURL 与超时时间
const instance = axios.create({
  baseURL,
  timeout: 100000
})

const SUCCESS_CODE = 200
const UNAUTHORIZED_CODE = 401

const redirectToLogin = () => {
  const userStore = useUserStore()
  userStore.clearUserInfo()
  if (router.currentRoute.value.path !== '/login') {
    router.push('/login')
  }
}

// 请求拦截器：自动携带登录 token（后端用 Authorization 识别身份）
instance.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      const headerName = userStore.tokenName || 'Authorization'
      config.headers[headerName] = userStore.token
    }
    return config
  },
  (err) => Promise.reject(err)
)

// 响应拦截器：统一处理业务码/未登录/错误提示
instance.interceptors.response.use(
  (res) => {
    const responseData = res.data || {}
    const { code, msg } = responseData

    // 业务成功：后端统一成功码为 200
    if (code === SUCCESS_CODE) {
      return responseData
    }

    // 未登录/登录过期：提示并跳转到登录页
    if (code === UNAUTHORIZED_CODE) {
      ElMessage({ message: msg || '未登录或登录已过期', type: 'error' })
      redirectToLogin()
      return Promise.reject(responseData)
    }

    // 业务失败：弹出错误信息，并把 data 作为 reject 的内容返回给调用方
    ElMessage({ message: msg || '服务异常', type: 'error' })
    return Promise.reject(responseData)
  },
  (err) => {
    // 网络异常/服务异常：优先读取后端 msg，否则给通用提示
    const message = err.response?.data?.msg || err.message || '服务异常'
    ElMessage({ message, type: 'error' })

    // HTTP 401：强制跳转到登录页（兜底处理）
    if (err.response?.status === 401) {
      redirectToLogin()
    }
    return Promise.reject(err)
  }
)

// 导出 axios 实例：供各 api 模块复用
export default instance
// 导出 baseURL：例如用于拼接图片/文件访问地址
export { baseURL }