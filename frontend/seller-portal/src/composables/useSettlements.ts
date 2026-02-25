import { ref, computed } from 'vue'
import { useApi } from './useApi'

export type SettlementStatus = 'pending' | 'processing' | 'paid' | 'disputed'

export interface Settlement {
  id: string
  lotId: string
  lotTitle: string
  lotThumbnail: string
  buyerAlias: string
  hammerPrice: number
  commissionRate: number
  commissionAmount: number
  netAmount: number
  currency: string
  status: SettlementStatus
  bankReference: string | null
  invoiceUrl: string | null
  paidAt: string | null
  createdAt: string
  updatedAt: string
}

export interface SettlementFilter {
  status?: SettlementStatus
  dateFrom?: string
  dateTo?: string
  page?: number
  pageSize?: number
}

export interface SettlementTotals {
  totalHammerPrice: number
  totalCommission: number
  totalNetAmount: number
  currency: string
  count: number
}

interface PaginatedResponse<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

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

  async function fetchSettlements(filters: SettlementFilter = {}): Promise<void> {
    const params: Record<string, unknown> = {
      page: filters.page ?? 1,
      pageSize: filters.pageSize ?? 20,
    }
    if (filters.status) params.status = filters.status
    if (filters.dateFrom) params.dateFrom = filters.dateFrom
    if (filters.dateTo) params.dateTo = filters.dateTo

    try {
      const response = await get<PaginatedResponse<Settlement>>('/sellers/me/settlements', params)
      settlements.value = response.items
      pagination.value = {
        total: response.total,
        page: response.page,
        pageSize: response.pageSize,
        totalPages: response.totalPages,
      }
    } catch {
      // Seller-service may not have settlements yet – show empty state
      settlements.value = []
      error.value = null
    }
  }

  async function fetchSettlementTotals(filters: SettlementFilter = {}): Promise<void> {
    const params: Record<string, unknown> = {}
    if (filters.status) params.status = filters.status
    if (filters.dateFrom) params.dateFrom = filters.dateFrom
    if (filters.dateTo) params.dateTo = filters.dateTo

    try {
      totals.value = await get<SettlementTotals>('/sellers/me/settlements/totals', params)
    } catch {
      // Clear error – totals endpoint is optional
      error.value = null
    }
  }

  async function downloadInvoice(settlementId: string): Promise<void> {
    const { url } = await get<{ url: string }>(`/sellers/me/settlements/${settlementId}/invoice`)
    window.open(url, '_blank')
  }

  async function fetchMonthlySettlements(): Promise<
    { month: string; amount: number; count: number }[]
  > {
    return get('/sellers/me/settlements/monthly')
  }

  return {
    settlements,
    pagination,
    totals,
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
