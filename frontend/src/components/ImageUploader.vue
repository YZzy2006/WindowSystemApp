<template>
  <div class="image-uploader">
    <div class="upload-box" @click="triggerUpload">
      <input ref="fileInput" type="file" accept="image/*" @change="handleChange" hidden />
      <template v-if="uploading">
        <span class="upload-text">上传中...</span>
      </template>
      <template v-else-if="modelValue">
        <img :src="modelValue" alt="preview" />
        <div class="upload-mask">
          <span>点击更换</span>
        </div>
      </template>
      <template v-else>
        <span class="upload-plus">+</span>
        <span class="upload-text">上传图片</span>
      </template>
    </div>
    <p v-if="modelValue" class="upload-url">{{ modelValue }}</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { upload } from '@/api/admin'

const props = defineProps({
  modelValue: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue'])
const fileInput = ref(null)
const uploading = ref(false)

function triggerUpload() {
  fileInput.value?.click()
}

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
    emit('update:modelValue', url)
  } catch {
    ElMessage.error('图片上传失败')
  } finally {
    uploading.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}
</script>

<style scoped>
.upload-box {
  width: 128px;
  height: 128px;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color .2s;
}
.upload-box:hover {
  border-color: #1677ff;
}
.upload-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.upload-mask {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,.45);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity .2s;
}
.upload-box:hover .upload-mask {
  opacity: 1;
}
.upload-plus {
  font-size: 28px;
  color: #999;
}
.upload-text {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
.upload-url {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
  word-break: break-all;
}
</style>
