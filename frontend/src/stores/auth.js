import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi, refreshToken as refreshTokenApi } from '@/api/admin'
import request from '@/api/index'

const REFRESH_INTERVAL = 2 * 60 * 60 * 1000 // 2小时刷新一次（token有效期3小时）
const HEARTBEAT_INTERVAL = 30 * 1000 // 30秒心跳检测一次
const VISIBILITY_REFRESH_THRESHOLD = 30 * 60 * 1000 // 页面重新可见超过30分钟则立即刷新

export const useAuthStore = defineStore('auth', () => {
  const token = ref(sessionStorage.getItem('token') || '')
  const role = ref(sessionStorage.getItem('role') || 'admin')
  const isLoggedIn = ref(!!token.value)

  const error = ref('')
  let refreshTimer = null
  let heartbeatTimer = null
  let lastRefreshTime = Date.now()
  let loginGeneration = parseInt(sessionStorage.getItem('loginGeneration') || '0')
  let sessionEventSource = null

  async function doRefresh() {
    try {
      const res = await refreshTokenApi()
      const newToken = res.data?.data
      if (newToken) {
        token.value = newToken
        sessionStorage.setItem('token', newToken)
        lastRefreshTime = Date.now()
      }
    } catch (e) {
      console.warn('token刷新失败:', e.message)
      doLogout()
    }
  }

  function startRefreshTimer() {
    stopRefreshTimer()
    refreshTimer = setInterval(doRefresh, REFRESH_INTERVAL)
  }

  function onVisibilityChange() {
    if (document.visibilityState === 'visible') {
      const elapsed = Date.now() - lastRefreshTime
      if (elapsed > VISIBILITY_REFRESH_THRESHOLD) {
        doRefresh()
      }
    }
  }

  function startVisibilityListener() {
    document.addEventListener('visibilitychange', onVisibilityChange)
  }

  function stopVisibilityListener() {
    document.removeEventListener('visibilitychange', onVisibilityChange)
  }

  function startHeartbeat() {
    stopHeartbeat()
    heartbeatTimer = setInterval(async () => {
      try {
        await request.get('/admin/ping')
      } catch (e) {
        // 401/403 时清理本地状态（拦截器负责跳转，这里负责清定时器和内存状态）
        if (e.response?.status === 401 || e.response?.status === 403) {
          doLogout()
        } else if (!sessionStorage.getItem('token')) {
          // token 被外部清除（如密码修改页面），清理定时器和内存状态
          doLogout()
        }
      }
    }, HEARTBEAT_INTERVAL)
  }

  function stopRefreshTimer() {
    if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  }

  function stopHeartbeat() {
    if (heartbeatTimer) { clearInterval(heartbeatTimer); heartbeatTimer = null }
  }

  function connectSessionEvents() {
    disconnectSessionEvents()
    const currentToken = sessionStorage.getItem('token')
    if (!currentToken) return
    const es = new EventSource(`/api/admin/session/events?token=${encodeURIComponent(currentToken)}`)
    es.addEventListener('kick', (e) => {
      sessionStorage.setItem('kickMsg', e.data || '您的账号在其他设备登录')
      doLogout()
      window.location.hash = '#/admin/login'
    })
    es.onerror = () => {
      es.close()
      sessionEventSource = null
      // 自动重连（仅在仍登录状态下）
      if (isLoggedIn.value) {
        setTimeout(() => connectSessionEvents(), 3000)
      }
    }
    sessionEventSource = es
  }

  function disconnectSessionEvents() {
    if (sessionEventSource) {
      sessionEventSource.close()
      sessionEventSource = null
    }
  }

  function doLogout() {
    const currentToken = sessionStorage.getItem('token')
    const currentGen = parseInt(sessionStorage.getItem('loginGeneration') || '0')
    if (currentToken && currentToken !== token.value) return
    if (currentGen !== loginGeneration) return
    stopRefreshTimer()
    stopHeartbeat()
    stopVisibilityListener()
    disconnectSessionEvents()
    token.value = ''
    role.value = 'admin'
    isLoggedIn.value = false
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('role')
    sessionStorage.removeItem('loginGeneration')
  }

  async function login(username, password) {
    error.value = ''
    try {
      const res = await loginApi({ username, password })
      const data = res.data?.data
      if (!data?.token) throw new Error('登录失败：未获取到token')
      token.value = data.token
      role.value = data.role || 'admin'
      isLoggedIn.value = true
      loginGeneration++
      sessionStorage.setItem('token', data.token)
      sessionStorage.setItem('role', role.value)
      sessionStorage.setItem('adminName', username)
      sessionStorage.setItem('loginGeneration', String(loginGeneration))
      lastRefreshTime = Date.now()
      startRefreshTimer()
      startHeartbeat()
      startVisibilityListener()
      connectSessionEvents()
      return data.token
    } catch (e) {
      error.value = e.response?.data?.msg || e.message || '登录失败'
      throw e
    }
  }

  async function logout() {
    try { await logoutApi() } catch {}
    doLogout()  // 无论服务端是否成功，都清本地状态
  }

  function clearError() {
    error.value = ''
  }

  // 页面加载时如果已有 token，启动定时器
  if (token.value) {
    startRefreshTimer()
    startHeartbeat()
    startVisibilityListener()
    connectSessionEvents()
  }

  // 首次进入管理端时验证 session 是否仍有效（token 可能在服务端已失效）
  async function validateSession() {
    if (!token.value) return
    try {
      await request.get('/admin/ping')
    } catch {
      // 401/403 → 拦截器已处理跳转，这里无需额外操作
    }
  }

  return { token, role, isLoggedIn, error, login, logout, clearError, doLogout, validateSession }
})
