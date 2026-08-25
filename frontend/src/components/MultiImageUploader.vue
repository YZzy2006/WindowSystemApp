<template>
  <div class="multi-uploader">
    <div class="image-list">
      <div v-for="(url, i) in urls" :key="i" class="image-item">
        <img :src="url" />
        <button class="remove-btn" @click="remove(i)">✕</button>
      </div>
      <div class="upload-btn" @click="triggerUpload" v-if="urls.length < 6">
        <input ref="fileInput" type="file" accept="image/*" @change="handleChange" hidden />
        <span class="upload-plus">+</span>
        <span class="upload-text">上传图片</span>
      </div>
    </div>
    <p class="hint" v-if="urls.length > 0">已上传 {{ urls.length }} 张，点击图片可移除</p>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { upload } from '@/api/admin'

const props = defineProps({
  modelValue: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue'])
const fileInput = ref(null)
const uploading = ref(false)

const urls = computed({
  get: () => props.modelValue ? props.modelValue.split(',').map(s => s.trim()).filter(Boolean) : [],
  set: (val) => emit('update:modelValue', val.join(','))
})

function triggerUpload() { fileInput.value?.click() }

async function handleChange(e) {
  const file = e.target.files[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('只能上传图片文件')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    return
  }
  uploading.value = true
  try {
    const res = await upload(file)
    const url = res.data.data
    urls.value = [...urls.value, url]
  } catch {
    ElMessage.error('图片上传失败')
  } finally {
    uploading.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}

function remove(i) {
  const arr = [...urls.value]
  arr.splice(i, 1)
  urls.value = arr
}
</script>

<style scoped>
.image-list { display: flex; gap: 10px; flex-wrap: wrap; }
.image-item {
  width: 96px; height: 96px; border-radius: 8px; overflow: hidden;
  position: relative; border: 1px solid #eae8e5;
}
.image-item img { width: 100%; height: 100%; object-fit: cover; }
.remove-btn {
  position: absolute; top: 4px; right: 4px; width: 22px; height: 22px;
  border-radius: 50%; background: rgba(0,0,0,.55); color: #fff; border: none;
  font-size: 11px; cursor: pointer; display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: opacity .2s;
}
.image-item:hover .remove-btn { opacity: 1; }
.upload-btn {
  width: 96px; height: 96px; border: 2px dashed #ddd; border-radius: 8px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  cursor: pointer; transition: all .2s; color: #bbb; gap: 4px;
}
.upload-btn:hover { border-color: #d4af37; color: #d4af37; }
.upload-plus { font-size: 24px; line-height: 1; }
.upload-text { font-size: 10px; }
.hint { font-size: 11px; color: #bbb; margin-top: 8px; }
</style>
