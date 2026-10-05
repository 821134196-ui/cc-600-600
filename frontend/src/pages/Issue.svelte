<script>
  import { onMount } from 'svelte'
  import { api } from '../api.js'
  import { showToast } from '../stores.js'

  let batches = []
  let unopened = []

  // 入库表单
  let stockForm = { batchId: '', count: 1, operator: '' }
  // 领用表单
  let issueForm = { itemCode: '', openedDate: new Date().toISOString().slice(0, 10), counterLocation: '', operator: '' }

  async function load() {
    try {
      batches = await api.batches()
      const all = await api.testers({ status: 'UNOPENED' })
      unopened = all
    } catch (e) {
      showToast(e.message, 'err')
    }
  }

  async function stockIn() {
    try {
      const created = await api.stockIn({
        batchId: Number(stockForm.batchId),
        count: Number(stockForm.count),
        operator: stockForm.operator
      })
      showToast(`入库成功：${created.map(i => i.itemCode).join('、')}`)
      stockForm.count = 1
      load()
    } catch (e) {
      showToast(e.message, 'err')
    }
  }

  async function issue() {
    try {
      const item = await api.issue({ ...issueForm })
      showToast(`${item.itemCode} 领用成功，效期至 ${item.expiryDate}`)
      issueForm.itemCode = ''
      load()
    } catch (e) {
      showToast(e.message, 'err')
    }
  }

  onMount(load)
</script>

<h2>入库与领用</h2>
<div class="sub">备品入库后获得独立编号；领用时登记正装批次、开封日期与柜位，效期自开封日起算</div>

<div class="two-col">
  <div class="card">
    <h3>① 备品入库（未开封）</h3>
    <div class="form-grid">
      <div class="field">
        <label>正装批次</label>
        <select bind:value={stockForm.batchId}>
          <option value="">请选择</option>
          {#each batches as b}
            <option value={b.id} disabled={b.recalled}>
              {b.batchNo} — {b.productName}{b.recalled ? '（已召回）' : ''}
            </option>
          {/each}
        </select>
      </div>
      <div class="field"><label>数量</label><input type="number" min="1" max="500" bind:value={stockForm.count} /></div>
      <div class="field"><label>经手人</label><input bind:value={stockForm.operator} placeholder="库管" /></div>
    </div>
    <div style="margin-top:14px">
      <button class="btn" disabled={!stockForm.batchId} on:click={stockIn}>入库</button>
    </div>
  </div>

  <div class="card">
    <h3>② 领用开封</h3>
    <div class="form-grid">
      <div class="field">
        <label>试用品编号（未开封备品）</label>
        <select bind:value={issueForm.itemCode}>
          <option value="">请选择</option>
          {#each unopened as u}
            <option value={u.itemCode}>{u.itemCode} — {u.productName}</option>
          {/each}
        </select>
      </div>
      <div class="field"><label>开封日期</label><input type="date" bind:value={issueForm.openedDate} /></div>
      <div class="field"><label>柜位</label><input bind:value={issueForm.counterLocation} placeholder="如 A-01" /></div>
      <div class="field"><label>经手人</label><input bind:value={issueForm.operator} placeholder="柜长" /></div>
    </div>
    <div style="margin-top:14px">
      <button class="btn" disabled={!issueForm.itemCode || !issueForm.counterLocation} on:click={issue}>领用</button>
    </div>
  </div>
</div>

<div class="card">
  <h3>未开封备品库存（{unopened.length} 件）</h3>
  {#if unopened.length === 0}
    <div class="empty">暂无备品</div>
  {:else}
    <table>
      <thead><tr><th>编号</th><th>品名</th><th>批次</th><th>品牌</th><th>PAO（月）</th></tr></thead>
      <tbody>
        {#each unopened as u}
          <tr>
            <td class="mono">{u.itemCode}</td>
            <td>{u.productName}</td>
            <td class="mono">{u.batchNo}</td>
            <td>{u.brand || '—'}</td>
            <td>{u.paoMonths}</td>
          </tr>
        {/each}
      </tbody>
    </table>
  {/if}
</div>
