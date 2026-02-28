import { ref, readonly } from 'vue'
import { useApi } from './useApi'
import type {
  PlatformOverview,
  MonthlyRevenue,
  RegistrationTrend,
  CategoryPopularity,
  DailyBidVolume,
} from '@/types/analytics'

export type {
  PlatformOverview,
  MonthlyRevenue,
  RegistrationTrend,
  CategoryPopularity,
  DailyBidVolume,
} from '@/types/analytics'

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
      overview.value = await get<PlatformOverview>('/analytics/overview')
    } catch {
      error.value = null
    } finally {
      loading.value = false
    }
  }

  async function fetchMonthlyRevenue(months = 12): Promise<void> {
    try {
      monthlyRevenue.value = await get<MonthlyRevenue[]>('/analytics/revenue', {
        params: { months },
      })
    } catch {
      error.value = null
    }
  }

  async function fetchRegistrationTrends(months = 12): Promise<void> {
    try {
      registrationTrends.value = await get<RegistrationTrend[]>('/analytics/registrations', {
        params: { months },
      })
    } catch {
      error.value = null
    }
  }

  async function fetchCategoryPopularity(): Promise<void> {
    try {
      categoryPopularity.value = await get<CategoryPopularity[]>('/analytics/categories')
    } catch {
      error.value = null
    }
  }

  async function fetchDailyBidVolume(days = 30): Promise<void> {
    try {
      dailyBidVolume.value = await get<DailyBidVolume[]>('/analytics/bids/daily', {
        params: { days },
      })
    } catch {
      error.value = null
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
    loading: readonly(loading),
    error: readonly(error),
    fetchOverview,
    fetchMonthlyRevenue,
    fetchRegistrationTrends,
    fetchCategoryPopularity,
    fetchDailyBidVolume,
    fetchAll,
  }
}
