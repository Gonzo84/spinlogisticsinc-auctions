import { ref, readonly } from 'vue'
import { useApi } from './useApi'
import type { LotIntakeRequest, BulkLotIntakeRequest, LotIntakeResponse } from '@/types'

interface ApiResponse<T> {
  data: T
  success: boolean
  message?: string
}

export function useLotIntake() {
  const { post, loading, error } = useApi()
  const lastIntake = ref<LotIntakeResponse | null>(null)
  const bulkResults = ref<LotIntakeResponse[]>([])

  async function submitIntake(data: LotIntakeRequest): Promise<LotIntakeResponse | null> {
    try {
      const response = await post<ApiResponse<LotIntakeResponse>>('/brokers/lots/intake', data)
      lastIntake.value = response.data
      return response.data
    } catch {
      return null
    }
  }

  async function submitBulkIntake(data: BulkLotIntakeRequest): Promise<LotIntakeResponse[]> {
    try {
      const response = await post<ApiResponse<LotIntakeResponse[]>>('/brokers/lots/bulk-intake', data)
      bulkResults.value = response.data
      return response.data
    } catch {
      return []
    }
  }

  return {
    lastIntake: readonly(lastIntake),
    bulkResults: readonly(bulkResults),
    loading,
    error,
    submitIntake,
    submitBulkIntake,
  }
}
