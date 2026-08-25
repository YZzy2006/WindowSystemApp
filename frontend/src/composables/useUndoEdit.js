import { onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { showUndoToast } from './undoToast'
import { useUndoLogStore } from './undoLogStore'

/**
 * 可回撤的编辑操作（最多保留最近N次修改的快照，超出自动丢弃最早的）
 * @param {Object} options
 * @param {Function} options.updateApi - 更新API (id, data) => Promise
 * @param {Function} [options.onSuccess] - 回撤成功后的回调
 * @param {Function} [options.onError] - 回撤失败后的回调
 * @param {string} [options.entityLabel='记录'] - 实体名称
 * @param {number} [options.delay=120000] - 可回撤时间窗口(ms)，默认2分钟
 * @param {number} [options.maxPending=5] - 最多保留的可回撤次数
 */
export function useUndoEdit({ updateApi, onSuccess, onError, entityLabel = '记录', delay = 120000, maxPending = 5 }) {
  const stack = []
  const undoLog = useUndoLogStore()

  function formatDelay() {
    return delay >= 60000 ? `${delay / 60000}分钟` : `${delay / 1000}秒`
  }

  function snapshotBeforeEdit(row) {
    stack.push({ snapshot: JSON.parse(JSON.stringify(row)), entityId: row.id })
    while (stack.length > maxPending) stack.shift()
  }

  function afterEditSave(savedRow, isDirty = true) {
    if (!stack.length) return
    const entityId = savedRow.id
    const idx = stack.findIndex(e => e.entityId === entityId)
    if (idx === -1) return
    const entry = stack[idx]

    // 从栈中移除已处理的条目
    stack.splice(idx, 1)

    // 没有实际改动，不注册撤回
    if (!isDirty) return

    const displayName = savedRow.name || savedRow.orderNo || `#${entityId}`

    showUndoToast({
      message: `已修改${entityLabel}「${displayName}」，${formatDelay()}内可回撤`,
      duration: Math.min(delay, 10000),
    })

    undoLog.addOperation({
      type: 'edit',
      entityLabel,
      displayName,
      entityId,
      undoFn: async () => {
        try {
          await updateApi(entityId, entry.snapshot)
          onSuccess?.()
          ElMessage.success('已恢复')
        } catch (err) {
          onError?.(err)
        }
      },
      duration: delay,
    })
  }

  onUnmounted(() => {
    stack.length = 0
  })

  return { snapshotBeforeEdit, afterEditSave }
}
