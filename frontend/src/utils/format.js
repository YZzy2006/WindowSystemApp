export function formatDate(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}`
}

/** 金额乘法（避免浮点精度丢失） */
export function multiplyMoney(a, b) {
  if (a == null || b == null) return 0
  return Math.round(a * b * 100) / 100
}

/** 金额加法（避免浮点精度丢失） */
export function addMoney(a, b) {
  return Math.round(((a || 0) + (b || 0)) * 100) / 100
}

/** 格式化金额（保留2位小数，千分位） */
export function formatMoney(val) {
  if (val == null || isNaN(val)) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
