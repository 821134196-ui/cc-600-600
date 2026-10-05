<script>
  import { onMount } from 'svelte'
  import { api, CONDITION_LABEL, fmtDate, fmtTime } from '../api.js'
  import { showToast } from '../stores.js'

  let inUse = []
  let records = []
  let form = {
    itemCode: '',
    condition: 'NORMAL',
    note: '',
    inspector: '',
    patrolDate: new Date().toISOString().slice(0, 10)
  }

  async function load() {
    try {
      ;[inUse, records] = await Promise.all([
        api.testers({ status: 'IN_USE' }),
        api.patrols()
      ])
    } catch (e) {
      showToast(e.message, 'err')
    }
  }

  async function submit() {
    try {
      await api.patrol(form.itemCode, { ...form })
      const msg = form.condition === 'CONTAMINATED' || form.condition === 'EXPIRED'
        ? `巡查已登记，${form.itemCode} 已立即下架`
        : `巡查已登记`
      showToast(msg)
      form.note = ''
      load()
    } catch (e) {
      showToast(e.message, 'err')
    }
  }

  onMount(load)
</script>

<h2>巡查记录</h2>
<div class="sub">店员巡查登记污染、临期、到期情况；污染或到期样品登记后立即下架</div>

<div class="card">
  <h3>登记巡查</h3>
  <div class="form-grid">
    <div class="field">
      <label>试用品（在用）</label>
      <select bind:value={form.itemCode}>
        <option value="">请选择</option>
        {#each inUse as t}
          <option value={t.itemCode}>
            {t.itemCode} — {t.productName}（{t.counterLocation}）
            {t.nearExpiry ? '【临期】' : ''}
          </option>
        {/each}
      </select>
    </div>
    <div class="field">
      <label>巡查结论</label>
      <select bind:value={form.condition}>
        <option value="NORMAL">正常</option>
        <option value="CONTAMINATED">污染 → 立即下架</option>
        <option value="NEAR_EXPIRY">临期</option>
        <option value="EXPIRED">到期 → 立即下架</option>
      </select>
    </div>
    <div class="field"><label>巡查日期</label><input type="date" bind:value={form.patrolDate} /></div>
    <div class="field"><label>巡查人</label><input bind:value={form.inspector} /></div>
    <div class="field" style="grid-column: 1 / -1"><label>备注</label><input bind:value={form.note} placeholder="如：瓶口污渍、气味异常…" /></div>
  </div>
  <div style="margin-top:14px">
    <button class="btn" disabled={!form.itemCode} on:click={submit}>登记</button>
  </div>
</div>

<div class="card">
  <h3>最近巡查（{records.length}）</h3>
  <table>
    <thead>
      <tr><th>时间</th><th>编号</th><th>品名</th><th>巡查日期</th><th>结论</th><th>备注</th><th>巡查人</th></tr>
    </thead>
    <tbody>
      {#each records as r}
        <tr>
          <td class="mono">{fmtTime(r.createdAt)}</td>
          <td class="mono">{r.itemCode}</td>
          <td>{r.productName}</td>
          <td>{fmtDate(r.patrolDate)}</td>
          <td>
            <span class="badge {r.condition === 'NORMAL' ? 'IN_USE' : r.condition === 'NEAR_EXPIRY' ? 'warn' : 'danger'}">
              {CONDITION_LABEL[r.condition]}
            </span>
          </td>
          <td>{r.note || '—'}</td>
          <td>{r.inspector || '—'}</td>
        </tr>
      {:else}
        <tr><td colspan="7" class="empty">暂无巡查记录</td></tr>
      {/each}
    </tbody>
  </table>
</div>
