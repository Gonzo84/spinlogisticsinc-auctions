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
      const raw = await get<PlatformOverview | { data: PlatformOverview }>('/analytics/overview')
      overview.value = (raw as Record<string, unknown>)?.data
        ? (raw as { data: PlatformOverview }).data
        : raw as PlatformOverview
    } catch {
      overview.value = null
    } finally {
      loading.value = false
    }
  }

  async function fetchMonthlyRevenue(months = 12): Promise<void> {
    try {
      const raw = await get<MonthlyRevenue[] | { data: MonthlyRevenue[] }>('/analytics/revenue', {
        params: { months },
      })
      const result = Array.isArray(raw) ? raw : (Array.isArray((raw as Record<string, unknown>)?.data) ? (raw as { data: MonthlyRevenue[] }).data : [])
      monthlyRevenue.value = result
    } catch {
      monthlyRevenue.value = []
    }
  }

  async function fetchRegistrationTrends(months = 12): Promise<void> {
    try {
      const raw = await get<RegistrationTrend[] | { data: RegistrationTrend[] }>('/analytics/registrations', {
        params: { months },
      })
      const result = Array.isArray(raw) ? raw : (Array.isArray((raw as Record<string, unknown>)?.data) ? (raw as { data: RegistrationTrend[] }).data : [])
      registrationTrends.value = result
    } catch {
      registrationTrends.value = []
    }
  }

  async function fetchCategoryPopularity(): Promise<void> {
    try {
      const raw = await get<CategoryPopularity[] | { data: CategoryPopularity[] }>('/analytics/categories')
      const result = Array.isArray(raw) ? raw : (Array.isArray((raw as Record<string, unknown>)?.data) ? (raw as { data: CategoryPopularity[] }).data : [])
      categoryPopularity.value = result
    } catch {
      categoryPopularity.value = []
    }
  }

  async function fetchDailyBidVolume(days = 30): Promise<void> {
    try {
      const raw = await get<DailyBidVolume[] | { data: DailyBidVolume[] }>('/analytics/bids/daily', {
        params: { days },
      })
      const result = Array.isArray(raw) ? raw : (Array.isArray((raw as Record<string, unknown>)?.data) ? (raw as { data: DailyBidVolume[] }).data : [])
      dailyBidVolume.value = result
    } catch {
      dailyBidVolume.value = []
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
