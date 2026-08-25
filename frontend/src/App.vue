<template>
  <el-config-provider :locale="zhCn">
    <div id="route-progress-bar" class="route-progress-bar"></div>
    <router-view v-slot="{ Component }">
      <transition name="page" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </el-config-provider>
</template>

<script setup>
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
</script>

<style>
.page-enter-active { transition: opacity .3s ease, transform .3s cubic-bezier(.4,0,.2,1); }
.page-leave-active { transition: opacity .15s ease; position: absolute; }
.page-enter-from { opacity: 0; transform: translateY(16px) scale(.99); }
.page-leave-to { opacity: 0; }

.reveal {
  opacity: 0; transform: translateY(36px);
  transition: opacity .7s cubic-bezier(.4,0,.2,1), transform .7s cubic-bezier(.4,0,.2,1);
}
.reveal.revealed { opacity: 1; transform: translateY(0); }
.reveal:nth-child(2) { transition-delay: .1s; }
.reveal:nth-child(3) { transition-delay: .2s; }
.reveal:nth-child(4) { transition-delay: .3s; }
.reveal:nth-child(5) { transition-delay: .4s; }
.reveal:nth-child(6) { transition-delay: .5s; }

/* 悬浮漂浮动画 */
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}
.float-anim { animation: float 3s ease-in-out infinite; }

@keyframes shimmer {
  0% { background-position: -400px 0; }
  100% { background-position: 400px 0; }
}
.skeleton {
  background: linear-gradient(90deg, #f0eeeb 25%, #e8e5e0 50%, #f0eeeb 75%);
  background-size: 800px 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 8px;
}

/* 脉冲指示 */
@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(196,154,44,.4); }
  50% { box-shadow: 0 0 0 8px rgba(196,154,44,0); }
}
.pulse { animation: pulse 2s infinite; }

/* 路由进度条 */
.route-progress-bar {
  display: none; position: fixed; top: 0; left: 0; z-index: 99999;
  height: 3px; width: 0;
  background: linear-gradient(90deg, #C5A265, #e0c882);
  transition: width .3s ease;
  box-shadow: 0 0 8px rgba(197,162,101,.5);
}
</style>
