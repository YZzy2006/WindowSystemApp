import axios from 'axios'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

// 静默刷新 token 相关状态
let isRefreshing = false
let refreshQueue = [] // { resolve, reject, config }[]

function clearAuth() {
  try {
    const authStore = useAuthStore()
    authStore.doLogout()
  } catch {
    // Pinia 未初始化时的兜底（理论上不会发生）
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('role')
    sessionStorage.removeItem('loginGeneration')
  }
}

// 请求拦截器 — 自动带 token + Accept 头
request.interceptors.request.use(config => {
  config.headers.Accept = 'application/json'
  const token = sessionStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器 — 静默刷新 + 401/403 处理 + 全局错误兜底
request.interceptors.response.use(
  response => {
    const data = response.data
    if (data && typeof data.code === 'number' && data.code !== 200) {
      if (data.code === 401) {
        if (data.msg) sessionStorage.setItem('kickMsg', data.msg)
        clearAuth()
        router.push('/admin/login')
      }
      const err = new Error(data.msg || '请求失败')
      return Promise.reject(err)
    }
    return response
  },
  async error => {
    const originalConfig = error.config

    // === 401：尝试静默刷新 token ===
    if (error.response?.status === 401 && !originalConfig._retried) {
      // 刷新端点自身返回 401 → 立即登出，不死循环
      if (originalConfig.url === '/admin/refresh-token') {
        isRefreshing = false
        refreshQueue.forEach(({ reject }) => reject(error))
        refreshQueue = []
        clearAuth()
        router.push('/admin/login')
        return Promise.reject(error)
      }

      const token = sessionStorage.getItem('token')

      if (token && !isRefreshing) {
        isRefreshing = true
        originalConfig._retried = true
        try {
          // 直接调用刷新端点，避免循环依赖（标记 _retried 防止刷新失败时再次触发刷新）
          const refreshConfig = { _retried: true }
          const res = await request.post('/admin/refresh-token', null, refreshConfig)
          const newToken = res.data?.data
          if (!newToken) throw new Error('刷新返回空 token')

          // 更新 token
          sessionStorage.setItem('token', newToken)

          isRefreshing = false
          // 重试队列中所有请求
          refreshQueue.forEach(({ resolve, config }) => {
            config.headers.Authorization = `Bearer ${newToken}`
            config._retried = true
            resolve(request(config))
          })
          refreshQueue = []
          // 重试原始请求
          originalConfig.headers.Authorization = `Bearer ${newToken}`
          return request(originalConfig)
        } catch (refreshError) {
          isRefreshing = false
          refreshQueue.forEach(({ reject }) => reject(refreshError))
          refreshQueue = []
          // 刷新失败 → 登出
          clearAuth()
          router.push('/admin/login')
          return Promise.reject(refreshError)
        }
      } else if (isRefreshing) {
        // 正在刷新中 → 排队等待
        return new Promise((resolve, reject) => {
          refreshQueue.push({ resolve, reject, config: originalConfig })
        })
      }

      // 无 token 或已在重试 → 直接登出
      clearAuth()
      router.push('/admin/login')
      return Promise.reject(error)
    }

    // === 403：冻结账号 ===
    if (error.response?.status === 403) {
      sessionStorage.setItem('kickMsg', error.response?.data?.msg || '账号已被冻结')
      clearAuth()
      router.push('/admin/login')
      error._handled = true
      return Promise.reject(error)
    }

    // === 全局兜底：超时 / 断网 / 429 / 5xx ===
    let msg = ''
    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      msg = '请求超时，请检查网络后重试'
    } else if (!error.response) {
      msg = '网络连接失败，请检查网络'
    } else if (error.response.status === 429) {
      msg = error.response.data?.msg || '请求过于频繁，请稍后再试'
    } else if (error.response.status >= 500) {
      msg = '服务器错误，请稍后重试'
    }

    if (msg) {
      import('element-plus').then(({ ElMessage }) => ElMessage.error(msg))
      error._handled = true
    }

    return Promise.reject(error)
  }
)

export default request
