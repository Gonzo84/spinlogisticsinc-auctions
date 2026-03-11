import { ref, readonly } from 'vue'
import { useApi } from './useApi'
import type { BrokerDashboard } from '@/types'

interface ApiResponse<T> {
  data: T
  success: boolean
  message?: string
}

export function useDashboard() {
  const { get, loading, error } = useApi()
  const dashboard = ref<BrokerDashboard | null>(null)

  async function fetchDashboard(): Promise<void> {
    try {
      const response = await get<ApiResponse<BrokerDashboard>>('/brokers/me/dashboard')
      dashboard.value = response.data
    } catch {
      // error is already set by useApi
    }
  }

  return {
    dashboard: readonly(dashboard),
    loading,
    error,
    fetchDashboard,
  }
}
