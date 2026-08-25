/**
 * 滚动触发显形 — 直接执行（调用时 DOM 已就绪）
 * @param {string} selector - CSS 选择器
 */
export function runScrollReveal(selector = '.reveal') {
  const els = document.querySelectorAll(selector)
  if (!els.length) return

  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('revealed')
        observer.unobserve(entry.target)
      }
    })
  }, { threshold: 0.1, rootMargin: '0px 0px -40px 0px' })

  els.forEach(el => observer.observe(el))
}
