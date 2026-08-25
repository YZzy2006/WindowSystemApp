<template>
  <Transition name="fade">
    <button v-show="visible" class="back-to-top" @click="scrollToTop" title="回到顶部">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="18 15 12 9 6 15" />
      </svg>
    </button>
  </Transition>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  threshold: { type: Number, default: 300 },
})

const visible = ref(false)

function onScroll() {
  visible.value = window.scrollY > props.threshold
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
.back-to-top {
  position: fixed;
  bottom: 32px;
  right: 32px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #C5A265;
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 12px rgba(197, 162, 101, 0.4);
  transition: all 0.2s;
  z-index: 100;
  padding: 0;
}
.back-to-top:hover {
  background: #d4b885;
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(197, 162, 101, 0.5);
}
.back-to-top svg {
  width: 20px;
  height: 20px;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
