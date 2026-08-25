import { h } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * 显示提示消息（纯信息，撤回操作通过浮动面板或 Ctrl+Z）
 * @param {Object} options
 * @param {string} options.message - 提示文字
 * @param {number} [options.duration=10000] - 自动关闭时间(ms)
 * @param {string} [options.type='warning'] - 消息类型
 * @returns {{ close: Function }}
 */
export function showUndoToast({ message, duration = 10000, type = 'warning' }) {
  const msgInstance = ElMessage({
    message: h('span', { style: 'font-size:13px;' }, message),
    type,
    duration,
    showClose: true,
  })
  return { close: () => msgInstance.close() }
}
