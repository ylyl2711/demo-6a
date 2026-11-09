import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getMyOrders } from '@/api'
import type { WorkOrderSummary } from '@/types'
import { buildMessages } from '@/utils/message'

const READ_KEY = 'stockwork_h5_read_msgs'
const CLEAR_KEY = 'stockwork_h5_cleared_msgs'

function loadIds(key: string): string[] {
  const raw = localStorage.getItem(key)
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? (parsed as string[]) : []
  } catch {
    localStorage.removeItem(key)
    return []
  }
}

export const useOrderStore = defineStore('order', () => {
  const orders = ref<WorkOrderSummary[]>([])
  const loading = ref(false)
  const loaded = ref(false)
  const readIds = ref<string[]>(loadIds(READ_KEY))
  const clearedIds = ref<string[]>(loadIds(CLEAR_KEY))

  const messages = computed(() => buildMessages(orders.value))
  const visibleMessages = computed(() =>
    messages.value.filter((msg) => !clearedIds.value.includes(msg.id))
  )
  const unreadCount = computed(
    () => visibleMessages.value.filter((msg) => !readIds.value.includes(msg.id)).length
  )

  const stats = computed(() => {
    let good = 0
    let bad = 0
    let done = 0
    let pending = 0
    orders.value.forEach((order) => {
      good += Number(order.goodTotal) || 0
      bad += Number(order.badTotal) || 0
      if (order.workStatus === 3) done += 1
      if (order.workStatus === 0 || order.workStatus === 1) pending += 1
    })
    const rate = good + bad > 0 ? Math.round((good / (good + bad)) * 1000) / 10 : 0
    return { total: orders.value.length, done, pending, good, bad, rate }
  })

  async function load() {
    loading.value = true
    try {
      orders.value = await getMyOrders()
      loaded.value = true
    } catch {
      // 错误提示由请求层统一处理
    } finally {
      loading.value = false
    }
  }

  async function ensureLoaded() {
    if (!loaded.value && !loading.value) await load()
  }

  function isRead(id: string) {
    return readIds.value.includes(id)
  }

  function markRead(id: string) {
    if (readIds.value.includes(id)) return
    readIds.value = [...readIds.value, id]
    localStorage.setItem(READ_KEY, JSON.stringify(readIds.value))
  }

  function markAllRead() {
    const ids = visibleMessages.value.map((msg) => msg.id)
    readIds.value = Array.from(new Set([...readIds.value, ...ids]))
    localStorage.setItem(READ_KEY, JSON.stringify(readIds.value))
  }

  function clearMessages() {
    clearedIds.value = messages.value.map((msg) => msg.id)
    localStorage.setItem(CLEAR_KEY, JSON.stringify(clearedIds.value))
  }

  function reset() {
    orders.value = []
    loaded.value = false
  }

  return {
    orders,
    loading,
    loaded,
    messages,
    visibleMessages,
    unreadCount,
    stats,
    load,
    ensureLoaded,
    isRead,
    markRead,
    markAllRead,
    clearMessages,
    reset
  }
})
