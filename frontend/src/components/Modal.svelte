<script>
  import { createEventDispatcher } from 'svelte'
  export let title = ''
  const dispatch = createEventDispatcher()
  function close() { dispatch('close') }
</script>

<div class="mask" on:click|self={close} on:keydown={() => {}} role="dialog">
  <div class="modal">
    <div class="head">
      <h3>{title}</h3>
      <button class="x" on:click={close}>✕</button>
    </div>
    <slot />
  </div>
</div>

<style>
  .mask {
    position: fixed; inset: 0; background: rgba(31, 35, 40, .45);
    display: flex; align-items: center; justify-content: center; z-index: 50;
  }
  .modal {
    background: #fff; border-radius: 10px; padding: 18px 20px;
    min-width: 380px; max-width: 560px; max-height: 85vh; overflow: auto;
    box-shadow: 0 8px 30px rgba(0,0,0,.2);
  }
  .head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
  h3 { margin: 0; font-size: 15px; }
  .x { border: none; background: none; font-size: 15px; color: #8b949e; padding: 2px 6px; }
</style>
