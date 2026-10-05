<script>
  import { onMount, createEventDispatcher } from 'svelte'
  import { api, fmtDate, fmtDateTime } from '../api.js'

  const dispatch = createEventDispatcher()
  let recalls = []
  let lastSync = null

  onMount(load)

  async function load() {
    try {
      recalls = await api.recalls()
    } catch (e) {
      dispatch('error', { message: e.message })
    }
  }

  async function sync() {
    try {
      lastSync = await api.syncRecalls()
      dispatch('done', {
        message: `同步完成：收到 ${lastSync.noticesReceived} 条通知，新增 ${lastSync.noticesCreated} 条，${lastSync.itemsFlagged} 件样品已下架`
      })
    } catch (e) {
      dispatch('error', { message: e.message })
    }
  }
</script>

<div class="card">
  <h2>
    召回通知
    <button class="primary sm" style="float:right" on:click={sync}>从本地模拟接口同步</button>
  </h2>
  <p class="muted">
    通知由本地模拟接口 <code>GET /mock-api/recalls</code> 提供；同步后涉及批次的全部样品（备品/在用/冻结）立即下架。
    {#if lastSync}
      最近同步：收到 {lastSync.noticesReceived} 条，新增 {lastSync.noticesCreated} 条，下架 {lastSync.itemsFlagged} 件。
    {/if}
  </p>
  <table>
    <thead><tr><th>通知编号</th><th>涉及批次</th><th>商品</th><th>原因</th><th>发布日期</th><th>来源</th><th>同步时间</th></tr></thead>
    <tbody>
      {#each recalls as r}
        <tr>
          <td><strong>{r.noticeCode}</strong></td>
          <td><span class="badge RECALLED">{r.batchNo}</span></td>
          <td>{r.productName}</td>
          <td>{r.reason}</td>
          <td>{fmtDate(r.publishedAt)}</td>
          <td class="muted">{r.source}</td>
          <td class="muted">{fmtDateTime(r.syncedAt)}</td>
        </tr>
      {:else}
        <tr><td colspan="7" class="muted">尚未同步召回通知，点击右上角「同步」。</td></tr>
      {/each}
    </tbody>
  </table>
</div>
