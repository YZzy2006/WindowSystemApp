<template>
  <div class="spec-editor">
    <div v-for="group in groups" :key="group.name" class="spec-group">
      <div class="group-header">
        <span class="group-icon">{{ group.icon }}</span>
        <h4>{{ group.label }}</h4>
        <button type="button" class="add-row-btn" @click="addRow(group)">＋ 添加</button>
      </div>
      <div class="group-rows">
        <div v-for="(item, i) in group.items" :key="i" class="spec-row">
          <input :value="item.key" @input="updateKey(group, i, $event.target.value)" placeholder="属性名" class="spec-key" />
          <input :value="item.value" @input="updateVal(group, i, $event.target.value)" placeholder="属性值" class="spec-val" />
          <button type="button" class="del-row-btn" @click="delRow(group, i)">✕</button>
        </div>
      </div>
    </div>
    <p class="hint" v-if="totalCount === 0">暂无规格参数，点击"添加"按钮录入</p>
  </div>
</template>

<script setup>
import { reactive, computed, watch } from 'vue'

const props = defineProps({ modelValue: { type: String, default: '{}' } })
const emit = defineEmits(['update:modelValue'])

const groupDefs = [
  { name: '型材', label: '🏗️ 型材参数', icon: '🏗️', prefix: '型材:' },
  { name: '玻璃', label: '🪞 玻璃配置', icon: '🪞', prefix: '玻璃:' },
  { name: '五金', label: '🔧 五金配件', icon: '🔧', prefix: '五金:' },
  { name: '花色', label: '🎨 颜色款式', icon: '🎨', prefix: '花色:' },
  { name: '工艺', label: '📐 工艺细节', icon: '📐', prefix: '工艺:' },
  { name: '服务', label: '🛡️ 服务保障', icon: '🛡️', prefix: '服务:' },
]

const groups = reactive(groupDefs.map(g => ({ ...g, items: [] })))

const totalCount = computed(() => groups.reduce((s, g) => s + g.items.length, 0))

function parseSpecs(json) {
  let obj = {}
  try { obj = JSON.parse(json || '{}') } catch {}
  if (typeof obj !== 'object') obj = {}
  for (const g of groups) g.items = []
  for (const [k, v] of Object.entries(obj)) {
    const group = groups.find(g => k.startsWith(g.prefix))
    if (group) {
      group.items.push({ key: k.replace(group.prefix, ''), value: String(v) })
    } else {
      // 未归类放第一个组
      groups[0].items.push({ key: k, value: String(v) })
    }
  }
}

function toJson() {
  const obj = {}
  for (const g of groups) {
    for (const item of g.items) {
      if (item.key.trim()) {
        obj[g.prefix + item.key.trim()] = item.value
      }
    }
  }
  return JSON.stringify(obj)
}

function emitUpdate() { emit('update:modelValue', toJson()) }

function addRow(group) {
  group.items.push({ key: '', value: '' })
}

function updateKey(group, i, val) {
  group.items[i].key = val
  emitUpdate()
}

function updateVal(group, i, val) {
  group.items[i].value = val
  emitUpdate()
}

function delRow(group, i) {
  group.items.splice(i, 1)
  emitUpdate()
}

// 初始解析
parseSpecs(props.modelValue)

// 外部更新时重新解析
watch(() => props.modelValue, (val) => parseSpecs(val))
</script>

<style scoped>
.spec-editor { max-height: 360px; overflow-y: auto; }

.spec-group { margin-bottom: 16px; background: #f9f9f9; border-radius: 8px; padding: 14px; }
.group-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.group-header h4 { font-size: 14px; font-weight: 600; color: #2c2c2c; flex: 1; }
.group-icon { font-size: 14px; }

.add-row-btn {
  font-size: 11px; padding: 3px 10px; background: #fff; color: #b8942e;
  border: 1px solid #d4af37; border-radius: 4px; cursor: pointer; transition: all .2s;
}
.add-row-btn:hover { background: #d4af37; color: #fff; }

.group-rows { display: flex; flex-direction: column; gap: 6px; }
.spec-row { display: flex; gap: 6px; align-items: center; }
.spec-key {
  width: 110px; padding: 7px 10px; border: 1px solid #e0e0e0; border-radius: 6px;
  font-size: 12px; outline: none; background: #fff; flex-shrink: 0;
}
.spec-key:focus { border-color: #d4af37; }
.spec-val {
  flex: 1; padding: 7px 10px; border: 1px solid #e0e0e0; border-radius: 6px;
  font-size: 12px; outline: none; background: #fff;
}
.spec-val:focus { border-color: #d4af37; }
.del-row-btn {
  width: 26px; height: 26px; border-radius: 50%; border: none; background: transparent;
  color: #ccc; cursor: pointer; font-size: 13px; flex-shrink: 0; transition: all .2s;
}
.del-row-btn:hover { background: #fff1f0; color: #e74c3c; }

.hint { font-size: 12px; color: #bbb; text-align: center; padding: 20px 0; }
</style>
