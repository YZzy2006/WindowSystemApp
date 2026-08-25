<template>
  <div class="skeleton-wrap" v-if="loading">
    <div class="skeleton-row" v-for="i in rows" :key="i">
      <div class="skeleton-cell" v-for="j in columns" :key="j" :style="{ width: colWidths ? colWidths[j - 1] + 'px' : undefined, flex: colWidths ? 'none' : 1 }">
        <div class="skeleton-bar" :style="{ width: barWidths ? barWidths[j - 1] : randomWidth(i, j) }"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  loading: { type: Boolean, default: true },
  rows: { type: Number, default: 8 },
  columns: { type: Number, default: 6 },
  colWidths: { type: Array, default: null },
  barWidths: { type: Array, default: null },
})

const widths = ['60%', '75%', '50%', '85%', '45%', '70%', '65%', '55%', '80%', '40%']
function randomWidth(row, col) {
  return widths[(row * 3 + col * 7) % widths.length]
}
</script>

<style scoped>
.skeleton-wrap {
  padding: 0;
}
.skeleton-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  border-bottom: 1px solid #f5f5f5;
}
.skeleton-cell {
  flex: 1;
}
.skeleton-bar {
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
