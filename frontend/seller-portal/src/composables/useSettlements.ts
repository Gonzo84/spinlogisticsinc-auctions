import { ref, computed, readonly } from 'vue'
import { useApi } from './useApi'
import type {
  Settlement,
  SettlementStatus,
  SettlementFilter,
  SettlementTotals,
  ApiResponse,
} from '@/types'

export type { Settlement, SettlementStatus, SettlementFilter, SettlementTotals }

export function useSettlements() {
  const { get, loading, error } = useApi()

  const settlements = ref<Settlement[]>([])
  const pagination = ref({ total: 0, page: 1, pageSize: 20, totalPages: 0 })
  const totals = ref<SettlementTotals>({
    totalHammerPrice: 0,
    totalCommission: 0,
    totalNetAmount: 0,
    currency: 'EUR',
    count: 0,
  })

  const pendingSettlements = computed(() =>
    settlements.value.filter((s) => s.status === 'pending'),
  )

  const paidSettlements = computed(() =>
    settlements.value.filter((s) => s.status === 'paid'),
  )

  /** Unwrap ApiResponse wrapper if present, returning the inner data. */
  function unwrapApiResponse<T>(raw: unknown): T {
    const obj = raw as Record<string, unknown> | null
    if (obj?.data && typeof obj.data === 'object') {
      return obj.data as T
    }
    return raw as T
  }

  async function fetchSettlements(filters: SettlementFilter = {}): Promise<void> {
    const params: Record<string, unknown> = {
      page: filters.page ?? 1,
      pageSize: filters.pageSize ?? 20,
    }
    if (filters.status) params.status = filters.status
    if (filters.dateFrom) params.dateFrom = filters.dateFrom
    if (filters.dateTo) params.dateTo = filters.dateTo

    try {
      const raw = await get<ApiResponse<Settlement[]> | Settlement[]>('/sellers/me/settlements', params)
      // Unwrap ApiResponse wrapper ({data: T}) if present
      const data = unwrapApiResponse<Settlement[] | { items?: Settlement[] }>(raw)
      // seller-service returns a flat list, not paginated
      const items: Settlement[] = Array.isArray(data) ? data : (data?.items ?? [])
      settlements.value = items
      pagination.value = {
        total: items.length,
        page: 1,
        pageSize: items.length || 20,
        totalPages: 1,
      }
    } catch {
      // Seller-service may not have settlements yet -- show empty state
      settlements.value = []
      error.value = null
    }
  }

  /**
   * Compute settlement totals from the fetched settlements list.
   * The /sellers/me/settlements/totals endpoint does not exist.
   */
  async function fetchSettlementTotals(filters: SettlementFilter = {}): Promise<void> {
    // If settlements haven't been loaded yet, fetch them first
    if (settlements.value.length === 0) {
      await fetchSettlements(filters)
    }

    // Compute totals from the list
    const items = settlements.value
    totals.value = {
      totalHammerPrice: items.reduce((sum, s) => sum + (s.hammerPrice ?? 0), 0),
      totalCommission: items.reduce((sum, s) => sum + (s.commissionAmount ?? 0), 0),
      totalNetAmount: items.reduce((sum, s) => sum + (s.netAmount ?? 0), 0),
      currency: 'EUR',
      count: items.length,
    }
  }

  async function downloadInvoice(settlementId: string): Promise<void> {
    try {
      const { url } = await get<{ url: string }>(`/sellers/me/settlements/${settlementId}/invoice`)
      window.open(url, '_blank')
    } catch {
      // Invoice download not available
      error.value = null
    }
  }

  async function fetchMonthlySettlements(): Promise<
    { month: string; amount: number; count: number }[]
  > {
    // Monthly settlements endpoint doesn't exist - return empty
    return []
  }

  return {
    settlements,
    pagination: readonly(pagination),
    totals: readonly(totals),
    pendingSettlements,
    paidSettlements,
    loading,
    error,
    fetchSettlements,
    fetchSettlementTotals,
    downloadInvoice,
    fetchMonthlySettlements,
  }
}
