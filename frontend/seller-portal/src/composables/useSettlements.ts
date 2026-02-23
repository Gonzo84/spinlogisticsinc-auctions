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

    const response = await get<PaginatedResponse<Settlement>>('/seller/settlements', params)
    settlements.value = response.items
    pagination.value = {
      total: response.total,
      page: response.page,
      pageSize: response.pageSize,
      totalPages: response.totalPages,
    }
  }

  async function fetchSettlementTotals(filters: SettlementFilter = {}): Promise<void> {
    const params: Record<string, unknown> = {}
    if (filters.status) params.status = filters.status
    if (filters.dateFrom) params.dateFrom = filters.dateFrom
    if (filters.dateTo) params.dateTo = filters.dateTo

    totals.value = await get<SettlementTotals>('/seller/settlements/totals', params)
  }

  async function downloadInvoice(settlementId: string): Promise<void> {
    const { url } = await get<{ url: string }>(`/seller/settlements/${settlementId}/invoice`)
    window.open(url, '_blank')
  }

  async function fetchMonthlySettlements(): Promise<
    { month: string; amount: number; count: number }[]
  > {
    return get('/seller/settlements/monthly')
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
