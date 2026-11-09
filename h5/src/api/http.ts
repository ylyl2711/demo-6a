import axios, { type AxiosRequestConfig } from 'axios'
import { showToast } from 'vant'
import { clearAuth, TOKEN_KEY } from '@/utils/storage'

interface ApiResult<T> {
  code: number
  msg: string
  data: T
}

const http = axios.create({
  timeout: 20000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/** 统一解包后端 {code,msg,data}，非 200 时提示并抛出 */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  let response
  try {
    response = await http.request<ApiResult<T>>(config)
  } catch {
    showToast('网络异常，请稍后重试')
    throw new Error('网络异常')
  }

  const body = response.data
  if (!body || typeof body.code !== 'number') {
    return body as unknown as T
  }
  if (body.code !== 200) {
    if (body.code === 401) {
      clearAuth()
      if (window.location.pathname !== '/login') {
        window.location.replace('/login')
      }
    }
    showToast(body.msg || '请求失败')
    throw new Error(body.msg || '请求失败')
  }
  return body.data
}
