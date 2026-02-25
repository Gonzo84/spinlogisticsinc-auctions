import { ref } from 'vue'
import { useApi } from './useApi'

export interface SellThroughData {
  totalListed: number
  totalSold: number
  rate: number
}

export interface PriceVsEstimate {
  category: string
  averageEstimate: number
  averageHammerPrice: number
  ratio: number
}

export interface CategoryPerformance {
  category: string
  lotsListed: number
  lotsSold: number
  revenue: number
  sellThroughRate: number
  averagePrice: number
}

export interface MonthlyRevenue {
  month: string
  revenue: number
  lotsSold: number
}

export interface BuyerDemographic {
  country: string
  countryCode: string
  buyerCount: number
  totalSpent: number
  percentage: number
}

export interface AnalyticsOverview {
  sellThrough: SellThroughData
  totalRevenue: number
  averageHammerPrice: number
  totalBids: number
  averageBidsPerLot: number
  currency: string
}

/**
 * Seller-service has a single analytics endpoint: GET /sellers/me/analytics
 * returning: { totalLots, totalSold, sellThroughRate, averageHammerPrice,
 *              totalRevenue, totalCommissionPaid, topCategories[], monthlyRevenue[] }
 *
 * We map this single response to the various refs that the UI views expect.
 */
export function useAnalytics() {
  const { get, loading, error } = useApi()

  const overview = ref<AnalyticsOverview | null>(null)
  const sellThrough = ref<SellThroughData | null>(null)
  const priceVsEstimate = ref<PriceVsEstimate[]>([])
  const categoryPerformance = ref<CategoryPerformance[]>([])
  const monthlyRevenue = ref<MonthlyRevenue[]>([])
  const buyerDemographics = ref<BuyerDemographic[]>([])

  async function fetchAnalytics(): Promise<void> {
    try {
      const raw = await get<any>('/sellers/me/analytics')
      // Unwrap ApiResponse wrapper if present
      const data = raw?.data && typeof raw.data === 'object' && !Array.isArray(raw.data) ? raw.data : raw

      // Map to overview
      const totalLots = data.totalLots ?? 0
      const totalSold = data.totalSold ?? 0
      const sellThroughRate = data.sellThroughRate ?? 0
      const avgHammerPrice = data.averageHammerPrice ?? 0
      const totalRev = data.totalRevenue ?? 0

      overview.value = {
        sellThrough: {
          totalListed: totalLots,
          totalSold: totalSold,
          rate: sellThroughRate,
        },
        totalRevenue: totalRev,
        averageHammerPrice: avgHammerPrice,
        totalBids: 0, // Not available from seller-service analytics
        averageBidsPerLot: 0,
        currency: 'EUR',
      }

      sellThrough.value = {
        totalListed: totalLots,
        totalSold: totalSold,
        rate: sellThroughRate,
      }

      // Map topCategories to categoryPerformance
      const cats = data.topCategories ?? []
      categoryPerformance.value = cats.map((c: any) => ({
        category: c.category ?? 'Unknown',
        lotsListed: c.lotCount ?? 0,
        lotsSold: c.lotCount ?? 0,
        revenue: c.revenue ?? 0,
        sellThroughRate: 100,
        averagePrice: c.lotCount > 0 ? (c.revenue ?? 0) / c.lotCount : 0,
      }))

      // Map monthlyRevenue
      const months = data.monthlyRevenue ?? []
      monthlyRevenue.value = months.map((m: any) => ({
        month: m.month ?? '',
        revenue: m.revenue ?? 0,
        lotsSold: m.lotsSold ?? 0,
      }))
    } catch {
      // Analytics endpoint is optional – clear error and show zeros
      error.value = null
    }
  }

  // Individual fetch functions all delegate to the single endpoint
  async function fetchOverview(): Promise<void> {
    if (!overview.value) await fetchAnalytics()
  }

  async function fetchPriceVsEstimate(): Promise<void> {
    // Not available from seller-service – keep empty
  }

  async function fetchCategoryPerformance(): Promise<void> {
    if (categoryPerformance.value.length === 0) await fetchAnalytics()
  }

  async function fetchMonthlyRevenue(_months: number = 12): Promise<void> {
    if (monthlyRevenue.value.length === 0) await fetchAnalytics()
  }

  async function fetchBuyerDemographics(): Promise<void> {
    // Not available from seller-service – keep empty
  }

  async function fetchAll(): Promise<void> {
    await fetchAnalytics()
  }

  return {
    overview,
    sellThrough,
    priceVsEstimate,
    categoryPerformance,
    monthlyRevenue,
    buyerDemographics,
    loading,
    error,
    fetchOverview,
    fetchPriceVsEstimate,
    fetchCategoryPerformance,
    fetchMonthlyRevenue,
    fetchBuyerDemographics,
    fetchAll,
  }
}
