import { ElMessage } from 'element-plus'
import { onUnmounted } from 'vue'
import { acquireOrderLock, releaseOrderLock } from '@/api/admin'

/**
 * 订单编辑锁 composable
 * @param {string} orderType - 订单类型: sale/purchase/sale_return/purchase_return
 */
export function useOrderLock(orderType) {
  let currentLockOrderId = null

  function onBeforeUnload() {
    if (!currentLockOrderId) return
    const token = sessionStorage.getItem('token')
    const url = `/api/admin/order-lock/release?orderType=${orderType}&orderId=${currentLockOrderId}`
    // fetch keepalive 在页面卸载时仍能发出请求，且支持自定义请求头
    fetch(url, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` },
      keepalive: true,
    })
  }

  /**
   * 尝试加锁，成功返回 true，被他人锁定返回 false（已弹提示）
   */
  async function acquireLock(orderId) {
    if (!orderId) return true
    try {
      const res = await acquireOrderLock(orderType, orderId)
      const data = res.data?.data ?? res.data
      if (data?.locked) {
        ElMessage.warning(`${data.username} 正在编辑此订单，请稍后再试`)
        return false
      }
      currentLockOrderId = orderId
      window.addEventListener('beforeunload', onBeforeUnload)
      return true
    } catch (e) {
      console.error('加锁失败:', e)
      return true // 加锁失败不阻止编辑，降级处理
    }
  }

  /**
   * 释放当前锁
   */
  async function releaseLock() {
    if (!currentLockOrderId) return
    const orderId = currentLockOrderId
    currentLockOrderId = null
    window.removeEventListener('beforeunload', onBeforeUnload)
    try {
      await releaseOrderLock(orderType, orderId)
    } catch (e) {
      console.error('释放锁失败:', e)
    }
  }

  onUnmounted(() => {
    if (currentLockOrderId) {
      window.removeEventListener('beforeunload', onBeforeUnload)
      // 组件卸载时也要释放服务端锁（路由切换等场景）
      const orderId = currentLockOrderId
      currentLockOrderId = null
      const token = sessionStorage.getItem('token')
      fetch(`/api/admin/order-lock/release?orderType=${orderType}&orderId=${orderId}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` },
        keepalive: true,
      })
    }
  })

  return { acquireLock, releaseLock }
}
