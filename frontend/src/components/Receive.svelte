<script>
  import { onMount, createEventDispatcher } from 'svelte'
  import { api, fmtDate } from '../api.js'

  const dispatch = createEventDispatcher()
  let products = []
  let sealedItems = []
  let today = new Date().toISOString().slice(0, 10)

  // 备品入库表单
  let stockForm = { productId: '', code: '', operator: '' }
  // 领用表单
  let issueForm = { code: '', openedDate: today, counterLocation: '', operator: '' }
  // 新批次表单
  let prodForm = { name: '', brand: '', batchNo: '', productionDate: today, shelfMonths: 36, paoMonths: 6 }

  onMount(load)

  async function load() {
    try {
      products = await api.products()
      sealedItems = (await api.items({ status: 'SEALED' }))
    } catch (e) {
      dispatch('error', { message: e.message })
    }
  }

  async function stockIn() {
    try {
      const r = await api.stockIn({
        productId: Number(stockForm.productId),
        code: stockForm.code || null,
        operator: stockForm.operator
      })
      stockForm = { productId: '', code: '', operator: stockForm.operator }
      dispatch('done', { message: `备品 ${r.code} 已入库` })
    } catch (e) {
      dispatch('error', { message: e.message })
    }
  }

  async function issue() {
    try {
      const r = await api.issue(issueForm.code, {
        openedDate: issueForm.openedDate || null,
        counterLocation: issueForm.counterLocation,
        operator: issueForm.operator
      })
      dispatch('done', { message: `${r.code} 已领用，到期日 ${r.expiryDate}` })
      issueForm = { code: '', openedDate: today, counterLocation: '', operator: issueForm.operator }
    } catch (e) {
      dispatch('error', { message: e.message })
    }
  }

  async function addProduct() {
    try {
      await api.createProduct({
        ...prodForm,
        shelfMonths: Number(prodForm.shelfMonths),
        paoMonths: Number(prodForm.paoMonths)
      })
      prodForm = { name: '', brand: '', batchNo: '', productionDate: today, shelfMonths: 36, paoMonths: 6 }
      dispatch('done', { message: '商品批次已登记' })
    } catch (e) {
      dispatch('error', { message: e.message })
    }
  }
</script>

<div class="cols">
  <div class="card">
    <h2>备品入库（未开封）</h2>
    <div class="field">
      <label>正装批次 *</label>
      <select bind:value={stockForm.productId}>
        <option value="">请选择…</option>
        {#each products as p}<option value={p.id}>{p.name}（{p.batchNo}）</option>{/each}
      </select>
    </div>
    <div class="field"><label>独立编号（留空自动生成）</label><input bind:value={stockForm.code} placeholder="如 T-00015" /></div>
    <div class="field"><label>经手人</label><input bind:value={stockForm.operator} /></div>
    <button class="primary" on:click={stockIn} disabled={!stockForm.productId}>入库</button>
  </div>

  <div class="card">
    <h2>领用开封</h2>
    <div class="field">
      <label>备品 *</label>
      <select bind:value={issueForm.code}>
        <option value="">请选择…</option>
        {#each sealedItems as i}<option value={i.code}>{i.code} — {i.productName}</option>{/each}
      </select>
    </div>
    <div class="field"><label>开封日期</label><input type="date" bind:value={issueForm.openedDate} /></div>
    <div class="field"><label>柜位 *</label><input bind:value={issueForm.counterLocation} placeholder="如 A-01" /></div>
    <div class="field"><label>经手人</label><input bind:value={issueForm.operator} /></div>
    <button class="primary" on:click={issue} disabled={!issueForm.code || !issueForm.counterLocation}>领用</button>
    <p class="muted">冻结的试用品不会出现在备品列表中，且系统会拒绝其领用。</p>
  </div>
</div>

<div class="card">
  <h2>正装商品批次</h2>
  <div class="row" style="margin-bottom:12px">
    <div class="field"><label>商品名称 *</label><input bind:value={prodForm.name} /></div>
    <div class="field"><label>品牌</label><input bind:value={prodForm.brand} /></div>
    <div class="field"><label>批次号 *</label><input bind:value={prodForm.batchNo} /></div>
    <div class="field"><label>生产日期</label><input type="date" bind:value={prodForm.productionDate} /></div>
    <div class="field"><label>未开封保质期(月)</label><input type="number" bind:value={prodForm.shelfMonths} style="width:90px" /></div>
    <div class="field"><label>开封后效期(月)</label><input type="number" bind:value={prodForm.paoMonths} style="width:90px" /></div>
    <button class="primary" on:click={addProduct} disabled={!prodForm.name || !prodForm.batchNo}>登记批次</button>
  </div>
  <table>
    <thead><tr><th>商品</th><th>品牌</th><th>批次号</th><th>生产日期</th><th>未开封保质期</th><th>开封后效期</th></tr></thead>
    <tbody>
      {#each products as p}
        <tr>
          <td>{p.name}</td><td class="muted">{p.brand}</td><td><strong>{p.batchNo}</strong></td>
          <td>{fmtDate(p.productionDate)}</td><td>{p.shelfMonths} 个月</td><td>{p.paoMonths} 个月</td>
        </tr>
      {/each}
    </tbody>
  </table>
</div>

<style>
  .cols { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
  @media (max-width: 800px) { .cols { grid-template-columns: 1fr; } }
  .field { margin-bottom: 10px; }
</style>
