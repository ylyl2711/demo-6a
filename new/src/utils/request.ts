import axios, { CanceledError } from 'axios';
import { isString } from 'lodash-es';
import qs from 'qs';
import { message as $message, Modal } from 'ant-design-vue';
import type { AxiosRequestConfig, AxiosResponse } from 'axios';
import { ResultEnum } from '@/enums/httpEnum';
import { accessToken, notifyUnauthorized, serverConnected } from '@/store/runtime';

export interface RequestOptions extends AxiosRequestConfig {
  /** 是否直接将数据从响应中提取出，例如直接返回 res.data，而忽略 res.code 等信息 */
  isReturnResult?: boolean;
  /** 请求成功是提示信息 */
  successMsg?: string;
  /** 请求失败是提示信息 */
  errorMsg?: string;
  /** 成功时，是否显示后端返回的成功信息 */
  showSuccessMsg?: boolean;
  /** 失败时，是否显示后端返回的失败信息 */
  showErrorMsg?: boolean;
  requestType?: 'json' | 'form';
}

const UNKNOWN_ERROR = '未知错误，请重试';

/** 真实请求的路径前缀 */
export const baseApiUrl = import.meta.env.VITE_BASE_API_URL;
/** mock请求路径前缀 */
// const baseMockUrl = import.meta.env.VITE_MOCK_API;

const service = axios.create({
  baseURL: baseApiUrl,
  timeout: 10000,
  paramsSerializer(params) {
    return qs.stringify(params, { arrayFormat: 'brackets' });
  },
});

service.interceptors.request.use(
  (config) => {
    const token = accessToken.value;
    if (token && config.headers)
      config.headers.Authorization = `Bearer ${token}`;
    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);

service.interceptors.response.use(
  (response: AxiosResponse<BaseResponse>) => {
    if (response.config.responseType === 'blob') {
      return response;
    }
    const res = response.data;
    const msg = res.msg || res.message;

    // if the custom code is not 200, it is judged as an error.
    if (res.code !== ResultEnum.SUCCESS) {
      $message.error(msg || UNKNOWN_ERROR);
      // token 失效 / 未登录
      if (res.code === 401) {
        Modal.confirm({
          title: '警告',
          content: msg || '登录已失效，您可以取消停留在该页上，或重新登录',
          okText: '重新登录',
          cancelText: '取消',
          onOk: () => {
            notifyUnauthorized();
            window.location.reload();
          },
        });
      }

      // throw other
      const error = new Error(msg || UNKNOWN_ERROR) as Error & { code: any };
      error.code = res.code;
      return Promise.reject(error);
    }

    serverConnected.value = true;
    return response;
  },
  (error) => {
    if (!(error instanceof CanceledError)) {
      // 处理 422 或者 500 的错误异常提示
      const errMsg = error?.response?.data?.msg ?? error?.response?.data?.message ?? UNKNOWN_ERROR;
      $message.error({ content: errMsg, key: errMsg });
      error.message = errMsg;
    }
    return Promise.reject(error);
  },
);

type BaseResponse<T = any> = Omit<API.ResOp, 'data'> & {
  data: T;
  msg?: string;
};

export function request<T = any>(
  url: string,
  config: { isReturnResult: false } & RequestOptions,
): Promise<BaseResponse<T>>;
export function request<T = any>(
  url: string,
  config: RequestOptions,
): Promise<BaseResponse<T>['data']>;
export function request<T = any>(
  config: { isReturnResult: false } & RequestOptions,
): Promise<BaseResponse<T>>;
export function request<T = any>(config: RequestOptions): Promise<BaseResponse<T>['data']>;
/**
 *
 * @param url - request url
 * @param config - AxiosRequestConfig
 */
export async function request(_url: string | RequestOptions, _config: RequestOptions = {}) {
  const url = isString(_url) ? _url : _url.url;
  const config = isString(_url) ? _config : _url;
  try {
    // 兼容 from data 文件上传的情况
    const { requestType, isReturnResult = true, ...rest } = config;

    const response = (await service.request({
      url,
      ...rest,
      headers: {
        ...rest.headers,
        ...(requestType === 'form' ? { 'Content-Type': 'multipart/form-data' } : {}),
      },
    })) as AxiosResponse<BaseResponse>;
    const { data } = response;
    const { code, message, msg } = data || {};

    const hasSuccess = data && Reflect.has(data, 'code') && code === ResultEnum.SUCCESS;

    if (hasSuccess) {
      const { successMsg, showSuccessMsg } = config;
      if (successMsg) {
        $message.success(successMsg);
      } else if (showSuccessMsg && (msg || message)) {
        $message.success(msg || message);
      }
    }

    // 页面代码需要获取 code，data，message 等信息时，需要将 isReturnResult 设置为 false
    if (!isReturnResult) {
      return data;
    } else {
      return data.data;
    }
  } catch (error: any) {
    return Promise.reject(error);
  }
}

/** 下载后端返回的文件（Excel 等） */
export async function download(
  url: string,
  params?: Recordable<any>,
  filename = 'export.xlsx',
) {
  const response = await service.get(url, { params, responseType: 'blob' });
  const disposition = response.headers['content-disposition'] as string | undefined;
  let name = filename;
  if (disposition) {
    const match = /filename\*?=(?:UTF-8'')?["']?([^"';]+)/i.exec(disposition);
    if (match?.[1]) {
      name = decodeURIComponent(match[1].replace(/["']/g, ''));
    }
  }
  const link = document.createElement('a');
  link.href = URL.createObjectURL(new Blob([response.data as BlobPart]));
  link.download = name;
  link.click();
  URL.revokeObjectURL(link.href);
}
