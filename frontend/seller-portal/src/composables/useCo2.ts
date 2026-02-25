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

/**
 * Seller-service has: GET /sellers/me/co2-report
 * Returns: { sellerId, totalCo2SavedKg, totalLotsContributed,
 *            averageCo2PerLotKg, equivalentTreesPlanted, reportPeriod, generatedAt }
 *
 * Co2-service has: GET /co2/summary (platform-wide)
 *                  GET /co2/sellers/{sellerId} (per-seller summary)
 *
 * We map these to what the CO2 Report view expects.
 */
export function useCo2() {
  const { get, loading, error } = useApi()

  const summary = ref<Co2Summary | null>(null)
  const lotBreakdown = ref<Co2LotBreakdown[]>([])
  const categoryBreakdown = ref<Co2CategoryBreakdown[]>([])
  const monthlyTrend = ref<Co2MonthlyTrend[]>([])

  async function fetchSummary(): Promise<void> {
    try {
      const raw = await get<any>('/sellers/me/co2-report')
      // Unwrap ApiResponse wrapper if present
      const data = raw?.data && typeof raw.data === 'object' && !Array.isArray(raw.data) ? raw.data : raw

      summary.value = {
        totalCo2AvoidedKg: data.totalCo2SavedKg ?? 0,
        totalLotsContributed: data.totalLotsContributed ?? 0,
        equivalentTreesPlanted: data.equivalentTreesPlanted ?? 0,
        equivalentCarKmAvoided: Math.round((data.totalCo2SavedKg ?? 0) * 6), // ~6 km per kg CO2
        currency: 'EUR',
      }
    } catch {
      // CO2 report not available yet – show empty state
      error.value = null
      summary.value = {
        totalCo2AvoidedKg: 0,
        totalLotsContributed: 0,
        equivalentTreesPlanted: 0,
        equivalentCarKmAvoided: 0,
        currency: 'EUR',
      }
    }
  }

  async function fetchLotBreakdown(): Promise<void> {
    // Per-lot CO2 data not available from seller-service – keep empty
  }

  async function fetchCategoryBreakdown(): Promise<void> {
    // Category breakdown not available from seller-service – keep empty
  }

  async function fetchMonthlyTrend(): Promise<void> {
    // Monthly trend not available from seller-service – keep empty
  }

  async function downloadReport(_format: 'pdf' | 'csv' = 'pdf'): Promise<void> {
    // Report download not available – no-op
  }

  async function fetchAll(): Promise<void> {
    await fetchSummary()
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
