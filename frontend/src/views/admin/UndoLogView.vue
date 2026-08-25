<template>
  <div class="page admin-page">
    <div class="page-header">
      <h2>撤回日志</h2>
      <span class="header-hint">记录最近2分钟内的操作，可选择性撤回</span>
    </div>

    <div class="card">
      <div class="log-list" v-if="allOps.length > 0">
        <div
          v-for="op in allOps"
          :key="op.id"
          class="log-row"
          :class="{ undone: op.undone, expired: !op.undone && op.expiresAt <= now }"
        >
          <span class="log-type-icon">{{ op.type === 'delete' ? '🗑' : '✏' }}</span>
          <div class="log-detail">
            <span class="log-action">{{ op.type === 'delete' ? '删除' : '修改' }}</span>
            <span class="log-entity">{{ op.entityLabel }}</span>
            <span class="log-name">「{{ op.displayName }}」</span>
          </div>
          <span class="log-time">{{ timeAgo(op.createdAt) }}</span>
          <span class="log-countdown" v-if="!op.undone && op.expiresAt > now">
            {{ formatRemaining(op.expiresAt) }}
          </span>
          <button
            class="btn-undo"
            :disabled="op.undone || op.expiresAt <= now"
            @click="handleUndo(op.id)"
          >
            {{ op.undone ? '已撤回' : (op.expiresAt <= now ? '已过期' : '撤回此操作') }}
          </button>
        </div>
      </div>
      <div v-else class="empty-state">
        <p>暂无可撤回的操作</p>
        <p class="empty-sub">执行删除或编辑后，操作会出现在这里</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { useUndoLogStore } from '@/composables/undoLogStore'

const { operations, undoOperation } = useUndoLogStore()

// 强制每秒刷新
const now = ref(Date.now())
const tickTimer = setInterval(() => { now.value = Date.now() }, 1000)
onUnmounted(() => clearInterval(tickTimer))

const allOps = computed(() => operations.value)

function formatRemaining(expiresAt) {
  const remaining = Math.max(0, Math.floor((expiresAt - now.value) / 1000))
  const min = Math.floor(remaining / 60)
  const sec = remaining % 60
  return `${min}:${String(sec).padStart(2, '0')}`
}

function timeAgo(ts) {
  if (!ts) return ''
  const diff = Math.floor((now.value - ts) / 1000)
  if (diff < 10) return '刚刚'
  if (diff < 60) return `${diff}秒前`
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  return `${Math.floor(diff / 3600)}小时前`
}

async function handleUndo(id) {
  await undoOperation(id)
}
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 20px;
}
.page-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}
.header-hint {
  font-size: 13px;
  color: #999;
}

.card {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.log-list {
  display: flex;
  flex-direction: column;
}

.log-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  border-bottom: 1px solid #f5f5f5;
  transition: background 0.15s;
}
.log-row:hover {
  background: #fafbfc;
}
.log-row.undone {
  opacity: 0.4;
}
.log-row.expired {
  opacity: 0.5;
}

.log-type-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.log-detail {
  flex: 1;
  font-size: 14px;
  color: #444;
}
.log-action {
  color: #888;
}
.log-entity {
  color: #666;
}
.log-name {
  font-weight: 600;
  color: #1a1a1a;
}

.log-time {
  font-size: 12px;
  color: #bbb;
  white-space: nowrap;
  min-width: 60px;
  text-align: right;
}

.log-countdown {
  font-size: 12px;
  font-weight: 600;
  color: #C5A265;
  font-family: var(--font-mono, monospace);
  min-width: 40px;
  text-align: right;
}

.btn-undo {
  padding: 6px 16px;
  border: 1px solid #C5A265;
  border-radius: 5px;
  background: #fff;
  color: #C5A265;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  font-family: inherit;
}
.btn-undo:hover:not(:disabled) {
  background: #C5A265;
  color: #fff;
}
.btn-undo:disabled {
  border-color: #ddd;
  color: #ccc;
  cursor: not-allowed;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #ccc;
  font-size: 14px;
}
.empty-sub {
  font-size: 12px;
  margin-top: 6px;
  color: #ddd;
}
</style>
