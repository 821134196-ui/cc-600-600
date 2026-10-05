<script>
  import { onMount } from 'svelte'
  import { api, STATUS_LABEL, fmtDate } from '../api.js'
  import { showToast } from '../stores.js'

  let items = []
  let counters = []
  let filterStatus = ''
  let filterCounter = ''
  let keyword = ''
  let loading = false

  // 弹窗状态：{ mode: 'transfer'|'freeze'|'remove'|'patrol', item }
  let modal = null
  let form = {}

  async function load() {
    loading = true
    try {
      ;[items, counters] = await Promise.all([
        api.testers({ status: filterStatus, counter: filterCounter, q: keyword }),
        api.counters()
      ])
    } catch (e) {
      showToast(e.message, 'err')
    } finally {
      loading = false
    }
  }

  function openModal(mode, item) {
    form = { operator: '', reason: '', counterLocation: '', condition: 'NORMAL', note: '', inspector: '', patrolDate: today() }
    modal = { mode, item }
  }

  function today() {
    return new Date().toISOString().slice(0, 10)
  }

  async function submit() {
    const { mode, item } = modal
    try {
      if (mode === 'transfer') {
        await api.transfer(item.itemCode, { counterLocation: form.counterLocation, operator: form.operator })
        showToast(`${item.itemCode} 已调柜至 ${form.counterLocation}，开封日期与效期保持不变`)
      } else if (mode === 'freeze') {
        await api.freeze(item.itemCode, { reason: form.reason, operator: form.operator })
        showToast(`${item.itemCode} 已冻结，不得再次领用`)
      } else if (mode === 'remove') {
        await api.remove(item.itemCode, { reason: form.reason, operator: form.operator })
        showToast(`${item.itemCode} 已下架`)
      } else if (mode === 'patrol') {
        await api.patrol(item.itemCode, {
          condition: form.condition, note: form.note,
          inspector: form.inspector, patrolDate: form.patrolDate
        })
        showToast(`${item.itemCode} 巡查已登记`)
      }
      modal = null
      load()
    } catch (e) {
      showToast(e.message, 'err')
    }
  }

  onMount(load)
</script>

<h2>试用品管理</h2>
<div class="sub">列表按优先级排序：待处理实物 &gt; 临期 &gt; 冻结 &gt; 正常在用 &gt; 未开封备品 &gt; 已销毁</div>

