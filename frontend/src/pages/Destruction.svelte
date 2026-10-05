<script>
  import { onMount } from 'svelte'
  import { api, STATUS_LABEL, fmtTime } from '../api.js'
  import { showToast } from '../stores.js'

  let pending = []
  let records = []
  let reconcile = null
  let selected = new Set()
  let reason = ''
  let handler = ''

  async function load() {
    try {
      ;[pending, records, reconcile] = await Promise.all([
        api.pendingDestruction(),
        api.destructions(),
        api.reconcile()
      ])
      selected = new Set([...selected].filter(c => pending.some(p => p.itemCode === c)))
    } catch (e) {
      showToast(e.message, 'err')
    }
  }

  function toggle(code) {
    if (selected.has(code)) selected.delete(code)
    else selected.add(code)
    selected = selected
  }

  function toggleAll() {
    if (selected.size === pending.length) selected = new Set()
    else selected = new Set(pending.map(p => p.itemCode))
  }

  async function destroy() {
    if (selected.size === 0) return
    if (!reason.trim() || !handler.trim()) {
      showToast('请填写销毁原因与经手人', 'err')
      return
    }
    if (!confirm(`确认销毁 ${selected.size} 件试用品？该操作不可撤销。`)) return
    try {
      const r = await api.destroy({
        itemCodes: [...selected],
        declaredQuantity: selected.size, // 登记数量 = 实际勾选件数，后端将再次核对
        reason,
        handler
      })
      showToast(`${r.message}（销毁批次 ${r.groupNo}）`)
      selected = new Set()
      reason = ''
      load()
    } catch (e) {
      showToast(e.message, 'err')
    }
  }

  onMount(load)
</script>

<h2>销毁管理</h2>
<div class="sub">销毁数量须与实际在库样品逐件核对；每件样品仅允许销毁一次，全程留痕</div>

<div class="card">
  <h3>待销毁实物（已下架 / 冻结，共 {pending.length} 件）</h3>
  {#if pending.length === 0}
    <div class="empty">暂无待销毁实物</div>
  {:else}
    <table>
      <thead>
        <tr>
          <th><input type="checkbox" checked={selected.size === pending.length && pending.length > 0} on:change={toggleAll} /></th>
          <th>编号</th><th>品名</th><th>批次</th><th>状态</th><th>原柜位</th>
        </tr>
      </thead>
      <tbody>
        {#each pending as p}
          <tr>
            <td><input type="checkbox" checked={selected.has(p.itemCode)} on:change={() => toggle(p.itemCode)} /></td>
            <td class="mono">{p.itemCode}</td>
            <td>{p.productName}</td>
            <td class="mono">{p.batchNo}</td>
            <td><span class="badge {p.status}">{STATUS_LABEL[p.status]}</span></td>
            <td>{p.counterLocation || '—'}</td>
          </tr>
        {/each}
      </tbody>
    </table>

    <div class="form-grid" style="margin-top:16px">
      <div class="field"><label>销毁原因 *</label><input bind:value={reason} placeholder="如：超过卫生效期 / 污染" /></div>
      <div class="field"><label>经手人 *</label><input bind:value={handler} /></div>
      <div class="field">
        <label>销毁数量（与勾选件数自动核对）</label>
        <input value={selected.size + ' 件'} disabled />
      </div>
    </div>
    <div style="margin-top:14px">
      <button class="btn danger" disabled={selected.size === 0} on:click={destroy}>
        销毁选中 {selected.size} 件
      </button>
    </div>
  {/if}
</div>

{#if reconcile}
  <div class="card">
    <h3>账实核对</h3>
    <p>
      已销毁样品 <b>{reconcile.destroyedItemCount}</b> 件 ＝
      销毁记录 <b>{reconcile.destructionRecordCount}</b> 条 ＝
      记录数量合计 <b>{reconcile.totalDestroyedQuantity}</b> 件
      {#if reconcile.consistent}
        <span class="reconcile-ok">✓ 账实一致</span>
      {:else}
        <span class="reconcile-bad">✗ 账实不符，请立即核查</span>
      {/if}
    </p>
  </div>
{/if}

<div class="card">
  <h3>销毁记录（{records.length}）</h3>
  <table>
    <thead>
      <tr><th>销毁时间</th><th>编号</th><th>品名</th><th>批次</th><th>销毁批次号</th><th>数量</th><th>原因</th><th>经手人</th></tr>
    </thead>
    <tbody>
      {#each records as r}
        <tr>
          <td class="mono">{fmtTime(r.destroyedAt)}</td>
          <td class="mono">{r.itemCode}</td>
          <td>{r.productName}</td>
          <td class="mono">{r.batchNo}</td>
          <td class="mono">{r.groupNo}</td>
          <td>{r.quantity}</td>
          <td>{r.reason}</td>
          <td>{r.handler}</td>
        </tr>
      {:else}
        <tr><td colspan="8" class="empty">暂无销毁记录</td></tr>
      {/each}
    </tbody>
  </table>
</div>
