export interface UserInfo {
  id: number
  username: string
  realName: string
  roleId?: number
  roleCode: string
  roleName: string
  perms: string
}

export interface LoginResult {
  token: string
  userInfo: UserInfo
}

/** 工单状态：0 待生产 1 生产中 2 待质检 3 已完成 */
export type WorkOrderStatus = 0 | 1 | 2 | 3

export interface WorkOrderSummary {
  id: number
  workOrderNo: string
  baseSite: string
  materialId: number
  materialCode: string
  materialName: string
  planNum: number
  operatorUserId: number
  operatorName: string
  workStatus: WorkOrderStatus
  productBatch: string
  createTime: string
  totalProcess: number
  doneProcess: number
  goodTotal: number
  badTotal: number
}

export interface BadReason {
  id: number
  label: string
  value: string
}

export interface ProcessReport {
  id: number
  workOrderId: number
  processId: number
  processName: string
  goodNum: number
  badNum: number
  badReasonDictId: number | null
  badReason: string
  photoUrl: string
  reportTime: string
}

export interface ProcessItem {
  id: number
  processSeq: number
  processName: string
  requirement: string
  /** 1 未报工 2 已报工 */
  processStatus: 1 | 2
  report: ProcessReport | null
}

export interface WorkOrderSummaryData {
  totalProcess: number
  doneProcess: number
  goodTotal: number
  badTotal: number
}

export type WorkOrderDetail = Omit<
  WorkOrderSummary,
  'totalProcess' | 'doneProcess' | 'goodTotal' | 'badTotal'
> & {
  processes: ProcessItem[]
  summary: WorkOrderSummaryData
  badReasons: BadReason[]
}

export interface SubmitPayload {
  workOrderId: number
  processId: number
  goodNum: number
  badNum: number
  badReasonId: number | null
  photoUrl: string
}

export interface UploadResult {
  url: string
  name: string
}

/** 消息类型：工单 / 预警 / 系统 */
export type MessageType = 'order' | 'alert' | 'system'

export interface AppMessage {
  id: string
  type: MessageType
  title: string
  content: string
  /** 原始时间，系统消息为空 */
  time: string
  orderId?: number
}
