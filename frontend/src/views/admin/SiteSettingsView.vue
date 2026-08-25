<template>
  <div class="page admin-page">
    <div class="page-header">
      <h2>站点设置</h2>
      <button class="btn-primary" @click="handleSave" :disabled="saving">{{ saving ? '保存中...' : '保存设置' }}</button>
    </div>

    <div class="card" v-if="cfg">
      <!-- Hero -->
      <section class="sec">
        <h3 class="sec-title">首页 Hero 横幅</h3>
        <div class="form-g"><label>标签文字</label><input v-model="cfg.hero.tagline" /></div>
        <div class="form-g"><label>主标题</label><input v-model="cfg.hero.heading" /></div>
        <div class="form-g"><label>描述</label><input v-model="cfg.hero.desc" /></div>
      </section>

      <!-- Stats -->
      <section class="sec">
        <h3 class="sec-title">信任数据（4 组）</h3>
        <div class="stats-row" v-for="(s, i) in cfg.stats" :key="i">
          <div class="form-g s-num"><label>数字</label><input v-model="s.num" /></div>
          <div class="form-g s-suffix"><label>后缀</label><input v-model="s.suffix" placeholder="如 + % 万" /></div>
          <div class="form-g s-label"><label>说明</label><input v-model="s.label" /></div>
        </div>
      </section>

      <!-- Series -->
      <section class="sec">
        <h3 class="sec-title">产品系列卡片（6 个）</h3>
        <div class="series-item" v-for="(s, i) in cfg.series" :key="i">
          <div class="form-g s-icon"><label>图标</label><input v-model="s.icon" placeholder="emoji" maxlength="4" /></div>
          <div class="form-g flex-1"><label>名称</label><input v-model="s.name" /></div>
          <div class="form-g flex-1"><label>描述</label><input v-model="s.desc" /></div>
        </div>
      </section>

      <!-- Advantages -->
      <section class="sec">
        <h3 class="sec-title">核心优势（6 项）</h3>
        <div class="adv-item" v-for="(a, i) in cfg.advantages" :key="i">
          <div class="form-g a-icon"><label>图标</label><input v-model="a.icon" placeholder="emoji" maxlength="4" /></div>
          <div class="form-g flex-1"><label>标题</label><input v-model="a.title" /></div>
          <div class="form-g flex-1"><label>描述</label><input v-model="a.desc" /></div>
        </div>
      </section>

      <!-- CTA -->
      <section class="sec">
        <h3 class="sec-title">CTA 行动号召</h3>
        <div class="form-g"><label>标题</label><input v-model="cfg.cta.heading" /></div>
        <div class="form-g"><label>描述</label><input v-model="cfg.cta.desc" /></div>
      </section>

      <!-- 服务流程 -->
      <section class="sec">
        <h3 class="sec-title">服务流程（5 步 + 3 质保）</h3>
        <h4 class="sub-title">流程步骤</h4>
        <div class="sv-step" v-for="(s, i) in cfg.service.steps" :key="i">
          <div class="form-g s-icon"><label>图标</label><input v-model="s.icon" placeholder="emoji" maxlength="4" /></div>
          <div class="form-g flex-1"><label>标题</label><input v-model="s.title" /></div>
          <div class="form-g flex-1"><label>描述</label><input v-model="s.desc" /></div>
          <div class="form-g flex-1"><label>详情（逗号分隔）</label><input v-model="s.detailStr" @change="s.details = s.detailStr.split(',').map(x=>x.trim()).filter(Boolean)" /></div>
        </div>
        <h4 class="sub-title">质保承诺</h4>
        <div class="sv-step" v-for="(w, i) in cfg.service.warranty" :key="'w'+i">
          <div class="form-g s-icon"><label>图标</label><input v-model="w.icon" placeholder="emoji" maxlength="4" /></div>
          <div class="form-g flex-1"><label>标题</label><input v-model="w.title" /></div>
          <div class="form-g flex-1"><label>内容（\\n 换行）</label><input v-model="w.desc" /></div>
        </div>
      </section>

      <!-- FAQ -->
      <section class="sec">
        <h3 class="sec-title">常见问题（FAQ）</h3>
        <div class="sv-step" v-for="(f, i) in cfg.faq" :key="'f'+i">
          <div class="form-g flex-1"><label>问题 {{ i + 1 }}</label><input v-model="f.q" /></div>
          <div class="form-g flex-1"><label>答案</label><input v-model="f.a" /></div>
        </div>
      </section>

      <!-- 客户评价 -->
      <section class="sec">
        <h3 class="sec-title">客户评价（3 条）</h3>
        <div class="sv-step" v-for="(r, i) in cfg.reviews" :key="'r'+i">
          <div class="form-g s-num"><label>星级</label><input v-model.number="r.stars" type="number" min="1" max="5" /></div>
          <div class="form-g flex-1"><label>评价内容</label><input v-model="r.text" /></div>
          <div class="form-g flex-1"><label>客户署名</label><input v-model="r.author" /></div>
          <div class="form-g flex-1"><label>购买产品</label><input v-model="r.product" /></div>
        </div>
      </section>

      <!-- 到店指引 -->
      <section class="sec">
        <h3 class="sec-title">到店指引（3 条）</h3>
        <div class="sv-step" v-for="(g, i) in cfg.guide" :key="'g'+i">
          <div class="form-g s-icon"><label>图标</label><input v-model="g.icon" placeholder="emoji" maxlength="4" /></div>
          <div class="form-g flex-1"><label>标题</label><input v-model="g.label" /></div>
          <div class="form-g flex-1"><label>说明</label><input v-model="g.text" /></div>
        </div>
      </section>

      <!-- 页脚设置 -->
      <section class="sec">
        <h3 class="sec-title">页脚设置</h3>
        <div class="form-g"><label>公司名称</label><input v-model="cfg.footer.companyName" placeholder="顺居门业" /></div>
        <div class="form-g"><label>ICP 备案号</label><input v-model="cfg.footer.icpNumber" placeholder="粤ICP备XXXXXXXX号" /></div>
        <div class="form-g"><label>公安备案号</label><input v-model="cfg.footer.psbNumber" placeholder="粤公网安备XXXXXXXXXX号（选填）" /></div>
        <div class="form-g"><label>自定义版权文字</label><input v-model="cfg.footer.copyright" placeholder="留空则自动生成 © 年份 公司名 All Rights Reserved" /></div>
      </section>

      <p class="hint">修改后点击右上角「保存设置」，刷新用户端即可看到变化。</p>
    </div>
    <p v-else-if="loadError" class="empty error-msg">{{ loadError }}</p>
    <p v-else class="empty">加载中...</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSiteConfig, updateSiteConfig } from '@/api/admin'

