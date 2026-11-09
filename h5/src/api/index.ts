import { request } from './http'
import type {
  LoginResult,
  SubmitPayload,
  UploadResult,
  WorkOrderDetail,
  WorkOrderStatus,
  WorkOrderSummary
} from '@/types'

export function login(username: string, password: string) {
  return request<LoginResult>({
    url: '/api/auth/login',
    method: 'post',
    data: { username, password }
  })
}

export function getMyOrders(workStatus?: WorkOrderStatus) {
  return request<WorkOrderSummary[]>({
    url: '/api/report/myOrders',
    method: 'get',
    params: workStatus === undefined ? {} : { workStatus }
  })
}

export function getOrderDetail(orderId: number) {
  return request<WorkOrderDetail>({
    url: '/api/report/detail',
    method: 'get',
    params: { orderId }
  })
}

export function submitReport(payload: SubmitPayload) {
  return request<void>({
    url: '/api/report/submit',
    method: 'post',
    data: payload
  })
}

export function finishOrder(workOrderId: number) {
  return request<void>({
    url: '/api/report/finish',
    method: 'post',
    params: { workOrderId }
  })
}

export function uploadFile(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request<UploadResult>({
    url: '/api/file/upload',
    method: 'post',
    data: form
  })
}
