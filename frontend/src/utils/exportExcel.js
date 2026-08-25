import * as XLSX from 'xlsx'
import { saveFile } from '@/utils/download'

/**
 * Export data to Excel file with professional styling (exceljs)
 * @param {Array} data - Array of objects
 * @param {Array} columns - [{key, label, width?, format?, _excelType?}]
 * @param {String} filename - File name without extension
 * @param {Object} options - { mergeKeys?, sheetName?, headerColor?, headerFontColor?, zebraColor? }
 */
export async function exportToExcel(data, columns, filename = 'export', options = {}) {
  const ExcelJS = await import('exceljs').then(m => m.default || m)

  const {
    mergeKeys = [],
    sheetName = 'Sheet1',
    headerColor = 'FF2B579A',
    headerFontColor = 'FFFFFFFF',
    zebraColor = 'FFF2F7FB',
    mergeColor = 'FFE8F0FE',
  } = options

  // Auto-detect column types from format functions
  columns.forEach(col => {
    if (col._excelType) return
    if (col.format === fmtDate) col._excelType = 'date'
    else if (col.format === fmtMoney) col._excelType = 'number'
  })

  const wb = new ExcelJS.Workbook()
  const ws = wb.addWorksheet(sheetName)

  // 1. Column definitions
  ws.columns = columns.map(col => ({
    header: col.label,
    key: col.key,
    width: col.width || 12,
  }))

  // 2. Header styling
  const headerRow = ws.getRow(1)
  headerRow.height = 28
  headerRow.eachCell(cell => {
    cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: headerColor } }
    cell.font = { bold: true, color: { argb: headerFontColor }, size: 11 }
    cell.alignment = { vertical: 'middle', horizontal: 'center', wrapText: true }
    cell.border = thinBorder()
  })

  // 3. Data rows
  for (const row of data) {
    const values = columns.map(col => {
      let val = row[col.key]
      if (val == null) return null
      if (col.format) val = col.format(val, row)
      return val
    })
    const r = ws.addRow(values)
    r.height = 22
    r.eachCell({ includeEmpty: true }, (cell, colNumber) => {
      const col = columns[colNumber - 1]
      if (col && col._excelType === 'number' && cell.value != null && cell.value !== '') {
        cell.numFmt = '#,##0.00'
      }
      if (col && col._excelType === 'date' && cell.value != null) {
        cell.numFmt = 'yyyy-mm-dd'
      }
      cell.border = thinBorder()
      cell.alignment = { vertical: 'middle' }
    })
  }

  // 4. Zebra striping
  for (let i = 2; i <= data.length + 1; i++) {
    if (i % 2 === 0) {
      const row = ws.getRow(i)
      row.eachCell({ includeEmpty: true }, cell => {
        cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: zebraColor } }
      })
    }
  }

  // 5. Vertical merge for repeated order-level columns
  // Build formatted values array for merge comparison
  const formattedRows = data.map(row =>
    columns.reduce((obj, col) => {
      let val = row[col.key]
      if (val != null && col.format) val = col.format(val, row)
      obj[col.key] = val
      return obj
    }, {})
  )
  // Determine order groups: rows with the same orderNo belong to one order.
  // Use the first mergeKey (typically 'orderNo') as the group boundary.
  const groupKey = mergeKeys[0]
  const groupColIdx = groupKey ? columns.findIndex(c => c.key === groupKey) : -1
  for (const key of mergeKeys) {
    const colIdx = columns.findIndex(c => c.key === key) + 1
    if (colIdx <= 0) continue
    let startRow = 2
    for (let i = 2; i <= data.length + 1; i++) {
      const curVal = mergeVal(formattedRows[i - 2], key)
      const nxtVal = mergeVal(formattedRows[i - 1], key)
      // Break merge when: value differs OR order group changes
      const groupChanged = groupColIdx >= 0 && key !== groupKey
        ? mergeVal(formattedRows[i - 2], groupKey) !== mergeVal(formattedRows[i - 1], groupKey)
        : false
      if (curVal !== nxtVal || groupChanged || i === data.length + 1) {
        if (i > startRow) {
          ws.mergeCells(startRow, colIdx, i, colIdx)
          const mergedCell = ws.getCell(startRow, colIdx)
          mergedCell.alignment = { vertical: 'middle', horizontal: 'center' }
          mergedCell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: mergeColor } }
          mergedCell.font = { bold: true, size: 11 }
        }
        startRow = i + 1
      }
    }
  }

  // 6. Auto filter
  const lastCol = colIndexToLetter(columns.length)
  ws.autoFilter = { from: 'A1', to: `${lastCol}1` }

  // 7. Freeze header row
  ws.views = [{ state: 'frozen', ySplit: 1 }]

  // 8. Export
  const buffer = await wb.xlsx.writeBuffer()
  const blob = new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  await saveFile(blob, `${filename}.xlsx`, { successMsg: '导出成功' })
}

