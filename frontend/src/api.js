async function req(method, url, body) {
  const opts = { method, headers: {} }
  if (body !== undefined) {
    opts.headers['Content-Type'] = 'application/json'
    opts.body = JSON.stringify(body)
  }
  const res = await fetch(url, opts)
  if (!res.ok) {
    let msg = `请求失败 (${res.status})`
    try {
      const j = await res.json()
      if (j.error) msg = j.error
    } catch { /* ignore */ }
    throw new Error(msg)
  }
  return res.json()
}

const qs = (params) => {
  const p = Object.entries(params || {}).filter(([, v]) => v !== undefined && v !== null && v !== '')
  return p.length ? '?' + new URLSearchParams(p).toString() : ''
}

export const api = {
  stats: () => req('GET', '/api/stats'),
  products: () => req('GET', '/api/products'),
  createProduct: (data) => req('POST', '/api/products', data),

  items: (params) => req('GET', '/api/items' + qs(params)),
  trail: (code) => req('GET', `/api/items/${code}/trail`),
  stockIn: (data) => req('POST', '/api/items/stock-in', data),
  issue: (code, data) => req('POST', `/api/items/${code}/issue`, data),
  transfer: (code, data) => req('POST', `/api/items/${code}/transfer`, data),
  freeze: (code, data) => req('POST', `/api/items/${code}/freeze`, data),
  unfreeze: (code, data) => req('POST', `/api/items/${code}/unfreeze`, data),
  remove: (code, data) => req('POST', `/api/items/${code}/remove`, data),
  inspect: (code, data) => req('POST', `/api/items/${code}/inspect`, data),

  inspections: () => req('GET', '/api/inspections'),
  recalls: () => req('GET', '/api/recalls'),
  syncRecalls: () => req('POST', '/api/recalls/sync'),
  scanExpired: () => req('POST', '/api/maintenance/scan-expired'),

  destructions: () => req('GET', '/api/destructions'),
  destroy: (data) => req('POST', '/api/destructions', data),
  reconcile: () => req('GET', '/api/destructions/reconcile')
}

export const STATUS_LABEL = {
  SEALED: '未开封备品',
  IN_USE: '在用',
  FROZEN: '冻结',
  REMOVED: '待销毁',
  DESTROYED: '已销毁'
}

export const CONDITION_LABEL = {
  NORMAL: '正常',
  NEAR_EXPIRY: '临期',
  EXPIRED: '已过期'
}

export const EVENT_LABEL = {
  STOCK_IN: '备品入库',
  ISSUE: '领用开封',
  TRANSFER: '调柜',
  INSPECT: '巡查',
  FREEZE: '冻结',
  UNFREEZE: '解冻',
  REMOVE: '下架',
  DESTROY: '销毁'
}

export const RESULT_LABEL = {
  NORMAL: '正常',
  CONTAMINATED: '污染',
  NEAR_EXPIRY: '临期',
  EXPIRED: '到期'
}

export const REASON_LABEL = {
  EXPIRED: '到期',
  RECALLED: '召回',
  CONTAMINATED: '污染',
  MANUAL: '手动'
}

export function fmtDate(d) {
  return d || '—'
}

export function fmtDateTime(d) {
  return d ? d.replace('T', ' ').slice(0, 16) : '—'
}
