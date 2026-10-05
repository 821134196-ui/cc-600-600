<script>
  import { onMount, createEventDispatcher } from 'svelte'
  import { api, REASON_LABEL, fmtDate, fmtDateTime } from '../api.js'

  const dispatch = createEventDispatcher()
  let removedItems = []
  let batches = []
  let reconcile = null
  let selected = new Set()
  let reason = ''
  let handler = ''

  onMount(load)

  async function load() {
    try {
      removedItems = await api.items({ status: 'REMOVED' })
      batches = await api.destructions()
      reconcile = await api.reconcile()
      selected = new Set()
    } catch (e) {
      dispatch('error', { message: e.message })
    }
  }

  function toggle(id) {
    selected.has(id) ? selected.delete(id) : selected.add(id)
    selected = selected
  }

  function toggleAll() {
    selected = selected.size === removedItems.length ? new Set() : new Set(removedItems.map(i => i.id))
  }

  async function submit() {
    try {
      const b = await api.destroy({ itemIds: [...selected], reason, handler })
      dispatch('done', {
        message: `销毁批次 ${b.batchCode} 已登记：申报 ${b.declaredQuantity} 件，实际处理 ${b.actualQuantity} 件`
      })
      reason = ''
    } catch (e) {
      dispatch('error', { message: e.message })
    }
  }
</script>

<div class="card">
  <h2>登记销毁（仅「待销毁」状态的试用品可销毁）</h2>
  {#if removedItems.length === 0}
    <p class="muted">当前没有待销毁的试用品。</p>
  {:else}
    <table>
      <thead>
        <tr>
          <th><input type="checkbox" checked={selected.size === removedItems.length} on:change={toggleAll} /></th>
          <th>编号</th><th>商品</th><th>批次</th><th>下架原因</th><th>下架时间</th><th>柜位</th>
        </tr>
      </thead>
      <tbody>
        {#each removedItems as i}
          <tr>
            <td><input type="checkbox" checked={selected.has(i.id)} on:change={() => toggle(i.id)} /></td>
            <td><strong>{i.code}</strong></td>
            <td>{i.productName}</td>
            <td class="muted">{i.batchNo}</td>
            <td><span class="badge {i.removedReason}">{REASON_LABEL[i.removedReason] || i.removedReason}</span></td>
            <td class="muted">{fmtDateTime(i.removedAt)}</td>
            <td>{i.counterLocation || '—'}</td>
          </tr>
        {/each}
        </tbody>
      </table>
    <div class="row" style="margin-top:12px">
      <div class="field"><label>销毁原因 *</label><input bind:value={reason} placeholder="如：到期集中销毁" style="width:220px" /></div>
      <div class="field"><label>经手人 *</label><input bind:value={handler} /></div>
      <button class="danger" on:click={submit} disabled={selected.size === 0 || !reason || !handler}>
        销毁选中的 {selected.size} 件
      </button>
    </div>
    <p class="muted">销毁为事务性操作：任何一件状态不符都会整体回滚；同一件样品不可能被重复销毁。</p>
  {/if}
</div>

<div class="card">
  <h2>
    销毁批次与数量核对
    {#if reconcile}
      {#if reconcile.ok}<span class="badge NORMAL">账实相符</span>
      {:else}<span class="badge EXPIRED">账实不符</span>{/if}
    {/if}
  </h2>
  <table>
    <thead><tr><th>批次号</th><th>申报数量</th><th>实际处理</th><th>库内已销毁件数</th><th>核对</th><th>原因</th><th>经手人</th><th>时间</th></tr></thead>
    <tbody>
      {#each batches as b}
        {@const rec = reconcile?.batches.find(r => r.batchCode === b.batchCode)}
        <tr>
          <td><strong>{b.batchCode}</strong></td>
          <td>{b.declaredQuantity}</td>
          <td>{b.actualQuantity}</td>
          <td>{rec ? rec.linkedDestroyedItems : '—'}</td>
          <td>{#if rec}{#if rec.ok}<span class="badge NORMAL">一致</span>{:else}<span class="badge EXPIRED">不一致</span>{/if}{/if}</td>
          <td>{b.reason}</td>
          <td>{b.handler}</td>
          <td class="muted">{fmtDateTime(b.destroyedAt)}</td>
        </tr>
      {:else}
        <tr><td colspan="8" class="muted">暂无销毁记录</td></tr>
      {/each}
    </tbody>
  </table>
  {#if reconcile}
    <p class="muted">
      合计：申报 {reconcile.totalDeclared} 件，实际处理 {reconcile.totalActual} 件，
      库内已销毁 {reconcile.totalDestroyedItems} 件，待销毁 {reconcile.pendingRemoval} 件。
    </p>
  {/if}
</div>