/** Extract a comparable primitive from a row value (Dates → timestamp) */
function mergeVal(row, key) {
  if (!row) return undefined
  const v = row[key]
  if (v instanceof Date) return v.getTime()
  return v
}

function thinBorder() {
  return {
    top: { style: 'thin', color: { argb: 'FF999999' } },
    left: { style: 'thin', color: { argb: 'FF999999' } },
    bottom: { style: 'thin', color: { argb: 'FF999999' } },
    right: { style: 'thin', color: { argb: 'FF999999' } },
  }
}

/** Convert 1-based column index to Excel letter (1→A, 27→AA) */
function colIndexToLetter(idx) {
  let letter = ''
  while (idx > 0) {
    const rem = (idx - 1) % 26
    letter = String.fromCharCode(65 + rem) + letter
    idx = Math.floor((idx - 1) / 26)
  }
  return letter
}

/**
 * Format date — returns Date object for native Excel date type
 */
export function fmtDate(val) {
  if (!val) return null
  const d = new Date(val)
  if (isNaN(d.getTime())) return String(val)
  return d
}

/**
 * Format money — returns number for native Excel number type
 */
export function fmtMoney(val) {
  if (val == null || val === '') return null
  return Number(val)
}

/**
 * Parse an Excel file and return raw headers + rows
 * @param {File} file
 * @returns {Promise<{headers: string[], rows: any[][]}>}
 */
export function parseExcelFile(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      try {
        const wb = XLSX.read(e.target.result, { type: 'array' })
        const ws = wb.Sheets[wb.SheetNames[0]]
        const data = XLSX.utils.sheet_to_json(ws, { header: 1, defval: '' })
        if (data.length < 2) {
          reject(new Error('Excel 文件至少需要包含标题行和一行数据'))
          return
        }
        const headers = data[0].map(h => String(h).trim())
        const rows = data.slice(1)
        resolve({ headers, rows })
      } catch (err) {
        reject(new Error('解析 Excel 文件失败：' + err.message))
      }
    }
    reader.onerror = () => reject(new Error('读取文件失败'))
    reader.readAsArrayBuffer(file)
  })
}

/**
 * Convert Excel serial date number to YYYY-MM-DD string
 */
