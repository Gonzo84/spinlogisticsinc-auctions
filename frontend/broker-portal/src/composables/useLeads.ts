import { ref, readonly } from 'vue'
import { useApi } from './useApi'
import type { Lead, VisitScheduleRequest } from '@/types'

interface ApiResponse<T> {
  data: T
  success: boolean
  message?: string
}

interface PagedResponse<T> {
  items: T[]
  totalItems: number
  page: number
  pageSize: number
  totalPages: number
}

export function useLeads() {
  const { get, post, loading, error } = useApi()
  const leads = ref<Lead[]>([])
  const totalItems = ref(0)
  const totalPages = ref(0)

  async function fetchLeads(params?: Record<string, unknown>): Promise<void> {
    try {
      const response = await get<ApiResponse<PagedResponse<Lead>>>('/brokers/leads', params)
      leads.value = response.data.items
      totalItems.value = response.data.totalItems
      totalPages.value = response.data.totalPages
    } catch {
      // error is already set by useApi
    }
  }

  async function scheduleVisit(leadId: string, data: VisitScheduleRequest): Promise<boolean> {
    try {
      await post<ApiResponse<Lead>>(`/leads/${leadId}/visit`, data)
      return true
    } catch {
      return false
    }
  }

  return {
    leads: readonly(leads),
    totalItems: readonly(totalItems),
    totalPages: readonly(totalPages),
    loading,
    error,
    fetchLeads,
    scheduleVisit,
  }
}
