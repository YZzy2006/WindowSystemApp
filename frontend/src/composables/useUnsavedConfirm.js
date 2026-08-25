import { ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'

/**
 * 弹窗未保存修改确认
 * @param {Ref} visible - 弹窗显示状态
 * @param {Function} getSnapshot - 获取当前表单快照（JSON.stringify 用）
 * @param {Object} opts
 * @param {string} opts.message - 确认提示文字
 * @param {Object} opts.form - 可选，reactive 表单对象，传入后自动深度监听变化
 * @returns {{ formDirty, markDirty, resetDirty, closeGuard }}
 */
export function useUnsavedConfirm(visible, getSnapshot, opts = {}) {
  const message = opts.message || '有未保存的修改，确定关闭吗？'
  const formDirty = ref(false)
  let snapshot = ''
  let ready = false

  watch(visible, (val) => {
    if (val) {
      snapshot = getSnapshot()
      formDirty.value = false
      ready = true
    } else {
      ready = false
    }
  })

  // 自动深度监听表单对象
  if (opts.form) {
    watch(() => opts.form, () => {
      if (!ready) return
      formDirty.value = getSnapshot() !== snapshot
    }, { deep: true })
  }

  function markDirty() {
    if (!visible.value) return
    formDirty.value = getSnapshot() !== snapshot
  }

  function resetDirty() {
    formDirty.value = false
  }

  function closeGuard(done) {
    if (!formDirty.value) {
      if (done) done(); else return true
      return
    }
    return ElMessageBox.confirm(message, '提示', {
      confirmButtonText: '确定关闭',
      cancelButtonText: '继续编辑',
      type: 'warning',
    }).then(() => {
      formDirty.value = false
      if (done) done()
      return true
    }).catch(() => false)
  }

  return { formDirty, markDirty, resetDirty, closeGuard }
}
