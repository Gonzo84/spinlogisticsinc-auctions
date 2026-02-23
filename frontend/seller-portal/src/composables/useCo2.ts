import { ref } from 'vue'
import { useApi } from './useApi'

export interface Co2Summary {
  totalCo2AvoidedKg: number
  totalLotsContributed: number
  equivalentTreesPlanted: number
  equivalentCarKmAvoided: number
  currency: string
}

export interface Co2LotBreakdown {
  lotId: string
  lotTitle: string
  category: string
  co2AvoidedKg: number
  calculationBasis: string
  soldAt: string
  hammerPrice: number
}

export interface Co2CategoryBreakdown {
  category: string
  co2AvoidedKg: number
  lotCount: number
  percentage: number
}

export interface Co2MonthlyTrend {
  month: string
  co2AvoidedKg: number
  lotCount: number
}

export function useCo2() {
  const { get, loading, error } = useApi()

  const summary = ref<Co2Summary | null>(null)
  const lotBreakdown = ref<Co2LotBreakdown[]>([])
  const categoryBreakdown = ref<Co2CategoryBreakdown[]>([])
  const monthlyTrend = ref<Co2MonthlyTrend[]>([])

  async function fetchSummary(): Promise<void> {
    summary.value = await get<Co2Summary>('/seller/co2/summary')
  }

  async function fetchLotBreakdown(): Promise<void> {
    lotBreakdown.value = await get<Co2LotBreakdown[]>('/seller/co2/lots')
  }

  async function fetchCategoryBreakdown(): Promise<void> {
    categoryBreakdown.value = await get<Co2CategoryBreakdown[]>('/seller/co2/categories')
  }

  async function fetchMonthlyTrend(): Promise<void> {
    monthlyTrend.value = await get<Co2MonthlyTrend[]>('/seller/co2/monthly')
  }

  async function downloadReport(format: 'pdf' | 'csv' = 'pdf'): Promise<void> {
    const { url } = await get<{ url: string }>('/seller/co2/report', { format })
    window.open(url, '_blank')
  }

  async function fetchAll(): Promise<void> {
    await Promise.all([
      fetchSummary(),
      fetchLotBreakdown(),
      fetchCategoryBreakdown(),
      fetchMonthlyTrend(),
    ])
  }

  return {
    summary,
    lotBreakdown,
    categoryBreakdown,
    monthlyTrend,
    loading,
    error,
    fetchSummary,
    fetchLotBreakdown,
    fetchCategoryBreakdown,
    fetchMonthlyTrend,
    downloadReport,
    fetchAll,
  }
}
