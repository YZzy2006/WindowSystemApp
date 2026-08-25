import { ElMessageBox, ElMessage } from 'element-plus'
import { showUndoToast } from './undoToast'
import { useUndoLogStore } from './undoLogStore'

/**
 * 通用可回撤操作（批量标星、批量上下架、批量已读等）
 * @param {Object} options
 * @param {Function} options.actionApi - 操作API (id) => Promise
 * @param {Function} [options.reverseApi] - 撤回时调用的API (id) => Promise（与 actionApi 互为逆操作）
 * @param {Function} [options.onSuccess] - 操作成功后的回调
 * @param {Function} [options.onError] - 操作失败后的回调
 * @param {string} [options.entityLabel='记录'] - 实体名称
 * @param {string} [options.actionLabel='操作'] - 操作名称
 * @param {number} [options.delay=120000] - 可回撤时间窗口(ms)，默认2分钟
 */
export function useUndoAction({ actionApi, reverseApi, onSuccess, onError, entityLabel = '记录', actionLabel = '操作', delay = 120000 }) {
  const undoLog = useUndoLogStore()

  function formatDelay() {
    return delay >= 60000 ? `${delay / 60000}分钟` : `${delay / 1000}秒`
  }

  async function handleBatchAction(ids, displayName) {
    if (!ids.length) return
    try {
      await ElMessageBox.confirm(
        `确定${actionLabel}选中的 ${ids.length} 条${entityLabel}？${formatDelay()}内可回撤。`,
        `批量${actionLabel}确认`,
        { confirmButtonText: `确定${actionLabel}`, cancelButtonText: '取消', type: 'warning' }
      )
    } catch {
      return
    }

    try {
      await Promise.all(ids.map(id => actionApi(id)))
      onSuccess?.()
    } catch (err) {
      onError?.(err)
      return
    }

    const undoFn = reverseApi
      ? async () => {
          try {
            await Promise.all(ids.map(id => reverseApi(id)))
            onSuccess?.()
            ElMessage.success('已恢复')
          } catch (err) {
            onError?.(err)
          }
        }
      : () => {}

    const label = displayName || `${ids.length}条${entityLabel}`
    showUndoToast({
      message: `已${actionLabel}${label}，${formatDelay()}内可回撤`,
      duration: Math.min(delay, 10000),
    })

    undoLog.addOperation({
      type: 'action',
      entityLabel: actionLabel + entityLabel,
      displayName: label,
      entityId: ids[0],
      undoFn,
      duration: delay,
    })
  }

  return { handleBatchAction }
}
