import type { WorkOrderStatus } from '@/types'

type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'default'

const STATUS_META: Record<WorkOrderStatus, { text: string; tag: TagType }> = {
  0: { text: '待生产', tag: 'warning' },
  1: { text: '生产中', tag: 'primary' },
  2: { text: '待质检', tag: 'danger' },
  3: { text: '已完成', tag: 'success' }
}

export function statusText(status: number): string {
  return STATUS_META[status as WorkOrderStatus]?.text ?? '未知状态'
}

export function statusTagType(status: number): TagType {
  return STATUS_META[status as WorkOrderStatus]?.tag ?? 'default'
}

/** 去掉多余小数位，例如 100.00 -> 100，98.50 -> 98.5 */
export function formatNum(value: number | string | null | undefined): string {
  const num = Number(value)
  if (!Number.isFinite(num)) return '0'
  return Number.isInteger(num)
    ? String(num)
    : num.toFixed(2).replace(/0+$/, '').replace(/\.$/, '')
}
