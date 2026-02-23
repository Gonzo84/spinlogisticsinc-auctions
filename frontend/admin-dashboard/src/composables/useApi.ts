import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { useAuth } from './useAuth'

let apiInstance: AxiosInstance | null = null

export function useApi() {
  if (!apiInstance) {
    apiInstance = axios.create({
      baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
    })

    apiInstance.interceptors.request.use(async (config) => {
      const { refreshToken } = useAuth()
      const token = await refreshToken()
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
      return config
    })

    apiInstance.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          const { logout } = useAuth()
          logout()
        }
        return Promise.reject(error)
      }
    )
  }

  async function get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response: AxiosResponse<T> = await apiInstance!.get(url, config)
    return response.data
  }

  async function post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    const response: AxiosResponse<T> = await apiInstance!.post(url, data, config)
    return response.data
  }

  async function put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    const response: AxiosResponse<T> = await apiInstance!.put(url, data, config)
    return response.data
  }

  async function patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    const response: AxiosResponse<T> = await apiInstance!.patch(url, data, config)
    return response.data
  }

  async function del<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response: AxiosResponse<T> = await apiInstance!.delete(url, config)
    return response.data
  }

  return {
    client: apiInstance,
    get,
    post,
    put,
    patch,
    del,
  }
}
