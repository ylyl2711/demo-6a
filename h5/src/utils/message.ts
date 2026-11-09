import type { AppMessage, WorkOrderSummary } from '@/types'

const STATUS_TIP: Record<number, { title: string; text: (o: WorkOrderSummary) => string }> = {
  0: {
    title: '新工单分配',
    text: (o) => `工单 ${o.workOrderNo} 已分配给您，产品：${o.materialName}，计划数量 ${o.planNum}`
  },
  1: {
    title: '工单生产中',
    text: (o) => `工单 ${o.workOrderNo}（${o.materialName}）正在生产，工序进度 ${o.doneProcess}/${o.totalProcess}`
  },
  2: {
    title: '工单待质检',
    text: (o) => `工单 ${o.workOrderNo} 已提交完工，等待质检确认，报工记录已锁定`
  },
  3: {
    title: '工单已完成',
    text: (o) => `工单 ${o.workOrderNo}（${o.materialName}）已完成，良品 ${o.goodTotal}，不良 ${o.badTotal}`
  }
}

/** 由当前操作工的工单派生消息，无需额外后端接口 */
export function buildMessages(orders: WorkOrderSummary[]): AppMessage[] {
  const list: AppMessage[] = []

  orders.forEach((order) => {
    const tip = STATUS_TIP[order.workStatus]
    if (tip) {
      list.push({
        id: `order-${order.id}-${order.workStatus}`,
        type: 'order',
        title: tip.title,
        content: tip.text(order),
        time: order.createTime,
        orderId: order.id
      })
    }
    if (Number(order.badTotal) > 0) {
      list.push({
        id: `alert-${order.id}-${order.badTotal}`,
        type: 'alert',
        title: '质量预警',
        content: `工单 ${order.workOrderNo} 累计不良 ${order.badTotal} 件，请核对该工序工艺参数`,
        time: order.createTime,
        orderId: order.id
      })
    }
  })

  list.sort((a, b) => toTimestamp(b.time) - toTimestamp(a.time))

  list.push(
    {
      id: 'system-maintain',
      type: 'system',
      title: '系统维护通知',
      content: '系统将于今晚 22:00-23:00 进行例行维护，期间可能短暂不可用',
      time: ''
    },
    {
      id: 'system-rule',
      type: 'system',
      title: '报工规范提醒',
      content: '请按工序实际产出填写良品/不良数量，不良数大于 0 时须选择不良原因并上传现场照片',
      time: ''
    }
  )

  return list
}

function toTimestamp(time: string): number {
  if (!time) return 0
  const ts = new Date(time.replace(/-/g, '/')).getTime()
  return Number.isFinite(ts) ? ts : 0
}

/** 相对时间：刚刚 / x分钟前 / x小时前 / x天前 */
export function relativeTime(time: string): string {
  if (!time) return '今天'
  const ts = toTimestamp(time)
  if (!ts) return time
  const diff = Date.now() - ts
  if (diff < 0) return '刚刚'
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}天前`
  return time.slice(0, 10)
}
