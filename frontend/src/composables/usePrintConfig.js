import { ref, onMounted } from 'vue'
import { getPrintSetting } from '@/api/admin'

// Per-type cache
const cache = {}

function getOrCreateCache(type) {
  if (!cache[type]) {
    cache[type] = { config: ref(null), loaded: false, loading: false, waiters: [] }
  }
  return cache[type]
}

export function invalidatePrintConfig(type = 'order') {
  const entry = cache[type]
  if (entry) {
    entry.loaded = false
    entry.config.value = null
  }
}

export function usePrintConfig(type = 'order') {
  const entry = getOrCreateCache(type)
  const printConfig = entry.config

  const DEFAULT_CONFIG = {
    fontSize: 11,
    lineWidths: { header: 2, info: 2, table: 2, summary: 2, terms: 2, footer: 2 },
    marginTop: 10, marginBottom: 10, marginLeft: 8, marginRight: 8,
    orientation: 'portrait',
    sectionStyles: {
      header: { bold: false, sizeOffset: 0 },
      info: { bold: false, sizeOffset: 0, align: 'left' },
      tableHead: { bold: true, sizeOffset: 0 },
      tableBody: { bold: false, sizeOffset: 0 },
      summary: { bold: false, sizeOffset: 0 },
      terms: { bold: false, sizeOffset: 0 },
      footer: { bold: false, sizeOffset: 0 },
    },
  }

  async function load() {
    if (entry.loaded) return
    if (entry.loading) {
      await new Promise(r => entry.waiters.push(r))
      return
    }
    entry.loading = true
    try {
      const res = await getPrintSetting(type)
      const data = res?.data?.data
      if (data) {
        let parsed = data
        if (typeof data === 'string') {
          try {
            parsed = JSON.parse(data)
          } catch {
            // 兼容历史双重转义 [{\"name\":...}] → [{"name":...}]
            try { parsed = JSON.parse(data.replace(/\\"/g, '"')) } catch { parsed = null }
          }
        }
        if (parsed) printConfig.value = parsed
      }
      // 加载失败/为空时兜底默认结构，避免依赖方拿到 null
      if (!printConfig.value) printConfig.value = JSON.parse(JSON.stringify(DEFAULT_CONFIG))
      entry.loaded = true
    } catch { /* ignore */ } finally {
      entry.loading = false
      entry.waiters.forEach(r => r())
      entry.waiters = []
    }
  }

  onMounted(load)

  return { printConfig, reloadPrintConfig: () => { entry.loaded = false; load() } }
}
