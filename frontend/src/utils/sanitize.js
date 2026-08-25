/**
 * 清理 HTML 字符串，防止 XSS
 * 白名单标签 + 属性过滤，其余全部转义
 */
const SAFE_TAGS = /^(strong|b|i|em|br|p|ul|ol|li|h[1-6]|a|img|blockquote|code|pre|span|div|hr|table|thead|tbody|tr|th|td|dl|dt|dd|sup|sub)$/i
const SAFE_ATTRS = /^(style|class|id|href|src|alt|title|colspan|rowspan|width|height|align|valign|target|rel)$/i

// 危险 CSS 属性/值
const DANGEROUS_CSS = /expression\s*\(|url\s*\(|behavior\s*:|-moz-binding\s*:|@import|javascript\s*:/i

export function sanitizeHTML(html) {
  if (!html) return ''
  const doc = new DOMParser().parseFromString(html, 'text/html')
  sanitizeNode(doc.body)
  return doc.body.innerHTML
}

function sanitizeNode(node) {
  for (let i = node.childNodes.length - 1; i >= 0; i--) {
    const child = node.childNodes[i]
    if (child.nodeType === Node.TEXT_NODE) continue
    if (child.nodeType !== Node.ELEMENT_NODE) {
      child.remove()
      continue
    }
    const tag = child.tagName.toLowerCase()
    if (!SAFE_TAGS.test(tag)) {
      const text = child.textContent
      child.replaceWith(document.createTextNode(text))
      continue
    }
    // 移除不安全属性
    for (const attr of [...child.attributes]) {
      const name = attr.name.toLowerCase()
      const val = attr.value
      if (!SAFE_ATTRS.test(name)) {
        child.removeAttribute(attr.name)
        continue
      }
      // href/src 只允许 http/https/mailto/data:image
      if (name === 'href' || name === 'src') {
        const trimmed = val.trim().toLowerCase()
        if (!/^(https?:\/\/|mailto:|data:image\/)/.test(trimmed)) {
          child.removeAttribute(attr.name)
        }
        continue
      }
      // style 过滤危险 CSS
      if (name === 'style' && DANGEROUS_CSS.test(val)) {
        child.removeAttribute(attr.name)
      }
    }
    sanitizeNode(child)
  }
}
