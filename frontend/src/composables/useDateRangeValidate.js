import { ElMessage } from 'element-plus'

/**
 * 校验日期范围：开始日期不能大于结束日期
 * @param {string} start - 开始日期 (YYYY-MM-DD)
 * @param {string} end - 结束日期 (YYYY-MM-DD)
 * @param {'start'|'end'} changed - 哪个日期刚被修改
 * @returns {boolean} 是否合法
 */
export function validateDateRange(start, end, changed) {
  if (!start || !end) return true
  if (start > end) {
    if (changed === 'start') {
      ElMessage.warning('开始日期不能晚于结束日期，请重新选择')
    } else {
      ElMessage.warning('结束日期不能早于开始日期，请重新选择')
    }
    return false
  }
  return true
}
