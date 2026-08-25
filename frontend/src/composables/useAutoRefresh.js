/**
 * 定时轮询刷新数据（仅页面可见时执行）
 * @param {Function} fn - 要执行的刷新函数（异步）
 * @param {number} intervalMs - 轮询间隔，默认 30 秒
 * @returns {Function} stop - 清理函数，调用后停止轮询并移除监听
 */
export function useAutoRefresh(fn, intervalMs = 30000) {
  let timer = null

  function start() {
    timer = setInterval(() => {
      if (document.visibilityState === 'visible') {
        fn().catch(() => {})
      }
    }, intervalMs)
  }

  // 切回标签页时立即刷新一次
  function onVisible() {
    if (document.visibilityState === 'visible') {
      fn().catch(() => {})
    }
  }

  document.addEventListener('visibilitychange', onVisible)
  start()

  return function stop() {
    clearInterval(timer)
    document.removeEventListener('visibilitychange', onVisible)
  }
}
