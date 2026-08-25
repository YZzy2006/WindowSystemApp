<template>
  <div class="undo-panel-wrapper" v-if="count > 0 || expanded">
    <!-- 悬浮按钮 -->
    <div class="undo-fab" @click="expanded = !expanded" :class="{ expanded }">
      <span class="undo-fab-icon">↩</span>
      <span class="undo-fab-badge" v-if="count > 0">{{ count }}</span>
    </div>

    <!-- 展开面板 -->
    <transition name="undo-panel">
      <div class="undo-panel" v-if="expanded">
        <div class="undo-panel-header">
          <span class="undo-panel-title">撤回操作</span>
          <button class="undo-panel-close" @click="expanded = false">✕</button>
        </div>
        <div class="undo-panel-list">
          <div v-for="op in activeOps" :key="op.id" class="undo-panel-item">
            <span class="undo-item-icon">{{ op.type === 'delete' ? '🗑' : '✏' }}</span>
            <div class="undo-item-info">
              <span class="undo-item-text">
                {{ op.type === 'delete' ? '删除' : '修改' }}{{ op.entityLabel }}「{{ op.displayName }}」
              </span>
              <span class="undo-item-timer">{{ formatRemaining(op.expiresAt) }}</span>
            </div>
            <button class="undo-item-btn" @click="handleUndo(op.id)">撤回</button>
          </div>
          <div v-if="activeOps.length === 0" class="undo-panel-empty">
            暂无可撤回的操作
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { useUndoLogStore } from '@/composables/undoLogStore'

const { operations, count, undoOperation } = useUndoLogStore()
const expanded = ref(false)

// 强制每秒刷新倒计时
const tick = ref(0)
const tickTimer = setInterval(() => { tick.value++ }, 1000)
onUnmounted(() => clearInterval(tickTimer))

const activeOps = computed(() => {
  // eslint-disable-next-line no-unused-vars
  const _ = tick.value // 触发响应式更新
  const now = Date.now()
  return operations.value.filter(op => !op.undone && op.expiresAt > now)
})

function formatRemaining(expiresAt) {
  // eslint-disable-next-line no-unused-vars
  const _ = tick.value
  const remaining = Math.max(0, Math.floor((expiresAt - Date.now()) / 1000))
  const min = Math.floor(remaining / 60)
  const sec = remaining % 60
  return `${min}:${String(sec).padStart(2, '0')}`
}

async function handleUndo(id) {
  await undoOperation(id)
}
</script>

<style scoped>
.undo-panel-wrapper {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
}

.undo-fab {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #C5A265;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(197, 162, 101, 0.4);
  transition: all 0.2s;
  position: relative;
}
.undo-fab:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(197, 162, 101, 0.5);
}
.undo-fab.expanded {
  background: #b08d4a;
}

.undo-fab-icon {
  font-size: 20px;
  font-weight: 700;
}

.undo-fab-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 20px;
  height: 20px;
  border-radius: 10px;
  background: #ff4d4f;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
}

.undo-panel {
  position: absolute;
  bottom: 56px;
  right: 0;
  width: 340px;
  max-height: 400px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.undo-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.undo-panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.undo-panel-close {
  background: none;
  border: 1px solid #e74c3c;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 700;
  color: #e74c3c;
  cursor: pointer;
  padding: 1px 8px;
  transition: all .15s;
}
.undo-panel-close:hover {
  background: #e74c3c;
  color: #fff;
}

.undo-panel-list {
  overflow-y: auto;
  max-height: 340px;
}

.undo-panel-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  border-bottom: 1px solid #f5f5f5;
  transition: background 0.15s;
}
.undo-panel-item:hover {
  background: #fafbfc;
}

.undo-item-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.undo-item-info {
  flex: 1;
  min-width: 0;
}

.undo-item-text {
  display: block;
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.undo-item-timer {
  font-size: 11px;
  color: #999;
  font-family: var(--font-mono, monospace);
}

.undo-item-btn {
  flex-shrink: 0;
  padding: 4px 12px;
  border: 1px solid #C5A265;
  border-radius: 4px;
  background: #fff;
  color: #C5A265;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.undo-item-btn:hover {
  background: #C5A265;
  color: #fff;
}

.undo-panel-empty {
  text-align: center;
  padding: 24px;
  color: #ccc;
  font-size: 13px;
}

/* 面板展开/收起动画 */
.undo-panel-enter-active,
.undo-panel-leave-active {
  transition: all 0.2s ease;
}
.undo-panel-enter-from,
.undo-panel-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
