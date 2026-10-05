<script>
  import { onMount } from 'svelte'
  import { api, fmtDate } from '../api.js'
  import { showToast } from '../stores.js'

  let batches = []
  let form = { batchNo: '', productName: '', brand: '', category: '护肤', paoMonths: 6, productionDate: '' }

  async function load() {
    try {
      batches = await api.batches()
    } catch (e) {
      showToast(e.message, 'err')
    }
  }

  async function create() {
    try {
      await api.createBatch({
        ...form,
        paoMonths: Number(form.paoMonths),
        productionDate: form.productionDate || null
      })
      showToast(`批次 ${form.batchNo} 创建成功`)
      form = { batchNo: '', productName: '', brand: '', category: '护肤', paoMonths: 6, productionDate: '' }
      load()
    } catch (e) {
      showToast(e.message, 'err')
    }
  }

  onMount(load)
</script>

<h2>批次管理</h2>
<div class="sub">正装批次登记，PAO（开封后使用月数）是卫生效期的计算基准</div>

<div class="card">
  <h3>新建批次</h3>
  <div class="form-grid">
    <div class="field"><label>批次号 *</label><input bind:value={form.batchNo} placeholder="如 B2026-LC-005" /></div>
    <div class="field"><label>品名 *</label><input bind:value={form.productName} /></div>
    <div class="field"><label>品牌</label><input bind:value={form.brand} /></div>
    <div class="field">
      <label>品类</label>
      <select bind:value={form.category}>
        <option>护肤</option><option>彩妆</option><option>香水</option><option>个护</option><option>其他</option>
      </select>
    </div>
    <div class="field"><label>开封后使用月数（PAO）*</label><input type="number" min="1" max="60" bind:value={form.paoMonths} /></div>
    <div class="field"><label>生产日期</label><input type="date" bind:value={form.productionDate} /></div>
  </div>
  <div style="margin-top:14px">
    <button class="btn" disabled={!form.batchNo || !form.productName} on:click={create}>创建批次</button>
  </div>
</div>

<div class="card">
  <h3>批次列表（{batches.length}）</h3>
  <table>
    <thead>
      <tr><th>批次号</th><th>品名</th><th>品牌</th><th>品类</th><th>PAO（月）</th><th>生产日期</th><th>召回状态</th></tr>
    </thead>
    <tbody>
      {#each batches as b}
        <tr>
          <td class="mono">{b.batchNo}</td>
          <td>{b.productName}</td>
          <td>{b.brand || '—'}</td>
          <td>{b.category || '—'}</td>
          <td>{b.paoMonths}</td>
          <td>{fmtDate(b.productionDate)}</td>
          <td>
            {#if b.recalled}
              <span class="badge danger">已召回</span>
              <span style="color:var(--muted);font-size:12px">{b.recallReason || ''}</span>
            {:else}
              <span class="badge IN_USE">正常</span>
            {/if}
          </td>
        </tr>
      {:else}
        <tr><td colspan="7" class="empty">暂无批次</td></tr>
      {/each}
    </tbody>
  </table>
</div>