<div class="card">
  <div class="filters">
    <input placeholder="搜索编号 / 品名 / 批次" bind:value={keyword} on:input={load} style="width:220px" />
    <select bind:value={filterStatus} on:change={load}>
      <option value="">全部状态</option>
      {#each Object.entries(STATUS_LABEL) as [k, v]}
        <option value={k}>{v}</option>
      {/each}
    </select>
    <select bind:value={filterCounter} on:change={load}>
      <option value="">全部柜位</option>
      {#each counters as c}
        <option value={c}>{c}</option>
      {/each}
    </select>
    <button class="btn ghost sm" on:click={load}>刷新</button>
    <span style="color:var(--muted);font-size:12.5px">{loading ? '加载中…' : `共 ${items.length} 件`}</span>
  </div>

  <table>
    <thead>
      <tr>
        <th>编号</th><th>品名</th><th>批次</th><th>柜位</th>
        <th>开封日期</th><th>效期截止</th><th>剩余</th><th>状态</th><th>提示</th><th>操作</th>
      </tr>
    </thead>
    <tbody>
      {#each items as t}
        <tr>
          <td class="mono">{t.itemCode}</td>
          <td>{t.productName}</td>
          <td class="mono">{t.batchNo}</td>
          <td>{t.counterLocation || '—'}</td>
          <td>{fmtDate(t.openedDate)}</td>
          <td>{fmtDate(t.expiryDate)}</td>
          <td>
            {#if t.daysLeft === null}—
            {:else if t.daysLeft < 0}<span class="badge danger">过期 {-t.daysLeft} 天</span>
            {:else if t.nearExpiry}<span class="badge warn">{t.daysLeft} 天</span>
            {:else}{t.daysLeft} 天{/if}
          </td>
          <td><span class="badge {t.status}">{STATUS_LABEL[t.status]}</span></td>
          <td>
            {#if t.recalled}<span class="badge danger">召回</span>{/if}
            {#if t.priorityLabel && t.priority <= 1}
              <span class="badge {t.priority === 0 ? 'danger' : 'warn'}">{t.priorityLabel}</span>
            {/if}
          </td>
          <td>
            <div class="row-actions">
              {#if t.status === 'IN_USE'}
                <button class="btn ghost sm" on:click={() => openModal('transfer', t)}>调柜</button>
                <button class="btn ghost sm" on:click={() => openModal('patrol', t)}>巡查</button>
                <button class="btn ghost sm" on:click={() => openModal('freeze', t)}>冻结</button>
                <button class="btn ghost sm" on:click={() => openModal('remove', t)}>下架</button>
              {:else if t.status === 'FROZEN'}
                <button class="btn ghost sm" on:click={() => openModal('remove', t)}>下架</button>
              {/if}
            </div>
          </td>
        </tr>
      {:else}
        <tr><td colspan="10" class="empty">暂无数据</td></tr>
      {/each}
    </tbody>
  </table>
</div>

{#if modal}
  <div class="modal-mask" on:click|self={() => (modal = null)}>
    <div class="modal">
      {#if modal.mode === 'transfer'}
        <h3>调柜 — {modal.item.itemCode}</h3>
        <p style="color:var(--muted);font-size:13px">
          当前柜位 <b>{modal.item.counterLocation}</b>；开封日期 <b>{modal.item.openedDate}</b>，
          效期至 <b>{modal.item.expiryDate}</b>。调柜仅变更位置，开封日期与效期保持不变。
        </p>
        <div class="field"><label>新柜位</label><input bind:value={form.counterLocation} placeholder="如 B-03" /></div>
        <div class="field" style="margin-top:10px"><label>经手人</label><input bind:value={form.operator} /></div>
      {:else if modal.mode === 'freeze'}
        <h3>冻结 — {modal.item.itemCode}</h3>
        <p style="color:var(--muted);font-size:13px">冻结后该试用品不得再次领用。</p>
        <div class="field"><label>冻结原因</label><input bind:value={form.reason} /></div>
        <div class="field" style="margin-top:10px"><label>经手人</label><input bind:value={form.operator} /></div>
      {:else if modal.mode === 'remove'}
        <h3>下架 — {modal.item.itemCode}</h3>
        <div class="field"><label>下架原因</label><input bind:value={form.reason} /></div>
        <div class="field" style="margin-top:10px"><label>经手人</label><input bind:value={form.operator} /></div>
      {:else if modal.mode === 'patrol'}
        <h3>巡查登记 — {modal.item.itemCode}</h3>
        <div class="field">
          <label>巡查结论</label>
          <select bind:value={form.condition}>
            <option value="NORMAL">正常</option>
            <option value="CONTAMINATED">污染（登记后立即下架）</option>
            <option value="NEAR_EXPIRY">临期</option>
            <option value="EXPIRED">到期（登记后立即下架）</option>
          </select>
        </div>
        <div class="field" style="margin-top:10px"><label>备注</label><input bind:value={form.note} /></div>
        <div class="field" style="margin-top:10px"><label>巡查人</label><input bind:value={form.inspector} /></div>
        <div class="field" style="margin-top:10px"><label>巡查日期</label><input type="date" bind:value={form.patrolDate} /></div>
      {/if}
      <div class="actions">
        <button class="btn ghost" on:click={() => (modal = null)}>取消</button>
        <button class="btn" on:click={submit}>确认</button>
      </div>
    </div>
  </div>
{/if}
