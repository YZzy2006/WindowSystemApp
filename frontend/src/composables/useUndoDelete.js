import { ElMessageBox, ElMessage } from 'element-plus'
import { showUndoToast } from './undoToast'
import { useUndoLogStore } from './undoLogStore'

/**
 * 可回撤的删除操作 — 立即执行删除，2分钟内可撤回（调恢复接口）
 * @param {Object} options
 * @param {Function} options.deleteApi - 删除API (id) => Promise
 * @param {Function} options.restoreApi - 恢复API (id) => Promise
 * @param {Function} [options.onSuccess] - 操作成功后的回调
 * @param {Function} [options.onError] - 操作失败后的回调
 * @param {string} [options.entityLabel='记录'] - 实体名称
 * @param {number} [options.delay=120000] - 可回撤时间窗口(ms)，默认2分钟
 */
export function useUndoDelete({ deleteApi, restoreApi, onSuccess, onError, entityLabel = '记录', delay = 120000 }) {
  const undoLog = useUndoLogStore()

  function formatDelay() {
    return delay >= 60000 ? `${delay / 60000}分钟` : `${delay / 1000}秒`
  }

  async function handleDelete(row) {
    const displayName = row.name || row.orderNo || `#${row.id}`
    try {
      await ElMessageBox.confirm(
        `确定删除${entityLabel}「${displayName}」？${formatDelay()}内可回撤。`,
        '删除确认',
        { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
      )
    } catch {
      return
    }

    try {
      await deleteApi(row.id)
      onSuccess?.()
    } catch (err) {
      if (onError) {
        onError(err)
      } else {
        const msg = err?.response?.data?.msg || err?.response?.data?.message || err?.message || '删除失败'
        ElMessage.error(msg)
      }
      return
    }

    showUndoToast({
      message: `已删除${entityLabel}「${displayName}」，${formatDelay()}内可回撤`,
      duration: Math.min(delay, 10000),
    })

    undoLog.addOperation({
      type: 'delete',
      entityLabel,
      displayName,
      entityId: row.id,
      undoFn: async () => {
        try {
          await restoreApi(row.id)
          onSuccess?.()
          ElMessage.success('已恢复')
        } catch (err) {
          if (onError) {
            onError(err)
          } else {
            ElMessage.error('恢复失败')
          }
        }
      },
      duration: delay,
    })
  }

  async function handleBatchDelete(ids) {
    if (!ids.length) return
    try {
      await ElMessageBox.confirm(
        `确定删除选中的 ${ids.length} 条${entityLabel}？${formatDelay()}内可回撤。`,
        '批量删除确认',
        { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
      )
    } catch {
      return
    }

    try {
      await Promise.all(ids.map(id => deleteApi(id)))
      onSuccess?.()
    } catch (err) {
      if (onError) {
        onError(err)
      } else {
        const msg = err?.response?.data?.msg || err?.response?.data?.message || err?.message || '删除失败'
        ElMessage.error(msg)
      }
      return
    }

    showUndoToast({
      message: `已删除 ${ids.length} 条${entityLabel}，${formatDelay()}内可回撤`,
      duration: Math.min(delay, 10000),
    })

    undoLog.addOperation({
      type: 'delete',
      entityLabel,
      displayName: `${ids.length}条${entityLabel}`,
      entityId: ids[0],
      undoFn: async () => {
        try {
          await Promise.all(ids.map(id => restoreApi(id)))
          onSuccess?.()
          ElMessage.success('已恢复')
        } catch (err) {
          if (onError) {
            onError(err)
          } else {
            ElMessage.error('恢复失败')
          }
        }
      },
      duration: delay,
    })
  }

  return { handleDelete, handleBatchDelete }
}
