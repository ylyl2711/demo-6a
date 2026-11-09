import { request, download } from '@/utils/request';

/** 分页结果 */
export interface PageResult<T> {
  total: number;
  list: T[];
}

/** 当前登录用户 */
export interface UserInfo {
  id: number;
  username: string;
  realName: string;
  avatar?: string;
  roleId: number;
  roleCode: 'ADMIN' | 'BUSINESS' | 'OPERATOR' | string;
  roleName: string;
  perms: string;
}

export interface LoginResult {
  token: string;
  userInfo: UserInfo;
}

export interface Material {
  id?: number;
  warehouseName: string;
  baseSite: string;
  materialCode: string;
  materialName: string;
  materialType?: number;
  totalStock?: number;
  stockMin?: number;
  stockMax?: number;
  stagnantDay?: number;
}

export interface WorkOrder {
  id: number;
  workOrderNo: string;
  baseSite: string;
  materialId: number;
  materialCode: string;
  materialName: string;
  planNum: number;
  operatorUserId: number;
  operatorName: string;
  workStatus: number;
  productBatch: string;
  createTime: string;
  totalProcess?: number;
  doneProcess?: number;
  goodTotal?: number;
  badTotal?: number;
}

/* ------------------------------ 认证 ------------------------------ */
export const authApi = {
  login: (body: { username: string; password: string }) =>
    request<LoginResult>('/auth/login', { method: 'POST', data: body }),
  me: () => request<UserInfo>('/auth/me', { method: 'GET' }),
  logout: () => request<void>('/auth/logout', { method: 'POST' }),
};

/* ---------------------------- 公共基础数据 ---------------------------- */
export const commonApi = {
  dict: (type: string) => request<any[]>('/common/dict', { method: 'GET', params: { type } }),
  baseSite: () => request<{ label: string; value: string }[]>('/common/baseSite', { method: 'GET' }),
  badReason: () =>
    request<{ id: number; label: string; value: string }[]>('/common/badReason', { method: 'GET' }),
  operator: () =>
    request<{ id: number; realName: string; username: string }[]>('/common/operator', {
      method: 'GET',
    }),
  product: () =>
    request<{ id: number; materialCode: string; materialName: string; baseSite: string }[]>(
      '/common/product',
      { method: 'GET' },
    ),
  warning: () => request<any[]>('/common/warning', { method: 'GET' }),
};

/* ---------------------------- 仓库物料档案 ---------------------------- */
export const warehouseApi = {
  list: (params: Recordable) =>
    request<PageResult<Material>>('/warehouse/list', { method: 'GET', params }),
  options: (params?: Recordable) =>
    request<Material[]>('/warehouse/options', { method: 'GET', params }),
  save: (data: Material) => request<void>('/warehouse/save', { method: 'POST', data }),
  remove: (id: number) => request<void>('/warehouse/delete', { method: 'POST', params: { id } }),
};

/* ------------------------------- 库存 ------------------------------- */
export const stockApi = {
  inBound: (data: Recordable) => request<any>('/stock/inBound', { method: 'POST', data }),
  outBound: (data: Recordable) => request<any[]>('/stock/outBound', { method: 'POST', data }),
  fifo: (materialId: number) =>
    request<any[]>('/stock/batch/fifo', { method: 'GET', params: { materialId } }),
  bills: (params: Recordable) =>
    request<PageResult<any>>('/stock/bill/list', { method: 'GET', params }),
  voidBill: (billId: number) =>
    request<void>('/stock/bill/void', { method: 'POST', params: { billId } }),
  stagnant: () => request<any[]>('/stock/stagnant', { method: 'GET' }),
  configStagnant: (materialId: number, day: number) =>
    request<void>('/stock/stagnant/config', {
      method: 'POST',
      params: { materialId, day },
    }),
  check: (materialId: number, realNum: number) =>
    request<any>('/stock/check', { method: 'POST', params: { materialId, realNum } }),
  exportLedger: (baseSite?: string) =>
    download('/stock/export', { baseSite }, '库存台账.xlsx'),
};

