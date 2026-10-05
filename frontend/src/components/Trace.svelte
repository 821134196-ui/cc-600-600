<script>
  import { createEventDispatcher } from 'svelte'
  import { api, STATUS_LABEL, EVENT_LABEL, RESULT_LABEL, fmtDate, fmtDateTime } from '../api.js'

  const dispatch = createEventDispatcher()
  let code = ''
  let trail = null
  let searched = false

  async function search() {
    if (!code.trim()) return
    try {
      trail = await api.trail(code.trim())
      searched = true
    } catch (e) {
      trail = null
      searched = true
      dispatch('error', { message: e.message })
    }
  }
</script>

<div class="card">
  <h2>履历查询 — 沿编号追溯从领用到销毁的全部记录</h2>
  <div class="row">
    <div class="field" style="flex:1">
      <label>试用品编号</label>
      <input bind:value={code} placeholder="如 T-00006" on:keydown={(e) => e.key === 'Enter' && search()} />
    </div>
    <button class="primary" on:click={search}>查询</button>
  </div>
</div>

{#if trail}
  <div class="card">
    <h2>试用品 {trail.item.code}
      <span class="badge {trail.item.status}">{STATUS_LABEL[trail.item.status]}</span>
    </h2>
    <p>
      {trail.item.productName}（批次 {trail.item.batchNo}） ·
      入库 {fmtDate(trail.item.stockedAt)} ·
      开封 {fmtDate(trail.item.openedDate)} ·
      到期 {fmtDate(trail.item.expiryDate)} ·
      当前柜位 {trail.item.counterLocation || '—'}
    </p>
    {#if trail.destruction}
      <p>
        销毁批次 <strong>{trail.destruction.batchCode}</strong>：
        申报 {trail.destruction.declaredQuantity} 件 / 实际 {trail.destruction.actualQuantity} 件，
        原因「{trail.destruction.reason}」，经手人 {trail.destruction.handler}，
        {fmtDateTime(trail.destruction.destroyedAt)}
      </p>
    {/if}
  </div>

  <div class="card">
    <h2>流转时间线</h2>
    <div class="timeline">
      {#each trail.events as e}
        <div class="event">
          <div class="dot"></div>
          <div class="body">
            <div class="line1">
              <strong>{EVENT_LABEL[e.type] || e.type}</strong>
              <span class="muted">{fmtDateTime(e.eventAt)}</span>
              {#if e.operator}<span class="muted">· {e.operator}</span>{/if}
            </div>
            <div class="line2 muted">
              {#if e.fromLocation || e.toLocation}
                {e.fromLocation || '—'} → {e.toLocation || '—'} ·
              {/if}
              {e.note || ''}
            </div>
          </div>
        </div>
      {/each}
    </div>
  </div>

  {#if trail.inspections.length > 0}
    <div class="card">
      <h2>巡查记录</h2>
      <table>
        <thead><tr><th>时间</th><th>结论</th><th>巡查人</th><th>备注</th></tr></thead>
        <tbody>
          {#each trail.inspections as r}
            <tr>
              <td class="muted">{fmtDateTime(r.inspectedAt)}</td>
              <td><span class="badge {r.result}">{RESULT_LABEL[r.result]}</span></td>
              <td>{r.inspector}</td>
              <td class="muted">{r.note || '—'}</td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>
  {/if}
{:else if searched}
  <div class="card"><p class="muted">未找到该编号的试用品。</p></div>
{/if}

<style>
  .timeline { position: relative; padding-left: 22px; }
  .timeline::before {
    content: ''; position: absolute; left: 6px; top: 6px; bottom: 6px;
    width: 2px; background: #e1e4e8;
  }
  .event { position: relative; padding: 6px 0 10px; }
  .dot {
    position: absolute; left: -20px; top: 12px; width: 10px; height: 10px;
    border-radius: 50%; background: #1f6feb; border: 2px solid #fff;
    box-shadow: 0 0 0 2px #1f6feb33;
  }
  .line1 { display: flex; gap: 10px; align-items: baseline; font-size: 14px; }
  .line2 { font-size: 12px; margin-top: 2px; }
</style>