function excelDateToISO(serial) {
  const d = new Date((serial - 25569) * 86400 * 1000)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/**
 * Map parsed Excel rows to objects using a column mapping config
 * @param {string[]} headers - Excel column headers (first row)
 * @param {any[][]} rows - Data rows (without header)
 * @param {Array<{label: string, key: string, reverseMap?: Object, type?: string, required?: boolean, itemField?: boolean}>} columnMap
 * @returns {{ records: Array<Object>, errors: Array<{row: number, message: string}>, unmatchedHeaders: string[] }}
 */
export function mapRows(headers, rows, columnMap) {
  // Fuzzy match Excel headers to column map labels
  const colIndexMap = {} // columnMap index → Excel column index
  const matchedExcelIndices = new Set()

  for (let ci = 0; ci < columnMap.length; ci++) {
    const expected = columnMap[ci].label
    let found = -1
    // Exact match first
    for (let hi = 0; hi < headers.length; hi++) {
      if (matchedExcelIndices.has(hi)) continue
      if (headers[hi] === expected) { found = hi; break }
    }
    // Fuzzy match (contains)
    if (found === -1) {
      for (let hi = 0; hi < headers.length; hi++) {
        if (matchedExcelIndices.has(hi)) continue
        const h = headers[hi].replace(/\s+/g, '')
        const e = expected.replace(/\s+/g, '')
        if (h.includes(e) || e.includes(h)) { found = hi; break }
      }
    }
    if (found !== -1) {
      colIndexMap[ci] = found
      matchedExcelIndices.add(found)
    }
  }

  // Find unmatched Excel headers
  const unmatchedHeaders = headers.filter((_, i) => !matchedExcelIndices.has(i) && headers[i])

  // Detect grouped-order imports (orderNo + item fields) so we can carry forward
  // order-level cells that the "detailed" export leaves empty via vertical cell merges.
  const hasItemFields = columnMap.some(c => c.itemField)
  const groupColIdx = columnMap.findIndex(c => c.key === 'orderNo')
  const groupExcelIdx = groupColIdx >= 0 ? colIndexMap[groupColIdx] : -1
  const headerColIdxs = []
  if (hasItemFields && groupExcelIdx >= 0) {
    for (let ci = 0; ci < columnMap.length; ci++) {
      if (!columnMap[ci].itemField && !columnMap[ci].skip && colIndexMap[ci] !== undefined) headerColIdxs.push(ci)
    }
  }
  let lastOrderVals = null

  const records = []
  const errors = []

  for (let ri = 0; ri < rows.length; ri++) {
    const row = rows[ri]
    // Skip completely empty rows
    if (!row || row.every(cell => cell === '' || cell == null)) continue

    // Carry forward merged order-level cells from the previous row of the same order
    if (hasItemFields && groupExcelIdx >= 0) {
      const cellRaw = row[groupExcelIdx]
      const isNewGroup = cellRaw !== '' && cellRaw != null
      if (isNewGroup) {
        lastOrderVals = {}
        for (const ci of headerColIdxs) {
          lastOrderVals[ci] = row[colIndexMap[ci]]
        }
      } else if (lastOrderVals) {
        for (const ci of headerColIdxs) {
          const excelIdx = colIndexMap[ci]
          if (row[excelIdx] === '' || row[excelIdx] == null) {
            row[excelIdx] = lastOrderVals[ci]
          }
        }
      }
    }

    const record = {}
    let hasData = false

    for (let ci = 0; ci < columnMap.length; ci++) {
      const col = columnMap[ci]
      // skip columns are matched (so they don't show as unmatched headers) but
      // are derived/ignored values — never mapped to records or validated
      if (col.skip) continue
      const excelIdx = colIndexMap[ci]
      let val = excelIdx !== undefined ? row[excelIdx] : undefined

      if (val === '' || val == null) {
        if (col.required) {
          errors.push({ row: ri + 2, message: `第 ${ri + 2} 行：「${col.label}」不能为空` })
        }
        record[col.key] = col.type === 'number' ? null : ''
        continue
      }

      hasData = true

      // Type conversion
      if (col.type === 'number') {
        const num = Number(val)
        if (isNaN(num)) {
          errors.push({ row: ri + 2, message: `第 ${ri + 2} 行：「${col.label}」应为数字，实际值为"${val}"` })
          record[col.key] = null
        } else {
          record[col.key] = num
        }
      } else if (col.type === 'date') {
        if (typeof val === 'number') {
          record[col.key] = excelDateToISO(val)
        } else {
          const s = String(val).trim()
          // Try to parse common date formats
          const d = new Date(s)
          if (!isNaN(d.getTime())) {
            record[col.key] = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
          } else {
            record[col.key] = s
          }
        }
      } else {
        record[col.key] = String(val).trim()
      }

      // Reverse mapping (e.g., status text → code)
      if (col.reverseMap && record[col.key]) {
        const mapped = col.reverseMap[record[col.key]]
        if (mapped !== undefined) {
          record[col.key] = mapped
        }
      }
    }

    if (hasData) {
      record._excelRow = ri + 2 // Excel row number (1-indexed, +1 for header)
      records.push(record)
    }
  }

  return { records, errors, unmatchedHeaders }
}

/**
 * Group flat order rows (one row per item) into order objects with items arrays
 * @param {Array<Object>} records - Flat records from mapRows
 * @param {string} orderKey - The key used to group (e.g., 'orderNo')
 * @param {Array<{key: string, itemField?: boolean}>} columnMap - Column map with itemField flags
 * @returns {Array<Object>} - Grouped order objects with items arrays
 */
export function groupOrderRows(records, orderKey, columnMap) {
  const headerKeys = columnMap.filter(c => !c.itemField && !c.skip).map(c => c.key)
  const itemKeys = columnMap.filter(c => c.itemField && !c.skip).map(c => c.key)

  const orderMap = new Map()
  const orderArr = []

  for (const rec of records) {
    const key = rec[orderKey] || `__auto_${orderArr.length + 1}`
    let order = orderMap.get(key)
    if (!order) {
      order = {}
      for (const hk of headerKeys) {
        order[hk] = rec[hk]
      }
      order.items = []
      orderMap.set(key, order)
      orderArr.push(order)
    }
    // Build item from item fields
    const item = {}
    let hasItem = false
    for (const ik of itemKeys) {
      if (rec[ik] != null && rec[ik] !== '') {
        hasItem = true
      }
      item[ik] = rec[ik] != null ? rec[ik] : null
    }
    if (hasItem) {
      order.items.push(item)
    }
  }

  return orderArr
}

/**
 * Download an import template Excel file with headers and one sample row.
 * @param {Array} columns - [{key, label, type?, itemField?}]
 * @param {String} filename - File name without extension
 * @param {Object} options - { sheetName?, sample? }
 */
export async function downloadTemplate(columns, filename = '导入模板', options = {}) {
  const ExcelJS = await import('exceljs').then(m => m.default || m)
  const { sheetName = '模板', sample = {}, groupBy } = options
  const SAMPLE_FILL = 'FFFBF3DB' // 浅黄：示例数据底色

  const wb = new ExcelJS.Workbook()
  const ws = wb.addWorksheet(sheetName)

  // Header row — 必填列标注 *
  const headers = columns.map(c => (c.required ? `${c.label}*` : c.label))
  const headerRow = ws.addRow(headers)
  headerRow.font = { bold: true, color: { argb: 'FFFFFFFF' }, size: 11 }
  headerRow.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF2B579A' } }
  headerRow.alignment = { horizontal: 'center', vertical: 'middle' }
  columns.forEach((c, i) => {
    if (c.required) {
      const cell = headerRow.getCell(i + 1)
      cell.font = { bold: true, color: { argb: 'FFFFD24D' }, size: 11 } // 黄色醒目
    }
  })

  // 示例数据行（浅黄底 = 参考数据，导入前替换/删除）
  const sampleRow = columns.map(c => {
    if (sample[c.key] !== undefined) return sample[c.key]
    if (c.type === 'number') return 0
    if (c.type === 'date') return '2025-01-01'
    return ''
  })
  const styleSample = (row) => {
    row.font = { color: { argb: 'FF555555' }, size: 10 }
    row.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: SAMPLE_FILL } }
  }
  styleSample(ws.addRow(sampleRow))
  // 分组单再补一行：同一订单号 = 同单第二条明细（演示一单多明细的结构）
  if (groupBy) {
    styleSample(ws.addRow(sampleRow.map(v => v)))
  }

  // Auto-width columns
  columns.forEach((c, i) => {
    const col = ws.getColumn(i + 1)
    const maxLen = Math.max(c.label.length * 2 + 1, 12)
    col.width = Math.min(maxLen, 30)
  })

  // Freeze header row
  ws.views = [{ state: 'frozen', ySplit: 1 }]

  // 填写说明页
  const info = wb.addWorksheet('填写说明')
  info.getColumn(1).width = 22
  info.getColumn(2).width = 10
  info.getColumn(3).width = 60
  info.addRow(['导入填写说明']).font = { bold: true, size: 14 }
  info.addRow([''])
  info.addRow(['● 第2行起（浅黄色）为示例数据，请替换成自己的数据，或整行删除后再导入。'])
  if (groupBy) {
    const label = groupBy === 'orderNo' ? '订单号' : groupBy
    info.addRow([`● 同一${label}的多行属于同一张单，每行=一条商品明细；增加明细=复制一行并保持${label}不变。`])
    info.addRow(['● 没有明细时（如简易导出），只保留一行、删除其余行，导入的订单将没有明细。'])
  }
  info.addRow(['● 带 * 的列为必填项，不能为空。'])
  info.addRow(['● 列名须与模板一致（支持顺序调整，但列名不能改）。'])
  info.addRow([''])
  const guideHeader = info.addRow(['列名', '必填', '填写说明'])
  guideHeader.font = { bold: true, color: { argb: 'FFFFFFFF' }, size: 11 }
  guideHeader.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF2B579A' } }
  for (const c of columns) {
    let note = ''
    if (c.reverseMap) note = '可选值: ' + Object.keys(c.reverseMap).join(' / ')
    if (c.type === 'date') note = (note ? note + '；' : '') + '日期格式: 2025-01-15'
    if (!note) note = c.itemField ? '商品明细字段' : ''
    info.addRow([c.label, c.required ? '是' : '否', note])
  }

  const buffer = await wb.xlsx.writeBuffer()
  const blob = new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  await saveFile(blob, filename + '.xlsx', { successMsg: '模板下载成功' })
}
