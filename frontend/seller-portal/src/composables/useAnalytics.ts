import { ref, readonly } from 'vue'
import { useApi } from './useApi'
import type {
  SellThroughData,
  PriceVsEstimate,
  CategoryPerformance,
  MonthlyRevenue,
  BuyerDemographic,
  AnalyticsOverview,
  ApiResponse,
  RawAnalyticsResponse,
  RawTopCategory,
  RawMonthlyRevenue,
} from '@/types'

export type {
  SellThroughData,
  PriceVsEstimate,
  CategoryPerformance,
  MonthlyRevenue,
  BuyerDemographic,
  AnalyticsOverview,
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

  /** Unwrap ApiResponse wrapper if present, returning the inner data. */
  function unwrapApiResponse<T>(raw: unknown): T {
    const obj = raw as Record<string, unknown> | null
    if (obj?.data && typeof obj.data === 'object' && !Array.isArray(obj.data)) {
      return obj.data as T
    }
    return raw as T
  }

  async function fetchAnalytics(): Promise<void> {
    try {
      const raw = await get<ApiResponse<RawAnalyticsResponse> | RawAnalyticsResponse>('/sellers/me/analytics')
      // Unwrap ApiResponse wrapper if present
      const data = unwrapApiResponse<RawAnalyticsResponse>(raw)

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
      const cats: RawTopCategory[] = data.topCategories ?? []
      categoryPerformance.value = cats.map((c: RawTopCategory) => ({
        category: c.category ?? 'Unknown',
        lotsListed: c.lotCount ?? 0,
        lotsSold: c.lotCount ?? 0,
        revenue: c.revenue ?? 0,
        sellThroughRate: 100,
        averagePrice: (c.lotCount ?? 0) > 0 ? (c.revenue ?? 0) / (c.lotCount ?? 1) : 0,
      }))

      // Map monthlyRevenue
      const months: RawMonthlyRevenue[] = data.monthlyRevenue ?? []
      monthlyRevenue.value = months.map((m: RawMonthlyRevenue) => ({
        month: m.month ?? '',
        revenue: m.revenue ?? 0,
        lotsSold: m.lotsSold ?? 0,
      }))
    } catch {
      // Analytics endpoint is optional -- clear error and show zeros
      error.value = null
    }
  }

  // Individual fetch functions all delegate to the single endpoint
  async function fetchOverview(): Promise<void> {
    if (!overview.value) await fetchAnalytics()
  }

  async function fetchPriceVsEstimate(): Promise<void> {
    // Not available from seller-service -- keep empty
  }

  async function fetchCategoryPerformance(): Promise<void> {
    if (categoryPerformance.value.length === 0) await fetchAnalytics()
  }

  async function fetchMonthlyRevenue(_months: number = 12): Promise<void> {
    if (monthlyRevenue.value.length === 0) await fetchAnalytics()
  }

  async function fetchBuyerDemographics(): Promise<void> {
    // Not available from seller-service -- keep empty
  }

  async function fetchAll(): Promise<void> {
    await fetchAnalytics()
  }

  return {
    overview: readonly(overview),
    sellThrough: readonly(sellThrough),
    priceVsEstimate: readonly(priceVsEstimate),
    categoryPerformance: readonly(categoryPerformance),
    monthlyRevenue: readonly(monthlyRevenue),
    buyerDemographics: readonly(buyerDemographics),
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
