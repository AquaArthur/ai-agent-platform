import axios, { type AxiosRequestConfig, type AxiosError } from 'axios'
import { ElMessage } from 'element-plus'

const BASE_API = import.meta.env.VITE_BASE_API || '/api'

// 是否启用 Mock（仅用于 getHello 前后端连通性测试）
const USE_MOCK = (import.meta.env.VITE_USE_MOCK ?? 'true') !== 'false'

const axiosInstance = axios.create({
  baseURL: BASE_API,
  timeout: 15000,
  headers: {
    'Accept': 'application/json',
    'Content-Type': 'application/json'
  }
})

// 请求拦截：注入 Token
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截：统一处理后端的 {code, message, data}
axiosInstance.interceptors.response.use(
  (response) => {
    // 二进制数据直接返回
    const contentType = response.headers?.['content-type'] || ''
    if (response.request?.responseType === 'blob' || 
        response.request?.responseType === 'arraybuffer' || 
        contentType.includes('octet-stream')) {
      return response
    }

    const payload = response.data
    
    // 解包后端统一结构
    if (payload && typeof payload === 'object' && 'code' in payload) {
      // 成功：code 为 0 或 200
      if (payload.code === 0 || payload.code === 200) {
        return payload.data
      }
      
      // 业务错误
      const err = new Error(payload.message || '请求失败') as any
      err.code = payload.code
      err.response = response
      throw err
    }
    
    // 非统一结构（兼容场景）
    return payload
  },
  (error: AxiosError) => {
    if (error.response) {
      // HTTP 错误
      const response = error.response
      const data = response.data as any
      
      // 401/403：清除认证信息并跳转登录
      if ((response.status === 401 || response.status === 403) && 
          !['/login', '/register'].includes(window.location.pathname)) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        ElMessage.error('认证已过期，请重新登录')
        setTimeout(() => window.location.href = '/login', 1000)
      }
      
      const message = data?.message || response.statusText || '请求失败'
      const err = new Error(message) as any
      err.code = data?.code ?? response.status
      err.response = response
      throw err
    }
    
    // 网络错误
    if (error.request) {
      const err = new Error('网络连接失败') as any
      err.code = 'NETWORK_ERROR'
      throw err
    }
    
    // 配置错误
    throw error
  }
)

// 类型安全的 HTTP 客户端
// 由于响应拦截器已经解包了响应，所以返回类型是 T 而不是 AxiosResponse<T>
interface HttpClient {
  get: <T = any>(url: string, config?: AxiosRequestConfig) => Promise<T>
  post: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) => Promise<T>
  put: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) => Promise<T>
  delete: <T = any>(url: string, config?: AxiosRequestConfig) => Promise<T>
  patch: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) => Promise<T>
}

const http: HttpClient = {
  get: <T = any>(url: string, config?: AxiosRequestConfig): Promise<T> => {
    return axiosInstance.get<T>(url, config) as Promise<T>
  },
  post: <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
    return axiosInstance.post<T>(url, data, config) as Promise<T>
  },
  put: <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
    return axiosInstance.put<T>(url, data, config) as Promise<T>
  },
  delete: <T = any>(url: string, config?: AxiosRequestConfig): Promise<T> => {
    return axiosInstance.delete<T>(url, config) as Promise<T>
  },
  patch: <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
    return axiosInstance.patch<T>(url, data, config) as Promise<T>
  }
}

export { http }
export { USE_MOCK }
