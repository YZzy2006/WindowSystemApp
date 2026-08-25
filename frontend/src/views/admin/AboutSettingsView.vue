<template>
  <div class="page admin-page">
    <div class="page-header">
      <h2>关于我们</h2>
      <button class="btn-primary" @click="handleSave" :disabled="saving">{{ saving ? '保存中...' : '保存设置' }}</button>
    </div>

    <div class="card" v-if="cfg">
      <!-- ===== Banner ===== -->
      <section class="sec">
        <h3 class="sec-title">Banner 横幅</h3>
        <div class="form-row">
          <div class="form-g flex-1"><label>主文案</label><input v-model="cfg.bannerDesc" placeholder="扎根乳源二十载，守护万家窗" /></div>
          <div class="form-g flex-1"><label>副文案</label><input v-model="cfg.bannerTagline" placeholder="2006-2026 | 本土门窗定制专家" /></div>
        </div>
      </section>

      <!-- ===== 里程碑 ===== -->
      <section class="sec">
        <h3 class="sec-title">里程碑铭牌</h3>
        <div class="form-row">
          <div class="form-g s-num"><label>大字</label><input v-model="cfg.msNum" placeholder="20+" /></div>
          <div class="form-g flex-1"><label>说明</label><input v-model="cfg.msLabel" placeholder="年专注门窗定制" /></div>
        </div>
      </section>

      <!-- ===== 三段式故事 ===== -->
      <section class="sec">
        <h3 class="sec-title">品牌故事（三段式）</h3>
        <div class="story-block" v-for="(st, i) in cfg.stories" :key="i">
          <h4 class="sub-title">第{{ i + 1 }}段</h4>
          <div class="form-row">
            <div class="form-g flex-1"><label>标题</label><input v-model="st.title" /></div>
          </div>
          <div class="form-g"><label>内容</label><textarea v-model="st.text" rows="3"></textarea></div>
          <div class="form-g"><label>配图（可多张，左右滑动）</label><MultiImageUploader v-model="st.images" /></div>
          <div class="form-g"><label>配图描述（显示在图片下方）</label><input v-model="st.imgDesc" placeholder="如：门店实景拍摄，专业安装团队现场作业" /></div>
        </div>
      </section>

      <!-- ===== 核心理念 ===== -->
      <section class="sec">
        <h3 class="sec-title">核心理念（3 张卡片）</h3>
        <div class="sv-step" v-for="(v, i) in cfg.values" :key="'v'+i">
          <span class="step-num">{{ i + 1 }}</span>
          <div class="form-g flex-1"><label>标题</label><input v-model="v.title" /></div>
          <div class="form-g flex-1"><label>描述（\\n 换行）</label><input v-model="v.desc" /></div>
        </div>
      </section>

      <!-- ===== 经营项目 ===== -->
      <section class="sec">
        <h3 class="sec-title">经营项目（6 项）</h3>
        <div class="biz-grid" v-for="(b, i) in cfg.bizItems" :key="'b'+i" style="display:flex;gap:8px;align-items:center;margin-bottom:8px;padding:8px 12px;background:#fafbfc;border-radius:8px">
          <span class="step-num" style="margin:0">{{ i + 1 }}</span>
          <div class="form-g flex-1" style="margin:0"><input v-model="b.text" placeholder="名称" /></div>
          <div class="form-g flex-1" style="margin:0"><input v-model="b.desc" placeholder="小字描述" /></div>
        </div>
      </section>

      <!-- ===== 客户证言 ===== -->
      <section class="sec">
        <h3 class="sec-title">客户证言（3 条）</h3>
        <div class="sv-step" v-for="(t, i) in cfg.testimonials" :key="'t'+i">
          <span class="step-num">{{ i + 1 }}</span>
          <div class="form-g flex-1"><label>姓名</label><input v-model="t.name" /></div>
          <div class="form-g flex-1"><label>小区/楼盘</label><input v-model="t.location" /></div>
          <div class="form-g flex-1"><label>评价内容</label><input v-model="t.text" /></div>
        </div>
      </section>

      <!-- ===== 联系方式 ===== -->
      <section class="sec">
        <h3 class="sec-title">联系方式</h3>
        <div class="form-row">
          <div class="form-g flex-1"><label>电话</label><input v-model="cfg.contact.phone" /></div>
          <div class="form-g flex-1"><label>营业时间</label><input v-model="cfg.contact.hours" /></div>
        </div>
        <div class="form-g"><label>地址</label><input v-model="cfg.contact.address" /></div>
        <div class="form-g"><label>地图链接（高德/百度）</label><input v-model="cfg.contact.mapUrl" placeholder="https://uri.amap.com/marker?position=..." /></div>
      </section>

      <p class="hint">修改后点击右上角「保存设置」，用户端"关于我们"页面立即更新。</p>
    </div>
    <p v-else class="empty">加载中...</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSiteConfig, updateSiteConfig } from '@/api/admin'
import MultiImageUploader from '@/components/MultiImageUploader.vue'

const cfg = ref(null)
const saving = ref(false)

