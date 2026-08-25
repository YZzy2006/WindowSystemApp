import { ref } from 'vue'

const MAX_ITEMS = 8

function getStorageKey() {
  const token = sessionStorage.getItem('token') || ''
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return `recent_orders_${payload.sub || 'guest'}`
  } catch {
    return 'recent_orders_guest'
  }
}

function read() {
  try {
    return JSON.parse(localStorage.getItem(getStorageKey())) || []
  } catch {
    return []
  }
}

function write(list) {
  localStorage.setItem(getStorageKey(), JSON.stringify(list))
}

// 模块级单例，所有组件共享同一个响应式引用
const recentOrders = ref(read())

export function useRecentOrders() {
  function addRecent(order) {
    if (!order || !order.id) return
    const item = {
      id: order.id,
      orderNo: order.orderNo || '',
      type: order.type || '',
      customerName: order.customerName || order.supplierName || ''
    }
    const key = o => `${o.id}-${o.type}`
    const list = read().filter(o => key(o) !== key(item))
    list.unshift(item)
    if (list.length > MAX_ITEMS) list.length = MAX_ITEMS
    write(list)
    recentOrders.value = list
  }

  function clearRecent() {
    localStorage.removeItem(getStorageKey())
    recentOrders.value = []
  }

  return { recentOrders, addRecent, clearRecent }
}
