<template>
  <div class="page admin-page">
    <div class="page-header">
      <h2>联系我们</h2>
      <button class="btn-primary" @click="handleSave" :disabled="saving">{{ saving ? '保存中...' : '保存设置' }}</button>
    </div>

    <div class="card" v-if="cfg">
      <section class="sec">
        <h3 class="sec-title">联系方式</h3>
        <div class="form-row">
          <div class="form-g flex-1"><label>电话</label><input v-model="cfg.phone" placeholder="138-0000-0000" /></div>
          <div class="form-g flex-1"><label>微信号</label><input v-model="cfg.wechat" placeholder="example-window" /></div>
        </div>
        <div class="form-row">
          <div class="form-g flex-1"><label>营业时间</label><input v-model="cfg.hours" placeholder="周一至周日 8:00 - 18:00" /></div>
          <div class="form-g flex-1"><label>联系邮箱</label><input v-model="cfg.email" placeholder="（选填）" /></div>
        </div>
        <div class="form-g"><label>门店地址</label><input v-model="cfg.address" placeholder="示例省示例市示例区示例街道..." /></div>
        <div class="form-g"><label>地图链接（高德/百度）</label><input v-model="cfg.mapUrl" placeholder="https://uri.amap.com/marker?position=..." /></div>
      </section>

      <section class="sec">
        <h3 class="sec-title">服务区域</h3>
        <div class="form-g">
          <label>区域列表（逗号分隔）</label>
          <input v-model="cfg.areasStr" placeholder="示例街道,示例镇A,示例镇B..." @change="cfg.areas = cfg.areasStr.split(',').map(s => s.trim()).filter(Boolean)" />
        </div>
        <div class="form-g">
          <label>区域描述</label>
          <input v-model="cfg.areaDesc" placeholder="如：我们为示例全市及周边地区提供门窗定制安装服务" />
        </div>
      </section>

      <p class="hint">修改后点击右上角「保存设置」，用户端"联系我们"页面立即更新。</p>
    </div>
    <p v-else class="empty">加载中...</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSiteConfig, updateSiteConfig } from '@/api/admin'

const cfg = ref(null)
const saving = ref(false)

const defContact = {
  phone: '138-0000-0000',
  wechat: 'example-window',
  hours: '周一至周日 8:00 - 18:00',
  email: '',
  address: '示例省示例市示例区示例街道示例路1号',
  mapUrl: '',
  areasStr: '示例街道,示例镇A,示例镇B,示例镇C,示例镇D,示例镇E,示例镇F,示例镇G',
  areas: ['示例街道','示例镇A','示例镇B','示例镇C','示例镇D','示例镇E','示例镇F','示例镇G'],
  areaDesc: '我们为示例全市及周边地区提供门窗定制安装服务',
}

onMounted(async () => {
  try {
    const r = await getSiteConfig()
    const raw = r.data.data
    const full = typeof raw === 'string' ? JSON.parse(raw) : raw
    const contact = (full.about && full.about.contact) ? full.about.contact : {}
    cfg.value = { ...defContact, ...contact }
    if (!cfg.value.areasStr && cfg.value.areas) {
      cfg.value.areasStr = cfg.value.areas.join(', ')
    }
    cfg.value._fullConfig = full
  } catch { cfg.value = { ...defContact } }
})

async function handleSave() {
  try {
    await ElMessageBox.confirm('确定保存"联系我们"设置？用户端将立即更新。', '保存确认', { type: 'warning' })
  } catch {
    return
  }
  saving.value = true
  try {
    const contact = { ...cfg.value }
    delete contact._fullConfig; delete contact.areasStr
    const full = cfg.value._fullConfig || {}
    if (!full.about) full.about = {}
    full.about.contact = contact
    await updateSiteConfig(JSON.stringify(full))
    ElMessage.success('保存成功')
  } catch (e) { if (!e._handled) ElMessage.error(e.message || '保存失败') }
  finally { saving.value = false }
}
</script>

<style scoped>
.card { padding: 28px 32px; max-width: 860px; }
.flex-1 { flex: 1; }
.hint { font-size: 13px; color: var(--text-placeholder); margin-top: 20px; text-align: center; }
</style>
