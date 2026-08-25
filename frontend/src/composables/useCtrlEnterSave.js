import { watch, onUnmounted } from 'vue'

/**
 * 对话框打开时注册 Ctrl+Enter 快捷保存，关闭时自动移除
 * @param {import('vue').Ref<boolean>} dialogVisible - 对话框显示状态
 * @param {Function} handleSave - 保存函数
 */
export function useCtrlEnterSave(dialogVisible, handleSave) {
  function onKeydown(e) {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault()
      handleSave()
    }
  }

  watch(dialogVisible, (val) => {
    if (val) {
      setTimeout(() => document.addEventListener('keydown', onKeydown), 50)
    } else {
      document.removeEventListener('keydown', onKeydown)
    }
  })

  onUnmounted(() => {
    document.removeEventListener('keydown', onKeydown)
  })
}
