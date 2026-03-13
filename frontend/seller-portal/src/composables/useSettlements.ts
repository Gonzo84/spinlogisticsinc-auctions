import { ref, computed, readonly } from 'vue'
import { useApi } from './useApi'
import { useErrorHandler } from './useErrorHandler'
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
  const { handleApiError, handleGracefulDegradation, is404 } = useErrorHandler()

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
      const rawItems = Array.isArray(data) ? data : (data?.items ?? [])
      // Backend now returns commissionAmount and commissionRate directly.
      // Fallback only needed if seller-service hasn't been updated yet.
      const items: Settlement[] = rawItems.map((s) => {
        const raw = s as unknown as Record<string, unknown>
        if (raw.commissionAmount === undefined && raw.commission !== undefined) {
          const commission = Number(raw.commission)
          const hammer = Number(raw.hammerPrice ?? 0)
          return {
            ...s,
            commissionAmount: commission,
            commissionRate: Number(raw.commissionRate) || (hammer > 0 ? Math.round(commission / hammer * 10000) / 100 : 0),
          } as Settlement
        }
        return s as Settlement
      })
      settlements.value = items
      pagination.value = {
        total: items.length,
        page: 1,
        pageSize: items.length || 20,
        totalPages: 1,
      }
    } catch (err: unknown) {
      settlements.value = []
      if (is404(err)) {
        handleGracefulDegradation('fetchSettlements')
      } else {
        error.value = handleApiError(err, 'Failed to load settlements')
      }
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
      const raw = await get<ApiResponse<{ url: string }> | { url: string }>(
        `/sellers/me/settlements/${settlementId}/invoice`,
      )
      const data = unwrapApiResponse<{ url: string }>(raw)
      if (data.url) {
        window.open(data.url, '_blank')
      }
    } catch (err: unknown) {
      if (is404(err)) {
        handleGracefulDegradation('downloadInvoice')
      } else {
        error.value = handleApiError(err, 'Failed to download invoice')
      }
    }
  }

  async function fetchMonthlySettlements(): Promise<
    { month: string; amount: number; count: number }[]
  > {
    try {
      const raw = await get<
        | ApiResponse<
            { month: string; totalNet: number; settlementCount: number }[]
          >
        | { month: string; totalNet: number; settlementCount: number }[]
      >('/sellers/me/settlements/monthly')
      const data = unwrapApiResponse<
        | { month: string; totalNet: number; settlementCount: number }[]
        | { items?: { month: string; totalNet: number; settlementCount: number }[] }
      >(raw)
      const items = Array.isArray(data) ? data : (data?.items ?? [])
      return items.map((item) => ({
        month: item.month,
        amount: item.totalNet ?? 0,
        count: item.settlementCount ?? 0,
      }))
    } catch (err: unknown) {
      if (is404(err)) {
        handleGracefulDegradation('fetchMonthlySettlements')
      } else {
        handleApiError(err, 'Failed to load monthly settlements')
      }
      return []
    }
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