const cfg = ref(null)
const saving = ref(false)
const loadError = ref('')

// 新字段默认值（当数据库中的旧 JSON 缺少这些 key 时补齐）
const serviceDefault = {"steps":[{"icon":"📏","title":"免费上门量尺","desc":"专业师傅携带精密仪器上门，精确测量门窗洞口尺寸，了解需求。","details":["全屋门窗尺寸精准测量","了解采光/通风/隔音需求","提供初步风格搭配建议"]},{"icon":"✏️","title":"方案设计报价","desc":"根据测量数据和客户需求，出具设计方案和详细报价清单。","details":["推荐合适型材与玻璃配置","3D 效果图预览（免费）","透明报价，无隐形消费"]},{"icon":"🏭","title":"生产加工定制","desc":"工厂按图纸精准加工，严控每一道工序，确保品质达标。","details":["高精度数控设备切割组装","型材表面喷涂/电泳处理","出厂前全检，合格率 ≥ 99%"]},{"icon":"🔧","title":"专业安装施工","desc":"经验丰富的安装团队携带专业工具上门，规范安装施工。","details":["旧窗拆除 + 垃圾清运","水平仪校准 → 发泡胶填充 → 密封处理","完工后现场清扫干净"]},{"icon":"✅","title":"验收交付售后","desc":"客户现场验收签字确认，建立售后服务档案。","details":["开关/密封/锁具逐项验收","讲解日常使用保养须知","录入售后系统，享受质保服务"]}],"warranty":[{"icon":"🛡️","title":"质保期限","desc":"型材框架 10年 质保\\n五金配件 3年 质保\\n玻璃密封 2年 质保"},{"icon":"🔄","title":"终身维护","desc":"质保期内免费维修\\n超期仅收材料成本费\\n定期回访巡检服务"},{"icon":"📋","title":"售后服务","desc":"售后电话 48小时响应\\n紧急情况 24小时上门\\n维修完成后电话回访"}]}
const faqDefault = [{"q":"你们提供免费上门测量吗？","a":"提供。乳源全县范围内免费上门测量门窗洞口尺寸，并给出初步方案建议，不收取任何费用。"},{"q":"定做窗户一般需要多久？","a":"从测量到安装完成，常规产品约需 7-15 天，具体视产品类型和订单量而定，急单可加急处理。"},{"q":"门窗质保多久？","a":"型材框架质保 10 年，五金配件质保 3 年，玻璃密封质保 2 年。质保期内免费维修更换。"},{"q":"旧窗换新需要拆墙吗？","a":"大多数情况下不需要。我们的旧窗改造采用无损拆换技术，原墙体结构保留，仅更换窗框和玻璃。"},{"q":"可以只换玻璃不换窗框吗？","a":"可以。如果现有窗框结构完好，可单独更换中空玻璃或 Low-E 玻璃，提升隔音隔热效果。"},{"q":"断桥铝和普通铝合金有什么区别？","a":"断桥铝中间有隔热条断开，隔音隔热效果远优于普通铝合金，特别适合乳源山区夏季高温、冬季潮湿的气候。"}]
const reviewsDefault = [{"stars":5,"text":"师傅上门很准时，量尺也专业，装完效果比想象的好很多","author":"乳城镇 陈先生","product":"断桥铝平开窗","date":"2025.12"},{"stars":5,"text":"旧窗换新做得很好，拆旧装新一天搞定，垃圾都清理干净了","author":"桂头镇 张女士","product":"旧窗改造","date":"2025.10"},{"stars":5,"text":"对比了好几家，最后还是选了本地店，价格实在服务也好","author":"大桥镇 李先生","product":"不锈钢防盗门","date":"2025.08"}]
const guideDefault = [{"icon":"🚗","label":"驾车到店","text":"导航搜索「顺居门业」，G323 国道旁源强门厂西北侧 100 米，门前可停车"},{"icon":"🚌","label":"公交路线","text":"乘坐乳源公交 1 路/2 路至「鲜明桥」站，步行约 5 分钟即达"},{"icon":"🏪","label":"周边地标","text":"幸福家园二期对面 · 乳源体育馆往西 300 米"}]

