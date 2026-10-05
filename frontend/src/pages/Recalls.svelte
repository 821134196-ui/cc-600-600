<script>
  import { onMount } from 'svelte'
  import { api, fmtDate, fmtTime } from '../api.js'
  import { showToast } from '../stores.js'

  let notices = []
  let syncing = false
  let lastResult = null

  async function load() {
    try {
      notices = await api.recalls()
    } catch (e) {
      showToast(e.message, 'err')
    }
  }

  async function sync() {
    syncing = true
    try {
      const r = await api.syncRecalls()
      lastResult = r
      showToast(`同步完成：新增 ${r.newNotices} 条召回通知，${r.itemsRemoved} 件样品下架，${r.stockFrozen} 件备品冻结`)
      load()
    } catch (e) {
      showToast(e.message, 'err')
    } finally {
      syncing = false
    }
  }

  onMount(load)
</script>

<h2>召回中心</h2>
<div class="sub">召回通知由本地模拟接口（/mock-api/recall-notices）提供；同步后涉及批次的在用样品立即下架、备品冻结</div>

<div class="card">
  <h3>
    召回通知同步
    <button class="btn sm" style="float:right" disabled={syncing} on:click={sync}>
      {syncing ? '同步中…' : '从模拟接口同步'}
    </button>
  </h3>
  {#if lastResult}
    <p style="color:var(--muted)">
      本次拉取 {lastResult.fetched} 条，新增 {lastResult.newNotices} 条；
      标记召回批次 {lastResult.batchesMarked} 个，
      在用样品下架 {lastResult.itemsRemoved} 件，
      备品冻结 {lastResult.stockFrozen} 件。
    </p>
  {:else}
    <p style="color:var(--muted)">点击右上角按钮，从本地模拟接口拉取最新召回通知。</p>
  {/if}
</div>

<div class="card">
  <h3>已同步召回通知（{notices.length}）</h3>
  <table>
    <thead>
      <tr><th>通知编号</th><th>批次号</th><th>品名</th><th>召回原因</th><th>通知日期</th><th>来源</th><th>同步时间</th></tr>
    </thead>
    <tbody>
      {#each notices as n}
        <tr>
          <td class="mono">{n.noticeNo}</td>
          <td class="mono">{n.batchNo}</td>
          <td>{n.productName || '—'}</td>
          <td>{n.reason || '—'}</td>
          <td>{fmtDate(n.noticeDate)}</td>
          <td>{n.source || '—'}</td>
          <td class="mono">{fmtTime(n.syncedAt)}</td>
        </tr>
      {:else}
        <tr><td colspan="7" class="empty">暂无召回通知</td></tr>
      {/each}
    </tbody>
  </table>
</div>