/* ------------------------------ 批次追溯 ------------------------------ */
export const batchApi = {
  traceForward: (batchNo: string) =>
    request<any>('/batch/traceForward', { method: 'GET', params: { batchNo } }),
  traceBackward: (productBatch: string) =>
    request<any>('/batch/traceBackward', { method: 'GET', params: { productBatch } }),
  exportForward: (batchNo: string) =>
    download('/batch/traceForward/export', { batchNo }, `原料批次正向追溯_${batchNo}.xlsx`),
  exportBackward: (productBatch: string) =>
    download(
      '/batch/traceBackward/export',
      { productBatch },
      `成品批次反向溯源_${productBatch}.xlsx`,
    ),
};

/* ------------------------------ 生产工单 ------------------------------ */
export const workOrderApi = {
  save: (data: Recordable) => request<WorkOrder>('/workorder/save', { method: 'POST', data }),
  list: (params: Recordable) =>
    request<PageResult<WorkOrder>>('/workorder/list', { method: 'GET', params }),
  detail: (id: number) => request<any>('/workorder/detail', { method: 'GET', params: { id } }),
  progress: (id: number) => request<any>('/workorder/progress', { method: 'GET', params: { id } }),
  bom: (productMaterialId: number) =>
    request<any[]>('/workorder/bom', { method: 'GET', params: { productMaterialId } }),
  saveBom: (data: Recordable) => request<void>('/workorder/bom/save', { method: 'POST', data }),
  template: (materialId: number) =>
    request<any[]>('/workorder/template', { method: 'GET', params: { materialId } }),
  saveTemplate: (data: Recordable) =>
    request<void>('/workorder/template/save', { method: 'POST', data }),
  options: () => request<any>('/workorder/options', { method: 'GET' }),
};

/* ------------------------------ 工单质检 ------------------------------ */
export const reportCheckApi = {
  todo: (params: Recordable) =>
    request<PageResult<WorkOrder>>('/reportCheck/todo', { method: 'GET', params }),
  audit: (data: Recordable) => request<void>('/reportCheck/audit', { method: 'POST', data }),
};

/* ---------------------------- 质量统计与报告 ---------------------------- */
export const qualityApi = {
  stat: (params: Recordable) => request<any>('/quality/stat', { method: 'GET', params }),
  exportStat: (params: Recordable) =>
    download('/quality/stat/export', params, '不良品分类统计.xlsx'),
  exportReport: (orderId: number) =>
    download('/quality/report/export', { orderId }, `质检报告_${orderId}.xlsx`),
};

/* ------------------------------- 系统管理 ------------------------------- */
export const systemApi = {
  userList: (params: Recordable) =>
    request<PageResult<any>>('/system/user/list', { method: 'GET', params }),
  saveUser: (data: Recordable) => request<void>('/system/user/save', { method: 'POST', data }),
  userStatus: (id: number, status: number) =>
    request<void>('/system/user/status', { method: 'POST', params: { id, status } }),
  deleteUser: (id: number) =>
    request<void>('/system/user/delete', { method: 'POST', params: { id } }),
  roleList: () => request<any[]>('/system/role/list', { method: 'GET' }),
  savePerm: (roleId: number, menus: string) =>
    request<void>('/system/role/perm', { method: 'POST', params: { roleId, menus } }),
  dictList: (dictType?: string) =>
    request<any[]>('/system/dict/list', { method: 'GET', params: { dictType } }),
  saveDict: (data: Recordable) => request<void>('/system/dict/save', { method: 'POST', data }),
  deleteDict: (id: number) =>
    request<void>('/system/dict/delete', { method: 'POST', params: { id } }),
  logList: (params: Recordable) =>
    request<PageResult<any>>('/system/log/list', { method: 'GET', params }),
  exportLog: (params: Recordable) => download('/system/log/export', params, '操作日志.xlsx'),
};

/* ------------------------------- 文件上传 ------------------------------- */
export const fileApi = {
  upload: (file: File) => {
    const data = new FormData();
    data.append('file', file);
    return request<{ url: string; name: string }>('/file/upload', {
      method: 'POST',
      data,
      requestType: 'form',
    });
  },
};
