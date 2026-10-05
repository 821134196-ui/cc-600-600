<script>
  import { onMount, createEventDispatcher } from 'svelte'
  import { api, STATUS_LABEL, CONDITION_LABEL, RESULT_LABEL, fmtDate } from '../api.js'
  import Modal from './Modal.svelte'

  const dispatch = createEventDispatcher()
  let items = []
  let filterStatus = ''
  let search = ''
  let action = null   // { type, item }
  let form = {}
  let today = new Date().toISOString().slice(0, 10)

  onMount(load)

  async function load() {
    try {
      items = await api.items({ status: filterStatus, q: search })
    } catch (e) {
      dispatch('error', { message: e.message })
    }
  }

  function open(type, item) {
    form = { openedDate: today, operator: '', toLocation: '', reason: '', result: 'NORMAL', inspector: '', note: '' }
    action = { type, item }
  }

  async function submit() {
    const { type, item } = action
    try {
      if (type === 'issue') {
        await api.issue(item.code, { openedDate: form.openedDate || null, counterLocation: form.toLocation, operator: form.operator })
        dispatch('done', { message: `${item.code} 已领用开封` })
      } else if (type === 'transfer') {
        await api.transfer(item.code, { toLocation: form.toLocation, operator: form.operator })
        dispatch('done', { message: `${item.code} 已调至 ${form.toLocation}，开封日期与效期不变` })
      } else if (type === 'freeze') {
        await api.freeze(item.code, { reason: form.reason, operator: form.operator })
        dispatch('done', { message: `${item.code} 已冻结` })
      } else if (type === 'remove') {
        await api.remove(item.code, { reason: form.reason || 'MANUAL', operator: form.operator })
        dispatch('done', { message: `${item.code} 已下架` })
      } else if (type === 'inspect') {
        await api.inspect(item.code, { result: form.result, inspector: form.inspector, note: form.note })
        dispatch('done', { message: `${item.code} 巡查已登记（${RESULT_LABEL[form.result]}）` })
      }
      action = null
    } catch (e) {
      dispatch('error', { message: e.message })
    }
  }

  async function unfreeze(item) {
    try {
      await api.unfreeze(item.code, {})
      dispatch('done', { message: `${item.code} 已解冻` })
    } catch (e) {
      dispatch('error', { message: e.message })
    }
  }

  const ACTION_TITLE = { issue: '领用开封', transfer: '调柜', freeze: '冻结', remove: '下架', inspect: '巡查登记' }
</script>

