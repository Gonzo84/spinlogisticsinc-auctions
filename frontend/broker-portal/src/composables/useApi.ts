import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosError } from 'axios'
import { inject, ref, readonly } from 'vue'
import type Keycloak from 'keycloak-js'

interface ApiErrorData {
  message?: string
}

let apiInstance: AxiosInstance | null = null

export function useApi() {
  const keycloak = inject<Keycloak>('keycloak')
  const loading = ref(false)
  const error = ref<string | null>(null)

  if (!apiInstance) {
    apiInstance = axios.create({
      baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
    })

    // Request interceptor: attach auth token
    apiInstance.interceptors.request.use(
      async (config) => {
        if (keycloak?.authenticated) {
          try {
            await keycloak.updateToken(30)
          } catch {
            await keycloak.login()
            return Promise.reject(new Error('Token refresh failed'))
          }
          config.headers.Authorization = `Bearer ${keycloak.token}`
        }
        return config
      },
      (err: unknown) => Promise.reject(err),
    )

    // Response interceptor: handle errors
    apiInstance.interceptors.response.use(
      (response) => response,
      (err: AxiosError) => {
        if (err.response?.status === 401 && keycloak) {
          keycloak.login()
        }
        return Promise.reject(err)
      },
    )
  }

  async function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
    loading.value = true
    error.value = null
    try {
      const response = await apiInstance!.request<T>(config)
      return response.data
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        const axiosErr = err as AxiosError<ApiErrorData>
        error.value = axiosErr.response?.data?.message ?? axiosErr.message ?? 'An error occurred'
      } else {
        error.value = err instanceof Error ? err.message : 'An error occurred'
      }
      throw err
    } finally {
      loading.value = false
    }
  }

  async function get<T = unknown>(url: string, params?: Record<string, unknown>): Promise<T> {
    return request<T>({ method: 'GET', url, params })
  }

  async function post<T = unknown>(url: string, data?: unknown): Promise<T> {
    return request<T>({ method: 'POST', url, data })
  }

  async function put<T = unknown>(url: string, data?: unknown): Promise<T> {
    return request<T>({ method: 'PUT', url, data })
  }

  async function patch<T = unknown>(url: string, data?: unknown): Promise<T> {
    return request<T>({ method: 'PATCH', url, data })
  }

  async function del<T = unknown>(url: string): Promise<T> {
    return request<T>({ method: 'DELETE', url })
  }

  return {
    api: apiInstance!,
    loading: readonly(loading),
    error,
    get,
    post,
    put,
    patch,
    del,
  }
}
