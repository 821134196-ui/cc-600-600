const BASE = ''

async function request(path, options = {}) {
  const res = await fetch(BASE + path, {
    headers: options.body ? { 'Content-Type': 'application/json' } : {},
    ...options,
    body: options.body ? JSON.stringify(options.body) : undefined
  })
  if (!res.ok) {
    let msg = `HTTP ${res.status}`
    try {
      const data = await res.json()
      if (data.error) msg = data.error
    } catch { /* ignore */ }
    throw new Error(msg)
  }
  return res.json()
}

export const api = {
  // 统计与列表
  stats: () => request('/api/testers/stats'),
  testers: (params = {}) => {
    const q = new URLSearchParams(Object.entries(params).filter(([, v]) => v))
    return request('/api/testers' + (q.size ? '?' + q : ''))
  },
  tester: (code) => request(`/api/testers/${encodeURIComponent(code)}`),
  events: (code) => request(`/api/testers/${encodeURIComponent(code)}/events`),
  counters: () => request('/api/testers/counters'),
  nextCode: () => request('/api/testers/next-code'),
  enforceExpiries: () => request('/api/testers/enforce-expiries', { method: 'POST' }),

  // 批次
  batches: () => request('/api/batches'),
  createBatch: (body) => request('/api/batches', { method: 'POST', body }),

  // 库存与生命周期
  stockIn: (body) => request('/api/testers/stock-in', { method: 'POST', body }),
  issue: (body) => request('/api/testers/issue', { method: 'POST', body }),
  transfer: (code, body) => request(`/api/testers/${encodeURIComponent(code)}/transfer`, { method: 'POST', body }),
  freeze: (code, body) => request(`/api/testers/${encodeURIComponent(code)}/freeze`, { method: 'POST', body }),
  remove: (code, body) => request(`/api/testers/${encodeURIComponent(code)}/remove`, { method: 'POST', body }),
  patrol: (code, body) => request(`/api/testers/${encodeURIComponent(code)}/patrol`, { method: 'POST', body }),
  patrols: () => request('/api/patrols'),

  // 销毁
  pendingDestruction: () => request('/api/destructions/pending'),
  destructions: () => request('/api/destructions'),
  reconcile: () => request('/api/destructions/reconcile'),
  destroy: (body) => request('/api/destructions', { method: 'POST', body }),

  // 召回
  recalls: () => request('/api/recalls'),
  syncRecalls: () => request('/api/recalls/sync', { method: 'POST' })
}

export const STATUS_LABEL = {
  UNOPENED: '未开封',
  IN_USE: '在用',
  FROZEN: '冻结',
  REMOVED: '已下架',
  DESTROYED: '已销毁'
}

export const EVENT_LABEL = {
  STOCK_IN: '备品入库',
  ISSUED: '领用开封',
  TRANSFERRED: '调柜',
  FROZEN: '冻结',
  PATROL: '巡查',
  REMOVED: '手动下架',
  EXPIRED_REMOVED: '过期下架',
  RECALL_REMOVED: '召回下架',
  DESTROYED: '销毁'
}

export const CONDITION_LABEL = {
  NORMAL: '正常',
  CONTAMINATED: '污染',
  NEAR_EXPIRY: '临期',
  EXPIRED: '到期'
}

export function fmtDate(d) {
  return d || '—'
}

export function fmtTime(t) {
  if (!t) return '—'
  return new Date(t).toLocaleString('zh-CN', { hour12: false })
}
