<script>
  import { onMount, createEventDispatcher } from 'svelte'
  import { api, STATUS_LABEL, CONDITION_LABEL, fmtDate } from '../api.js'

  const dispatch = createEventDispatcher()
  let stats = null
  let pending = []

  onMount(load)

  async function load() {
    try {
      stats = await api.stats()
      const items = await api.items()
      // 待处理实物：待销毁 + 已过期 + 临期，按优先级取前 8 条
      pending = items.filter(i =>
        i.status === 'REMOVED' || i.condition === 'EXPIRED' || i.condition === 'NEAR_EXPIRY'
      ).slice(0, 8)
    } catch (e) {
      dispatch('error', { message: e.message })
    }
  }

  async function scan() {
    try {
      const r = await api.scanExpired()
      dispatch('done', { message: `效期扫描完成，${r.removed} 件过期试用品已下架` })
    } catch (e) {
      dispatch('error', { message: e.message })
    }
  }
</script>

{#if stats}
  <div class="grid">
    <div class="stat"><div class="num sealed">{stats.sealed}</div><div class="lbl">未开封备品</div></div>
    <div class="stat"><div class="num inuse">{stats.inUse}</div><div class="lbl">已开封在用</div></div>
    <div class="stat"><div class="num near">{stats.nearExpiry}</div><div class="lbl">临期（7天内）</div></div>
    <div class="stat"><div class="num expired">{stats.expiredInUse}</div><div class="lbl">在用但已过期</div></div>
    <div class="stat"><div class="num removed">{stats.removedPending}</div><div class="lbl">待销毁</div></div>
    <div class="stat"><div class="num frozen">{stats.frozen}</div><div class="lbl">冻结</div></div>
    <div class="stat"><div class="num destroyed">{stats.destroyed}</div><div class="lbl">已销毁</div></div>
    <div class="stat"><div class="num recall">{stats.recallNotices}</div><div class="lbl">召回通知</div></div>
  </div>

  <div class="card">
    <h2>
      待处理实物（临期 / 过期 / 待销毁优先）
      <button class="sm" style="float:right" on:click={scan}>立即效期扫描</button>
    </h2>
    {#if pending.length === 0}
      <p class="muted">当前没有待处理实物 🎉</p>
    {:else}
      <table>
        <thead>
          <tr><th>编号</th><th>商品</th><th>批次</th><th>状态</th><th>卫生状态</th>
              <th>开封日期</th><th>到期日</th><th>剩余天数</th><th>柜位</th></tr>
        </thead>
        <tbody>
          {#each pending as i}
            <tr>
              <td><strong>{i.code}</strong></td>
              <td>{i.productName}</td>
              <td class="muted">{i.batchNo}</td>
              <td><span class="badge {i.status}">{STATUS_LABEL[i.status]}</span></td>
              <td>
                {#if i.condition !== 'NORMAL'}
                  <span class="badge {i.condition}">{CONDITION_LABEL[i.condition]}</span>
                {:else}<span class="muted">正常</span>{/if}
              </td>
              <td>{fmtDate(i.openedDate)}</td>
              <td>{fmtDate(i.expiryDate)}</td>
              <td>
                {#if i.daysToExpiry != null}
                  <span class:urgent={i.daysToExpiry < 0} class:warn={i.daysToExpiry >= 0 && i.daysToExpiry <= 7}>
                    {i.daysToExpiry < 0 ? `已超期 ${-i.daysToExpiry} 天` : `${i.daysToExpiry} 天`}
                  </span>
                {:else}—{/if}
              </td>
              <td>{i.counterLocation || '—'}</td>
            </tr>
          {/each}
        </tbody>
      </table>
    {/if}
  </div>

  <div class="card">
    <h2>销毁核对</h2>
    <p>
      销毁批次 <strong>{stats.destructionBatches}</strong> 个，
      申报销毁 <strong>{stats.destroyedDeclared}</strong> 件，
      实际处理 <strong>{stats.destroyedActual}</strong> 件，
      库内已销毁 <strong>{stats.destroyed}</strong> 件。
      {#if stats.destroyedActual === stats.destroyed}
        <span class="badge NORMAL">账实相符</span>
      {:else}
        <span class="badge EXPIRED">账实不符，请到「销毁管理」核对</span>
      {/if}
    </p>
  </div>
{:else}
  <p class="muted">加载中…</p>
{/if}

<style>
  .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)); gap: 10px; margin-bottom: 16px; }
  .stat { background: #fff; border: 1px solid #e1e4e8; border-radius: 10px; padding: 14px; text-align: center; }
  .num { font-size: 26px; font-weight: 700; }
  .lbl { font-size: 12px; color: #57606a; margin-top: 4px; }
  .sealed { color: #1a7f37; } .inuse { color: #0969da; } .near { color: #9a6700; }
  .expired, .recall { color: #cf222e; } .removed { color: #bc4c00; }
  .frozen { color: #57606a; } .destroyed { color: #8b949e; }
</style>
