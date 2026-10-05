<script>
  import { onMount, createEventDispatcher } from 'svelte'
  import { api, RESULT_LABEL, fmtDateTime } from '../api.js'

  const dispatch = createEventDispatcher()
  let records = []
  let inUseItems = []
  let form = { code: '', result: 'NORMAL', inspector: '', note: '' }

  onMount(load)

  async function load() {
    try {
      records = await api.inspections()
      inUseItems = await api.items({ status: 'IN_USE' })
    } catch (e) {
      dispatch('error', { message: e.message })
    }
  }

  async function submit() {
    try {
      await api.inspect(form.code, { result: form.result, inspector: form.inspector, note: form.note })
      dispatch('done', { message: `${form.code} 巡查已登记` })
      form = { code: '', result: 'NORMAL', inspector: form.inspector, note: '' }
    } catch (e) {
      dispatch('error', { message: e.message })
    }
  }
</script>

<div class="card">
  <h2>巡查登记</h2>
  <div class="row">
    <div class="field">
      <label>在用试用品 *</label>
      <select bind:value={form.code}>
        <option value="">请选择…</option>
        {#each inUseItems as i}
          <option value={i.code}>{i.code} — {i.productName}（{i.counterLocation || '未定位'}）</option>
        {/each}
      </select>
    </div>
    <div class="field">
      <label>结论</label>
      <select bind:value={form.result}>
        {#each Object.entries(RESULT_LABEL) as [k, v]}<option value={k}>{v}</option>{/each}
      </select>
    </div>
    <div class="field"><label>巡查人 *</label><input bind:value={form.inspector} /></div>
    <div class="field" style="flex:1"><label>备注</label><input bind:value={form.note} /></div>
    <button class="primary" on:click={submit} disabled={!form.code || !form.inspector}>登记</button>
  </div>
  <p class="muted">提示：结论为「污染」或「到期」时，试用品将立即自动下架并转入待销毁。</p>
</div>

<div class="card">
  <h2>巡查记录</h2>
  <table>
    <thead><tr><th>时间</th><th>编号</th><th>商品</th><th>结论</th><th>巡查人</th><th>备注</th></tr></thead>
    <tbody>
      {#each records as r}
        <tr>
          <td class="muted">{fmtDateTime(r.inspectedAt)}</td>
          <td><strong>{r.itemCode}</strong></td>
          <td>{r.productName}</td>
          <td><span class="badge {r.result}">{RESULT_LABEL[r.result]}</span></td>
          <td>{r.inspector}</td>
          <td class="muted">{r.note || '—'}</td>
        </tr>
      {:else}
        <tr><td colspan="6" class="muted">暂无巡查记录</td></tr>
      {/each}
    </tbody>
  </table>
</div>
