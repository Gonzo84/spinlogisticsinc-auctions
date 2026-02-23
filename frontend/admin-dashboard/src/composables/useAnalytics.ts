import { ref } from 'vue'
import { useApi } from './useApi'

export interface PlatformOverview {
  totalRevenue: number
  totalAuctions: number
  totalLotsSold: number
  totalRegisteredUsers: number
  totalBids: number
  averageHammerPrice: number
  sellThroughRate: number
  currency: string
}

export interface MonthlyRevenue {
  month: string
  revenue: number
  commission: number
  lotsSold: number
}

export interface RegistrationTrend {
  month: string
  buyers: number
  sellers: number
  total: number
}

export interface CategoryPopularity {
  category: string
  lotCount: number
  bidCount: number
  revenue: number
  sellThroughRate: number
  avgPrice: number
}

export interface DailyBidVolume {
  date: string
  bids: number
  uniqueBidders: number
}

export function useAnalytics() {
  const { get } = useApi()

  const overview = ref<PlatformOverview | null>(null)
  const monthlyRevenue = ref<MonthlyRevenue[]>([])
  const registrationTrends = ref<RegistrationTrend[]>([])
  const categoryPopularity = ref<CategoryPopularity[]>([])
  const dailyBidVolume = ref<DailyBidVolume[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchOverview(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      overview.value = await get<PlatformOverview>('/admin/analytics/overview')
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to fetch analytics overview'
    } finally {
      loading.value = false
    }
  }

  async function fetchMonthlyRevenue(months = 12): Promise<void> {
    try {
      monthlyRevenue.value = await get<MonthlyRevenue[]>('/admin/analytics/revenue', {
        params: { months },
      })
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to fetch revenue data'
    }
  }

  async function fetchRegistrationTrends(months = 12): Promise<void> {
    try {
      registrationTrends.value = await get<RegistrationTrend[]>('/admin/analytics/registrations', {
        params: { months },
      })
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to fetch registration data'
    }
  }

  async function fetchCategoryPopularity(): Promise<void> {
    try {
      categoryPopularity.value = await get<CategoryPopularity[]>('/admin/analytics/categories')
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to fetch category data'
    }
  }

  async function fetchDailyBidVolume(days = 30): Promise<void> {
    try {
      dailyBidVolume.value = await get<DailyBidVolume[]>('/admin/analytics/bids/daily', {
        params: { days },
      })
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to fetch bid volume'
    }
  }

  async function fetchAll(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      await Promise.all([
        fetchOverview(),
        fetchMonthlyRevenue(),
        fetchRegistrationTrends(),
        fetchCategoryPopularity(),
        fetchDailyBidVolume(),
      ])
    } finally {
      loading.value = false
    }
  }

  return {
    overview,
    monthlyRevenue,
    registrationTrends,
    categoryPopularity,
    dailyBidVolume,
    loading,
    error,
    fetchOverview,
    fetchMonthlyRevenue,
    fetchRegistrationTrends,
    fetchCategoryPopularity,
    fetchDailyBidVolume,
    fetchAll,
  }
}
