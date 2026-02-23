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

export function useAnalytics() {
  const { get, loading, error } = useApi()

  const overview = ref<AnalyticsOverview | null>(null)
  const sellThrough = ref<SellThroughData | null>(null)
  const priceVsEstimate = ref<PriceVsEstimate[]>([])
  const categoryPerformance = ref<CategoryPerformance[]>([])
  const monthlyRevenue = ref<MonthlyRevenue[]>([])
  const buyerDemographics = ref<BuyerDemographic[]>([])

  async function fetchOverview(): Promise<void> {
    overview.value = await get<AnalyticsOverview>('/seller/analytics/overview')
    sellThrough.value = overview.value.sellThrough
  }

  async function fetchPriceVsEstimate(): Promise<void> {
    priceVsEstimate.value = await get<PriceVsEstimate[]>('/seller/analytics/price-vs-estimate')
  }

  async function fetchCategoryPerformance(): Promise<void> {
    categoryPerformance.value = await get<CategoryPerformance[]>(
      '/seller/analytics/category-performance',
    )
  }

  async function fetchMonthlyRevenue(months: number = 12): Promise<void> {
    monthlyRevenue.value = await get<MonthlyRevenue[]>('/seller/analytics/monthly-revenue', {
      months,
    })
  }

  async function fetchBuyerDemographics(): Promise<void> {
    buyerDemographics.value = await get<BuyerDemographic[]>('/seller/analytics/buyer-demographics')
  }

  async function fetchAll(): Promise<void> {
    await Promise.all([
      fetchOverview(),
      fetchPriceVsEstimate(),
      fetchCategoryPerformance(),
      fetchMonthlyRevenue(),
      fetchBuyerDemographics(),
    ])
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
