<template>
  <div class="nf-page">
    <div class="nf-card">
      <span class="nf-num">404</span>
      <h1>页面未找到</h1>
      <p>您访问的页面不存在或已被移除</p>
      <div class="nf-btns">
        <router-link to="/" class="btn-gold">返回首页</router-link>
        <router-link to="/products" class="btn-outline">浏览产品</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.nf-page {
  min-height: 70vh; display: flex; align-items: center; justify-content: center; padding: 40px 20px;
}
.nf-card { text-align: center; max-width: 440px; }
.nf-num {
  display: block; font-size: 120px; font-weight: 200; letter-spacing: 12px;
  background: linear-gradient(135deg, #d4af37, #b8942e);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
  line-height: 1;
}
.nf-card h1 { font-size: 22px; font-weight: 600; color: #2c2c2c; margin: 16px 0 8px; }
.nf-card p { font-size: 14px; color: #999; margin-bottom: 32px; }
.nf-btns { display: flex; gap: 12px; justify-content: center; flex-wrap: wrap; }
.btn-gold {
  display: inline-block; padding: 13px 32px;
  background: linear-gradient(135deg, #d4af37, #b8942e); color: #fff;
  border-radius: 6px; font-size: 14px; font-weight: 600; letter-spacing: 1px;
}
.btn-gold:hover { opacity: .9; }
.btn-outline {
  display: inline-block; padding: 13px 32px;
  border: 1px solid #d4af37; color: #b8942e; border-radius: 6px;
  font-size: 14px; font-weight: 500;
}
.btn-outline:hover { background: #faf6ee; }

@media (max-width: 768px) {
  .nf-num { font-size: 80px; }
}
</style>
