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
    try {
      summary.value = await get<Co2Summary>('/co2/summary')
    } catch {
      error.value = null
    }
  }

  async function fetchLotBreakdown(): Promise<void> {
    try {
      lotBreakdown.value = await get<Co2LotBreakdown[]>('/co2/lots')
    } catch {
      error.value = null
    }
  }

  async function fetchCategoryBreakdown(): Promise<void> {
    try {
      categoryBreakdown.value = await get<Co2CategoryBreakdown[]>('/co2/categories')
    } catch {
      error.value = null
    }
  }

  async function fetchMonthlyTrend(): Promise<void> {
    try {
      monthlyTrend.value = await get<Co2MonthlyTrend[]>('/co2/monthly')
    } catch {
      error.value = null
    }
  }

  async function downloadReport(format: 'pdf' | 'csv' = 'pdf'): Promise<void> {
    const { url } = await get<{ url: string }>('/co2/report', { format })
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
