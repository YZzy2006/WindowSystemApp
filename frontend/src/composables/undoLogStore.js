import { reactive, computed } from 'vue'

/**
 * 全局撤回日志存储
 * 统一管理所有可撤回操作的 timer 和生命周期
 */
const state = reactive({
  operations: []
})

let counter = 0

function addOperation({ type, entityLabel, displayName, entityId, entityType, undoFn, duration = 120000 }) {
  const id = `log-${++counter}-${Date.now()}`
  const now = Date.now()
  const op = reactive({
    id,
    type, // 'delete' | 'edit' | 'action'
    entityLabel,
    displayName,
    entityId,
    entityType,
    undoFn,
    createdAt: now,
    expiresAt: now + duration,
    duration,
    undone: false,
  })

  state.operations.unshift(op)

  // 最多保留20条记录
  while (state.operations.length > 20) {
    state.operations.pop()
  }

  return id
}

async function undoOperation(id) {
  const op = state.operations.find(o => o.id === id)
  if (!op || op.undone || op.expiresAt <= Date.now()) return
  op.undone = true
  try {
    await op.undoFn()
  } finally {
    // 撤回后立即移除
    const idx = state.operations.findIndex(o => o.id === id)
    if (idx !== -1) state.operations.splice(idx, 1)
  }
}

function removeOperation(id) {
  const idx = state.operations.findIndex(op => op.id === id)
  if (idx !== -1) state.operations.splice(idx, 1)
}

function clearAll() {
  state.operations.length = 0
}

// 定时清理过期操作
setInterval(() => {
  const now = Date.now()
  for (let i = state.operations.length - 1; i >= 0; i--) {
    if (state.operations[i].expiresAt <= now && !state.operations[i].undone) {
      state.operations.splice(i, 1)
    }
  }
}, 5000)

const operations = computed(() => state.operations)
const count = computed(() => state.operations.filter(op => !op.undone && op.expiresAt > Date.now()).length)

export function useUndoLogStore() {
  return {
    operations,
    count,
    addOperation,
    removeOperation,
    undoOperation,
    clearAll,
  }
}