<div class="card">
  <h2>试用品台账 <span class="muted">（待销毁 → 已过期 → 临期 → 冻结 → 在用 → 备品 优先排序）</span></h2>
  <div class="row" style="margin-bottom:12px">
    <div class="field">
      <label>状态</label>
      <select bind:value={filterStatus} on:change={load}>
        <option value="">全部</option>
        {#each Object.entries(STATUS_LABEL) as [k, v]}<option value={k}>{v}</option>{/each}
      </select>
    </div>
    <div class="field" style="flex:1">
      <label>搜索（编号 / 商品 / 批次）</label>
      <input bind:value={search} on:input={load} placeholder="输入关键字…" />
    </div>
  </div>

  <table>
    <thead>
      <tr><th>编号</th><th>商品</th><th>批次</th><th>状态</th><th>卫生状态</th>
          <th>开封日期</th><th>到期日</th><th>剩余</th><th>柜位</th><th>操作</th></tr>
    </thead>
    <tbody>
      {#each items as i}
        <tr>
          <td><strong>{i.code}</strong></td>
          <td>{i.productName}</td>
          <td class="muted">{i.batchNo}</td>
          <td><span class="badge {i.status}">{STATUS_LABEL[i.status]}</span></td>
          <td>
            {#if i.condition !== 'NORMAL'}
              <span class="badge {i.condition}">{CONDITION_LABEL[i.condition]}</span>
            {:else}<span class="muted">正常</span>{/if}
            {#if i.status === 'REMOVED' && i.removedReason}
              <span class="badge {i.removedReason}">{i.removedReason}</span>
            {/if}
          </td>
          <td>{fmtDate(i.openedDate)}</td>
          <td>{fmtDate(i.expiryDate)}</td>
          <td>
            {#if i.daysToExpiry != null}
              <span class:urgent={i.daysToExpiry < 0} class:warn={i.daysToExpiry >= 0 && i.daysToExpiry <= 7}>
                {i.daysToExpiry < 0 ? `超 ${-i.daysToExpiry} 天` : `${i.daysToExpiry} 天`}
              </span>
            {:else}—{/if}
          </td>
          <td>{i.counterLocation || '—'}</td>
          <td style="white-space:nowrap">
            {#if i.status === 'SEALED'}
              <button class="sm primary" on:click={() => open('issue', i)}>领用</button>
              <button class="sm" on:click={() => open('transfer', i)}>调柜</button>
              <button class="sm" on:click={() => open('freeze', i)}>冻结</button>
            {:else if i.status === 'IN_USE'}
              <button class="sm" on:click={() => open('transfer', i)}>调柜</button>
              <button class="sm" on:click={() => open('inspect', i)}>巡查</button>
              <button class="sm" on:click={() => open('freeze', i)}>冻结</button>
              <button class="sm danger" on:click={() => open('remove', i)}>下架</button>
            {:else if i.status === 'FROZEN'}
              <button class="sm" on:click={() => unfreeze(i)}>解冻</button>
              <button class="sm danger" on:click={() => open('remove', i)}>下架</button>
            {/if}
          </td>
        </tr>
      {:else}
        <tr><td colspan="10" class="muted">暂无数据</td></tr>
      {/each}
    </tbody>
  </table>
</div>

{#if action}
  <Modal title="{ACTION_TITLE[action.type]} — {action.item.code}（{action.item.productName}）" on:close={() => (action = null)}>
    {#if action.type === 'issue'}
      <p class="muted">登记开封日期与柜位，到期日 = 开封日期 + 开封后使用月数，自动计算。</p>
      <div class="field"><label>开封日期</label><input type="date" bind:value={form.openedDate} /></div>
      <div class="field"><label>柜位 *</label><input bind:value={form.toLocation} placeholder="如 A-01" /></div>
      <div class="field"><label>经手人</label><input bind:value={form.operator} /></div>
    {:else if action.type === 'transfer'}
      <p class="muted">调柜仅改变柜位，原开封时间 <strong>{action.item.openedDate || '未开封'}</strong> 与效期继续沿用。</p>
      <div class="field"><label>目标柜位 *</label><input bind:value={form.toLocation} placeholder="如 B-02" /></div>
      <div class="field"><label>经手人</label><input bind:value={form.operator} /></div>
    {:else if action.type === 'freeze'}
      <p class="muted">冻结期间不得再次领用，解冻后恢复原状态。</p>
      <div class="field"><label>冻结原因</label><input bind:value={form.reason} placeholder="如：疑似污染待复检" /></div>
      <div class="field"><label>经手人</label><input bind:value={form.operator} /></div>
    {:else if action.type === 'remove'}
      <p class="muted">下架后进入「待销毁」，请在销毁管理中登记销毁。</p>
      <div class="field">
        <label>下架原因</label>
        <select bind:value={form.reason}>
          <option value="MANUAL">手动下架</option>
          <option value="EXPIRED">到期</option>
          <option value="CONTAMINATED">污染</option>
          <option value="RECALLED">召回</option>
        </select>
      </div>
      <div class="field"><label>经手人</label><input bind:value={form.operator} /></div>
    {:else if action.type === 'inspect'}
      <p class="muted">污染或到期的试用品将立即自动下架。</p>
      <div class="field">
        <label>巡查结论</label>
        <select bind:value={form.result}>
          {#each Object.entries(RESULT_LABEL) as [k, v]}<option value={k}>{v}</option>{/each}
        </select>
      </div>
      <div class="field"><label>巡查人 *</label><input bind:value={form.inspector} /></div>
      <div class="field"><label>备注</label><input bind:value={form.note} /></div>
    {/if}
    <div style="margin-top:14px; text-align:right">
      <button on:click={() => (action = null)}>取消</button>
      <button class="primary" on:click={submit}>确认</button>
    </div>
  </Modal>
{/if}

<style>
  .field { margin-bottom: 10px; }
</style>
