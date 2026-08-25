/**
 * File download utility — saves to Desktop via Java backend
 */
import { ElMessage as DefaultMessage } from 'element-plus'

/**
 * Save a file to disk via Java backend (saves to user's Desktop)
 * @param {Blob|ArrayBuffer|Uint8Array} data - File data
 * @param {string} filename - Suggested filename
 * @param {object} options
 * @param {string} [options.successMsg='保存成功'] - Success message text
 * @param {Function} [options.ElMessage] - Element Plus ElMessage instance (defaults to imported)
 * @returns {Promise<boolean>}
 */
export async function saveFile(data, filename, options = {}) {
  const { successMsg = '保存成功', ElMessage = DefaultMessage } = options
  const blob = data instanceof Blob ? data : new Blob([data])

  const doFetch = async (token) => {
    const headers = { 'Content-Type': 'application/json' }
    if (token) headers['Authorization'] = 'Bearer ' + token

    const base64 = await blobToBase64(blob)
    const body = JSON.stringify({ data: base64, filename })

    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 120000)

    try {
      const res = await fetch('/api/admin/save-file', {
        method: 'POST',
        headers,
        body,
        signal: controller.signal
      })
      clearTimeout(timeoutId)
      return await res.json()
    } catch (e) {
      clearTimeout(timeoutId)
      if (e.name === 'AbortError') throw new Error('保存超时，文件可能过大')
      throw e
    }
  }

  try {
    const token = sessionStorage.getItem('token')
    let result = await doFetch(token)

    // 401: try token refresh then retry once
    if (result.code === 401) {
      try {
        const refreshRes = await fetch('/api/admin/refresh-token', {
          method: 'POST',
          headers: { 'Authorization': 'Bearer ' + token }
        })
        const refreshData = await refreshRes.json()
        if (refreshData.code === 200 && refreshData.data) {
          sessionStorage.setItem('token', refreshData.data)
          result = await doFetch(refreshData.data)
        }
      } catch {}
    }

    if (result.code === 200) {
      ElMessage.success({ message: `${successMsg}<br/>保存位置: ${result.data}`, dangerouslyUseHTMLString: true })
      return true
    } else {
      ElMessage.error(result.msg || '保存失败')
      return false
    }
  } catch (e) {
    console.error('Save error:', e)
    ElMessage.error('保存失败: ' + (e.message || e))
    return false
  }
}

function blobToBase64(blob) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result.split(',')[1])
    reader.onerror = () => reject(new Error('读取文件失败'))
    reader.readAsDataURL(blob)
  })
}

/**
 * Download a file from a server response (Blob)
 */
export async function downloadBlob(blob, filename, options = {}) {
  return saveFile(blob, filename, { successMsg: '下载成功', ...options })
}

/**
 * Save a data URL (e.g., from toPng/canvas) to disk
 */
export async function saveDataUrl(dataUrl, filename, options = {}) {
  const res = await fetch(dataUrl)
  const blob = await res.blob()
  return saveFile(blob, filename, { successMsg: '保存成功', ...options })
}
