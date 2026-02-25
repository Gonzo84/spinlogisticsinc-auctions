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
    try {
      overview.value = await get<AnalyticsOverview>('/sellers/me/analytics/overview')
      sellThrough.value = overview.value.sellThrough
    } catch {
      error.value = null
    }
  }

  async function fetchPriceVsEstimate(): Promise<void> {
    try {
      priceVsEstimate.value = await get<PriceVsEstimate[]>('/sellers/me/analytics/price-vs-estimate')
    } catch {
      error.value = null
    }
  }

  async function fetchCategoryPerformance(): Promise<void> {
    try {
      categoryPerformance.value = await get<CategoryPerformance[]>(
        '/sellers/me/analytics/category-performance',
      )
    } catch {
      error.value = null
    }
  }

  async function fetchMonthlyRevenue(months: number = 12): Promise<void> {
    try {
      monthlyRevenue.value = await get<MonthlyRevenue[]>('/sellers/me/analytics/monthly-revenue', {
        months,
      })
    } catch {
      error.value = null
    }
  }

  async function fetchBuyerDemographics(): Promise<void> {
    try {
      buyerDemographics.value = await get<BuyerDemographic[]>('/sellers/me/analytics/buyer-demographics')
    } catch {
      error.value = null
    }
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
