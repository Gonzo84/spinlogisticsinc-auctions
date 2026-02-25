import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { inject } from 'vue'
import type Keycloak from 'keycloak-js'

let apiInstance: AxiosInstance | null = null
let keycloakRef: Keycloak | null = null

export function useApi() {
  // Capture keycloak during setup() context (only on first call)
  if (!keycloakRef) {
    keycloakRef = inject<Keycloak>('keycloak') ?? null
  }

  if (!apiInstance) {
    apiInstance = axios.create({
      baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
    })

    apiInstance.interceptors.request.use(async (config) => {
      if (keycloakRef) {
        try {
          await keycloakRef.updateToken(30)
        } catch {
          await keycloakRef.login()
        }
        if (keycloakRef.token) {
          config.headers.Authorization = `Bearer ${keycloakRef.token}`
        }
      }
      return config
    })

    apiInstance.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401 && keycloakRef) {
          keycloakRef.logout({ redirectUri: window.location.origin })
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
