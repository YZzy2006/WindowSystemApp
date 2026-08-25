<template>
  <button class="btn-refresh" :class="{ spinning }" @click="handleRefresh" :title="title || '刷新数据'">
    <svg class="refresh-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <polyline points="23 4 23 10 17 10" />
      <polyline points="1 20 1 14 7 14" />
      <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
    </svg>
  </button>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  onRefresh: { type: Function, required: true },
  title: { type: String, default: '刷新数据' },
})

const spinning = ref(false)

async function handleRefresh() {
  if (spinning.value) return
  spinning.value = true
  try {
    await props.onRefresh()
  } finally {
    spinning.value = false
  }
}
</script>

<style scoped>
.btn-refresh {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background: #fff;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  padding: 0;
}
.btn-refresh:hover {
  border-color: #C5A265;
  color: #C5A265;
}
.refresh-icon {
  width: 16px;
  height: 16px;
}
.spinning .refresh-icon {
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
