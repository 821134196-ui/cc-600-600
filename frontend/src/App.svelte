<script>
  import { toast } from './stores.js'
  import Dashboard from './pages/Dashboard.svelte'
  import Testers from './pages/Testers.svelte'
  import Issue from './pages/Issue.svelte'
  import Batches from './pages/Batches.svelte'
  import Patrol from './pages/Patrol.svelte'
  import Destruction from './pages/Destruction.svelte'
  import Recalls from './pages/Recalls.svelte'
  import Trace from './pages/Trace.svelte'

  const pages = [
    { id: 'dashboard', label: '工作台', comp: Dashboard },
    { id: 'testers', label: '试用品管理', comp: Testers },
    { id: 'issue', label: '入库与领用', comp: Issue },
    { id: 'batches', label: '批次管理', comp: Batches },
    { id: 'patrol', label: '巡查记录', comp: Patrol },
    { id: 'destruction', label: '销毁管理', comp: Destruction },
    { id: 'recalls', label: '召回中心', comp: Recalls },
    { id: 'trace', label: '追溯查询', comp: Trace }
  ]

  let current = location.hash.replace('#', '') || 'dashboard'
  $: active = pages.find(p => p.id === current) || pages[0]

  function nav(id) {
    current = id
    location.hash = id
  }
</script>

<div class="topbar">
  <div class="logo">🧴 试用品效期管理</div>
  <nav>
    {#each pages as p}
      <button class:active={p.id === active.id} on:click={() => nav(p.id)}>{p.label}</button>
    {/each}
  </nav>
</div>

<div class="page">
  <svelte:component this={active.comp} />
</div>

{#if $toast}
  <div class="toast {$toast.type}">{$toast.message}</div>
{/if}
