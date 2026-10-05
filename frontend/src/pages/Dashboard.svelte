<script>
  import { onMount } from 'svelte'
  import { api, STATUS_LABEL, fmtDate } from '../api.js'
  import { showToast } from '../stores.js'

  let stats = null
  let urgent = []
  let reconcile = null

  async function load() {
    try {
      ;[stats, reconcile] = await Promise.all([api.stats(), api.reconcile()])
      const all = await api.testers()
      // 待处理与临期实物优先展示（后端已按优先级排序）
      urgent = all.filter(t => t.priority <= 1).slice(0, 10)
    } catch (e) {
      showToast(e.message, 'err')
    }
  }

  async function enforce() {
    try {
      const r = await api.enforceExpiries()
      showToast(`巡检完成，${r.removed} 件过期样品已自动下架`)
      load()
    } catch (e) {
      showToast(e.message, 'err')
    }
  }

  onMount(load)
</script>

<h2>工作台</h2>
<div class="sub">未开封备品与已开封试用品分开统计，临期及待处理实物优先呈现</div>

{#if stats}
  <div class="stat-grid">
    <div class="stat"><div class="num">{stats.unopened}</div><div class="lbl">未开封备品</div></div>
    <div class="stat accent-green"><div class="num">{stats.inUse}</div><div class="lbl">已开封在用</div></div>
    <div class="stat accent-warn"><div class="num">{stats.nearExpiry}</div><div class="lbl">临期在用</div></div>
    <div class="stat accent-red"><div class="num">{stats.pendingDestruction}</div><div class="lbl">已下架待销毁</div></div>
    <div class="stat accent-frozen"><div class="num">{stats.frozen}</div><div class="lbl">冻结</div></div>
    <div class="stat"><div class="num">{stats.destroyed}</div><div class="lbl">已销毁</div></div>
    <div class="stat accent-red"><div class="num">{stats.recalledBatches}</div><div class="lbl">召回批次</div></div>
  </div>
{/if}

<div class="card">
  <h3>
    待处理与临期实物
    <button class="btn ghost sm" style="float:right" on:click={enforce}>立即巡检过期样品</button>
  </h3>
  {#if urgent.length === 0}
    <div class="empty">暂无待处理或临期试用品 🎉</div>
  {:else}
    <table>
      <thead>
        <tr>
          <th>编号</th><th>品名</th><th>批次</th><th>柜位</th>
          <th>开封日期</th><th>效期截止</th><th>剩余</th><th>状态</th><th>提示</th>
        </tr>
      </thead>
      <tbody>
        {#each urgent as t}
          <tr>
            <td class="mono">{t.itemCode}</td>
            <td>{t.productName}</td>
            <td class="mono">{t.batchNo}</td>
            <td>{t.counterLocation || '—'}</td>
            <td>{fmtDate(t.openedDate)}</td>
            <td>{fmtDate(t.expiryDate)}</td>
            <td>
              {#if t.daysLeft === null}—
              {:else if t.daysLeft < 0}<span class="badge danger">已过期 {-t.daysLeft} 天</span>
              {:else}<span class="badge warn">{t.daysLeft} 天</span>{/if}
            </td>
            <td><span class="badge {t.status}">{STATUS_LABEL[t.status]}</span></td>
            <td>
              {#if t.recalled}<span class="badge danger">召回</span>{/if}
              <span class="badge {t.priority === 0 ? 'danger' : 'warn'}">{t.priorityLabel}</span>
            </td>
          </tr>
        {/each}
      </tbody>
    </table>
  {/if}
</div>

{#if reconcile}
  <div class="card">
    <h3>销毁账实核对</h3>
    <p>
      已销毁样品 <b>{reconcile.destroyedItemCount}</b> 件，
      销毁记录 <b>{reconcile.destructionRecordCount}</b> 条，
      记录数量合计 <b>{reconcile.totalDestroyedQuantity}</b> 件 ——
      {#if reconcile.consistent}
        <span class="reconcile-ok">✓ 账实一致</span>
      {:else}
        <span class="reconcile-bad">✗ 账实不符，请立即核查</span>
      {/if}
    </p>
  </div>
{/if}
