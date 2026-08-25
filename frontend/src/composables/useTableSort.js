import { ref, computed, watch } from 'vue'

/**
 * Reusable client-side table sorting composable.
 *
 * @param {Ref<Array>} dataRef - reactive ref of the data array to sort
 * @param {Object} fieldTypes - optional map of field → 'number'|'date'|'string' for correct comparison
 * @param {Object} opts - optional options
 * @param {string} opts.storageKey - sessionStorage key to persist sort state
 * @returns {{ sortField, sortDir, sortedList, toggleSort, sortArrow }}
 */
export function useTableSort(dataRef, fieldTypes = {}, opts = {}) {
  const sortField = ref('')
  const sortDir = ref('asc')

  // Restore from sessionStorage if key provided
  const sortKey = opts.storageKey ? `${opts.storageKey}_sort` : null
  if (sortKey) {
    try {
      const saved = JSON.parse(sessionStorage.getItem(sortKey) || '{}')
      if (saved.sortField) sortField.value = saved.sortField
      if (saved.sortDir) sortDir.value = saved.sortDir
    } catch {}

    watch([sortField, sortDir], () => {
      try {
        const data = { sortField: sortField.value || undefined, sortDir: sortDir.value !== 'asc' ? sortDir.value : undefined }
        sessionStorage.setItem(sortKey, JSON.stringify(data))
      } catch {}
    })
  }

  const sortedList = computed(() => {
    const arr = [...dataRef.value]
    if (!sortField.value) return arr

    arr.sort((a, b) => {
      let va = a[sortField.value]
      let vb = b[sortField.value]

      const type = fieldTypes[sortField.value] || 'string'

      if (type === 'number') {
        va = Number(va) || 0
        vb = Number(vb) || 0
        return sortDir.value === 'asc' ? va - vb : vb - va
      }

      if (type === 'date') {
        va = va ? new Date(va).getTime() : 0
        vb = vb ? new Date(vb).getTime() : 0
        return sortDir.value === 'asc' ? va - vb : vb - va
      }

      // string
      va = (va || '').toString()
      vb = (vb || '').toString()
      const cmp = va.localeCompare(vb)
      return sortDir.value === 'asc' ? cmp : -cmp
    })

    return arr
  })

  function toggleSort(field) {
    if (sortField.value === field) {
      if (sortDir.value === 'asc') sortDir.value = 'desc'
      else { sortField.value = ''; sortDir.value = 'asc' }
    } else {
      sortField.value = field
      sortDir.value = 'asc'
    }
  }

  function sortArrow(field) {
    if (sortField.value !== field) return ''
    return sortDir.value === 'asc' ? '↑' : '↓'
  }

  return { sortField, sortDir, sortedList, toggleSort, sortArrow }
}
