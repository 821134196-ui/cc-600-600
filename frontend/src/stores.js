import { writable } from 'svelte/store'

export const toast = writable(null)

let timer
export function showToast(message, type = 'ok') {
  clearTimeout(timer)
  toast.set({ message, type })
  timer = setTimeout(() => toast.set(null), 3200)
}