onMounted(async () => {
  try {
    const r = await getSiteConfig()
    const raw = r.data.data
    cfg.value = typeof raw === 'string' ? JSON.parse(raw) : raw
    // 补齐旧 JSON 中缺失的新字段
    if (!cfg.value.service) cfg.value.service = JSON.parse(JSON.stringify(serviceDefault))
    if (!cfg.value.faq) cfg.value.faq = JSON.parse(JSON.stringify(faqDefault))
    if (!cfg.value.reviews) cfg.value.reviews = JSON.parse(JSON.stringify(reviewsDefault))
    if (!cfg.value.guide) cfg.value.guide = JSON.parse(JSON.stringify(guideDefault))
    if (!cfg.value.footer) cfg.value.footer = { companyName: '顺居门业', icpNumber: '', psbNumber: '', copyright: '' }
    // 初始化 detailStr
    if (cfg.value.service?.steps) {
      cfg.value.service.steps.forEach(s => {
        s.detailStr = s.detailStr || (s.details || []).join(', ')
      })
    }
  } catch (e) {
    loadError.value = e.message || '加载失败'
  }
})

async function handleSave() {
  try {
    await ElMessageBox.confirm('确定保存站点设置？修改后用户端首页立即生效。', '确认', { type: 'info' })
  } catch { return }
  saving.value = true
  try {
    await updateSiteConfig(JSON.stringify(cfg.value))
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.card { padding: 32px; }
.flex-1 { flex: 1; }
.sub-title { font-size: 13px; font-weight: 500; color: #888; margin: 16px 0 8px; }
.sv-step { display: flex; gap: 8px; align-items: flex-end; margin-bottom: 8px; padding: 8px; background: #fafbfc; border-radius: var(--radius); }
.biz-row { display: flex; gap: 8px; align-items: flex-end; margin-bottom: 6px; }
.stats-row { display: flex; gap: 12px; align-items: flex-end; }
.s-num { width: 100px; } .s-suffix { width: 80px; } .s-label { flex: 1; }
.series-item, .adv-item { display: flex; gap: 12px; align-items: flex-end; margin-bottom: 12px; padding: 12px; background: #fafbfc; border-radius: var(--radius); }
.s-icon input, .a-icon input { font-size: 20px; text-align: center; }
.s-icon { width: 64px; } .a-icon { width: 64px; } .s-num { width: 64px; }
.hint { font-size: 13px; color: var(--text-placeholder); margin-top: 24px; text-align: center; }
.error-msg { color: #e74c3c; }
</style>
