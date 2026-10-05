<script>
  import { onMount } from 'svelte'
  import Dashboard from './components/Dashboard.svelte'
  import Items from './components/Items.svelte'
  import Receive from './components/Receive.svelte'
  import Inspections from './components/Inspections.svelte'
  import Recalls from './components/Recalls.svelte'
  import Destructions from './components/Destructions.svelte'
  import Trace from './components/Trace.svelte'

  const tabs = [
    { id: 'dashboard', label: '仪表盘', comp: Dashboard },
    { id: 'items', label: '试用品台账', comp: Items },
    { id: 'receive', label: '入库与领用', comp: Receive },
    { id: 'inspections', label: '巡查记录', comp: Inspections },
    { id: 'recalls', label: '召回通知', comp: Recalls },
    { id: 'destructions', label: '销毁管理', comp: Destructions },
    { id: 'trace', label: '履历查询', comp: Trace }
  ]

  let current = 'dashboard'
  let toast = null
  let toastTimer

  onMount(() => {
    const h = location.hash.replace('#/', '')
    if (tabs.some(t => t.id === h)) current = h
  })

  function select(id) {
    current = id
    location.hash = '#/' + id
  }

  function notify(message, isError = false) {
    clearTimeout(toastTimer)
    toast = { message, isError }
    toastTimer = setTimeout(() => (toast = null), 4000)
  }

  // 供子组件调用：操作成功后提示并广播刷新
  function handleDone(e) {
    notify(e.detail.message || '操作成功')
    refreshKey++
  }

  function handleError(e) {
    notify(e.detail.message || '操作失败', true)
  }

  let refreshKey = 0
</script>

<header>
  <div class="brand">
    <span class="logo">🧴</span>
    <div>
      <h1>商场试用品开封与卫生效期管理系统</h1>
      <p>化妆品柜台 · 按实际开封日期管理使用期限</p>
    </div>
  </div>
  <nav>
    {#each tabs as t}
      <button class:active={current === t.id} on:click={() => select(t.id)}>{t.label}</button>
    {/each}
  </nav>
</header>

<main>
  {#key refreshKey}
    <svelte:component this={tabs.find(t => t.id === current).comp}
      on:done={handleDone} on:error={handleError} on:notify={handleDone} />
  {/key}
</main>

{#if toast}
  <div class="toast" class:error={toast.isError}>{toast.message}</div>
{/if}

<style>
  :global(*) { box-sizing: border-box; }
  :global(body) {
    margin: 0;
    font-family: 'PingFang SC', 'Microsoft YaHei', 'Noto Sans CJK SC', system-ui, sans-serif;
    background: #f4f5f7;
    color: #24292f;
  }
  :global(h1) { font-size: 17px; margin: 0; }
  :global(table) { width: 100%; border-collapse: collapse; font-size: 13px; }
  :global(th) {
    text-align: left; padding: 8px 10px; background: #fafbfc;
    border-bottom: 2px solid #e1e4e8; font-weight: 600; white-space: nowrap;
  }
  :global(td) { padding: 8px 10px; border-bottom: 1px solid #eef1f4; vertical-align: middle; }
  :global(tr:hover td) { background: #f8f9fb; }
  :global(input, select) {
    padding: 7px 9px; border: 1px solid #d0d7de; border-radius: 6px;
    font-size: 13px; font-family: inherit; background: #fff;
  }
  :global(input:focus, select:focus) { outline: 2px solid #0969da33; border-color: #0969da; }
  :global(button) {
    padding: 7px 14px; border: 1px solid #d0d7de; border-radius: 6px;
    background: #fff; font-size: 13px; cursor: pointer; font-family: inherit;
  }
  :global(button:hover) { background: #f3f4f6; }
  :global(button.primary) { background: #1f6feb; border-color: #1f6feb; color: #fff; }
  :global(button.primary:hover) { background: #1a5fd0; }
  :global(button.danger) { background: #cf222e; border-color: #cf222e; color: #fff; }
  :global(button.sm) { padding: 3px 8px; font-size: 12px; margin-right: 4px; }
  :global(.card) {
    background: #fff; border: 1px solid #e1e4e8; border-radius: 10px;
    padding: 16px 18px; margin-bottom: 16px;
  }
  :global(.card h2) { font-size: 15px; margin: 0 0 12px; }
  :global(.badge) {
    display: inline-block; padding: 2px 8px; border-radius: 10px;
    font-size: 12px; font-weight: 500; white-space: nowrap;
  }
  :global(.badge.SEALED) { background: #ddf4e4; color: #1a7f37; }
  :global(.badge.IN_USE) { background: #ddf0ff; color: #0969da; }
  :global(.badge.FROZEN) { background: #e8ebed; color: #57606a; }
  :global(.badge.REMOVED) { background: #ffe8d9; color: #bc4c00; }
  :global(.badge.DESTROYED) { background: #f0f1f3; color: #8b949e; }
  :global(.badge.NEAR_EXPIRY) { background: #fff3c2; color: #9a6700; }
  :global(.badge.EXPIRED, .badge.RECALLED, .badge.CONTAMINATED) { background: #ffd8d3; color: #cf222e; }
  :global(.badge.NORMAL) { background: #ddf4e4; color: #1a7f37; }
  :global(.row) { display: flex; gap: 10px; flex-wrap: wrap; align-items: flex-end; }
  :global(.field) { display: flex; flex-direction: column; gap: 4px; }
  :global(.field label) { font-size: 12px; color: #57606a; }
  :global(.muted) { color: #8b949e; font-size: 12px; }
  :global(.urgent) { color: #cf222e; font-weight: 600; }
  :global(.warn) { color: #9a6700; font-weight: 600; }

  header {
    background: #fff; border-bottom: 1px solid #e1e4e8;
    padding: 14px 24px 0; position: sticky; top: 0; z-index: 10;
  }
  .brand { display: flex; gap: 12px; align-items: center; margin-bottom: 12px; }
  .logo { font-size: 28px; }
  .brand p { margin: 2px 0 0; font-size: 12px; color: #8b949e; }
  nav { display: flex; gap: 2px; flex-wrap: wrap; }
  nav button {
    border: none; border-radius: 8px 8px 0 0; padding: 9px 16px;
    font-size: 14px; background: transparent; color: #57606a;
  }
  nav button:hover { background: #f3f4f6; }
  nav button.active {
    background: #f4f5f7; color: #1f6feb; font-weight: 600;
    box-shadow: inset 0 -2px 0 #1f6feb;
  }
  main { max-width: 1280px; margin: 0 auto; padding: 20px 24px 60px; }
  .toast {
    position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%);
    background: #1a7f37; color: #fff; padding: 10px 22px; border-radius: 8px;
    font-size: 14px; box-shadow: 0 4px 16px rgba(0,0,0,.18); z-index: 100;
  }
  .toast.error { background: #cf222e; }
</style>