const defAbout = {
  bannerDesc: '扎根乳源二十载，守护万家窗',
  bannerTagline: '2006-2026 | 本土门窗定制专家',
  msNum: '20+', msLabel: '年专注门窗定制',
  stories: [
    { title: '扎根本地 · 值得信赖', text: '顺居门业深耕乳源门窗市场二十载，从最初一间小作坊发展成为本地门窗定制领域的标杆企业。我们了解乳源山区的气候特点——夏季高温、冬季湿冷、台风频繁——每一款产品都针对本地环境做了优化设计，真正做到了"为乳源家庭而生"。', images: '', imgDesc: '顺居门业门店实景 · 乳城镇鲜明北路' },
    { title: '匠心工艺 · 专业团队', text: '我们拥有从业十年以上的安装师傅团队，每一位师傅都经过严格的技能培训和实操考核。从量尺、设计到安装、调试，每个环节都有标准化流程把控。我们承诺：不外包、不转包，全部由自有团队施工，确保每一扇窗都装得严丝合缝。', images: '', imgDesc: '专业安装团队现场作业 · 自有师傅不外包' },
    { title: '郑重承诺 · 终身维护', text: '型材框架质保十年，五金配件质保五年，玻璃密封质保三年——这些不是营销话术，而是白纸黑字写进合同的服务承诺。质保期内出现任何非人为质量问题，免费上门维修更换。超过质保期，仅收取材料成本费，不收上门费。', images: '', imgDesc: '品质保障 · 每扇窗出厂前全检合格' },
  ],
  values: [
    { title: '本地品牌', desc: '二十载扎根乳源\\n服务千家万户' },
    { title: '匠心工艺', desc: '精选优质型材\\n严苛品控标准' },
    { title: '诚信服务', desc: '免费上门测量\\n终身售后维护' },
  ],
  bizItems: [
    { text: '断桥隔热门窗', desc: '隔音隔热 · 系统窗定制' },
    { text: '不锈钢防盗门窗', desc: '甲级防盗 · 安全可靠' },
    { text: '室内定制木门', desc: '实木工艺 · 个性设计' },
    { text: '旧窗改造翻新', desc: '无损拆换 · 一天完工' },
    { text: '阳光房定制', desc: '全景落地 · 采光通透' },
    { text: '纱窗护栏配件', desc: '304不锈钢 · 防蚊防盗' },
  ],
  testimonials: [
    { name: '陈先生', location: '碧桂园·天汇', text: '对比了三家门窗店，最后选了顺居。师傅上门量尺很细心，安装也专业。装完整个家都亮堂了，非常满意！' },
    { name: '李女士', location: '乳源华景雅苑', text: '老房子旧窗换新，怕拆墙太麻烦。结果师傅用无损拆换技术两天就搞定了，一点墙都没坏，效果超出预期。' },
    { name: '张先生', location: '桂头镇自建房', text: '新建自建房整栋楼的窗户都是顺居做的，用料实在，价格透明。老板人实在，装了三年了没出过任何问题。' },
  ],
  contact: { phone: '189-4884-6839', hours: '周一至周日 8:00 - 18:00', address: '广东省韶关市乳源瑶族自治县乳城镇鲜明北路大群奔康住宅楼4号店铺', mapUrl: '' },
}

onMounted(async () => {
  try {
    const r = await getSiteConfig()
    const raw = r.data.data
    const full = typeof raw === 'string' ? JSON.parse(raw) : raw
    // Merge existing about with defaults for any missing fields
    const existing = full.about || {}
    cfg.value = { ...defAbout, ...existing }
    // Deep merge: use existing if it has data, otherwise fall back to defaults
    const mergeArr = (existingArr, defaultArr) => {
      const arr = (existingArr && existingArr.length ? existingArr : defaultArr)
      return arr.map((item, i) => ({ ...(defaultArr[i] || {}), ...item }))
    }
    cfg.value.stories = mergeArr(existing.stories, defAbout.stories)
    cfg.value.values = mergeArr(existing.values, defAbout.values)
    cfg.value.bizItems = mergeArr(existing.bizItems, defAbout.bizItems)
    cfg.value.testimonials = mergeArr(existing.testimonials, defAbout.testimonials)
    cfg.value.contact = { ...defAbout.contact, ...(existing.contact || {}) }
    // Store the full config for saving
    cfg.value._fullConfig = full
  } catch { cfg.value = { ...defAbout } }
})

async function handleSave() {
  try {
    await ElMessageBox.confirm('确定保存"关于我们"设置？用户端将立即更新。', '确认', { type: 'info' })
  } catch { return }
  saving.value = true
  try {
    const aboutData = { ...cfg.value }
    delete aboutData._fullConfig
    const full = cfg.value._fullConfig || {}
    full.about = aboutData
    await updateSiteConfig(JSON.stringify(full))
    ElMessage.success('保存成功')
  } catch (e) { if (!e._handled) ElMessage.error(e.message || '保存失败') }
  finally { saving.value = false }
}
</script>

<style scoped>
.card { padding: 28px 32px; max-width: 860px; }
.flex-1 { flex: 1; }
.s-num { width: 100px; }
.sub-title { font-size: 13px; font-weight: 600; color: #666; margin: 12px 0 8px; }
.sv-step { display: flex; gap: 8px; align-items: flex-end; margin-bottom: 8px; padding: 10px; background: #fafbfc; border-radius: var(--radius); }
.story-block { background: #fafbfc; border-radius: 10px; padding: 16px 18px; margin-bottom: 14px; }
.hint { font-size: 13px; color: var(--text-placeholder); margin-top: 20px; text-align: center; }
</style>
