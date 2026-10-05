<script>
  import { api, STATUS_LABEL, EVENT_LABEL, fmtDate, fmtTime } from '../api.js'
  import { showToast } from '../stores.js'

  let code = ''
  let item = null
  let events = []
  let searched = false

  async function search() {
    if (!code.trim()) return
    searched = true
    item = null
    events = []
    try {
      item = await api.tester(code.trim())
      events = await api.events(code.trim())
    } catch (e) {
      showToast(e.message, 'err')
    }
  }
</script>

<h2>追溯查询</h2>
<div class="sub">沿试用品独立编号，查询从入库、领用、调柜到销毁的全部记录</div>

<div class="card">
  <div class="filters">
    <input placeholder="输入试用品编号，如 TST-00001" bind:value={code}
           on:keydown={e => e.key === 'Enter' && search()} style="width:280px" />
    <button class="btn" on:click={search}>查询</button>
  </div>

  {#if searched && !item}
    <div class="empty">未找到该编号的试用品</div>
  {/if}
</div>

{#if item}
  <div class="card">
    <h3>试用品档案</h3>
    <div class="stat-grid">
      <div class="stat"><div class="lbl">编号</div><div class="mono" style="font-size:16px;font-weight:700">{item.itemCode}</div></div>
      <div class="stat"><div class="lbl">品名</div><div>{item.productName}</div></div>
      <div class="stat"><div class="lbl">批次</div><div class="mono">{item.batchNo}</div></div>
      <div class="stat"><div class="lbl">状态</div><div><span class="badge {item.status}">{STATUS_LABEL[item.status]}</span></div></div>
      <div class="stat"><div class="lbl">柜位</div><div>{item.counterLocation || '—'}</div></div>
      <div class="stat"><div class="lbl">开封日期</div><div>{fmtDate(item.openedDate)}</div></div>
      <div class="stat"><div class="lbl">效期截止</div><div>{fmtDate(item.expiryDate)}</div></div>
      <div class="stat">
        <div class="lbl">剩余天数</div>
        <div>
          {#if item.daysLeft === null}—
          {:else if item.daysLeft < 0}<span class="badge danger">已过期 {-item.daysLeft} 天</span>
          {:else}{item.daysLeft} 天{/if}
        </div>
      </div>
    </div>
    {#if item.recalled}
      <p><span class="badge danger">该批次已召回</span></p>
    {/if}
  </div>

  <div class="card">
    <h3>全链路记录（{events.length} 条）</h3>
    {#if events.length === 0}
      <div class="empty">暂无事件</div>
    {:else}
      <ul class="timeline">
        {#each events as e}
          <li>
            <span class="t-type">{EVENT_LABEL[e.eventType] || e.eventType}</span>
            <span class="t-time">{fmtTime(e.eventAt)}</span>
            <div class="t-detail">{e.detail || ''}</div>
            {#if e.operator}<div class="t-op">经手人：{e.operator}</div>{/if}
          </li>
        {/each}
      </ul>
    {/if}
  </div>
{/if}
