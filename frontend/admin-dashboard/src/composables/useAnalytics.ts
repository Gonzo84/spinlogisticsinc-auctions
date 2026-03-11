import { ref, readonly } from 'vue'
import { useApi } from './useApi'
import { useErrorHandler } from './useErrorHandler'
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
  const { handleApiError, handleGracefulDegradation, is404 } = useErrorHandler()

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
    } catch (err: unknown) {
      overview.value = null
      if (is404(err)) {
        handleGracefulDegradation('fetchOverview')
      } else {
        error.value = handleApiError(err, 'Failed to load platform overview')
      }
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
    } catch (err: unknown) {
      monthlyRevenue.value = []
      if (is404(err)) {
        handleGracefulDegradation('fetchMonthlyRevenue')
      } else {
        handleApiError(err, 'Failed to load monthly revenue')
      }
    }
  }

  async function fetchRegistrationTrends(months = 12): Promise<void> {
    try {
      const raw = await get<RegistrationTrend[] | { data: RegistrationTrend[] }>('/analytics/registrations', {
        params: { months },
      })
      const result = Array.isArray(raw) ? raw : (Array.isArray((raw as Record<string, unknown>)?.data) ? (raw as { data: RegistrationTrend[] }).data : [])
      registrationTrends.value = result
    } catch (err: unknown) {
      registrationTrends.value = []
      if (is404(err)) {
        handleGracefulDegradation('fetchRegistrationTrends')
      } else {
        handleApiError(err, 'Failed to load registration trends')
      }
    }
  }

  async function fetchCategoryPopularity(): Promise<void> {
    try {
      const raw = await get<CategoryPopularity[] | { data: CategoryPopularity[] }>('/analytics/categories')
      const result = Array.isArray(raw) ? raw : (Array.isArray((raw as Record<string, unknown>)?.data) ? (raw as { data: CategoryPopularity[] }).data : [])
      categoryPopularity.value = result
    } catch (err: unknown) {
      categoryPopularity.value = []
      if (is404(err)) {
        handleGracefulDegradation('fetchCategoryPopularity')
      } else {
        handleApiError(err, 'Failed to load category popularity')
      }
    }
  }

  async function fetchDailyBidVolume(days = 30): Promise<void> {
    try {
      const raw = await get<DailyBidVolume[] | { data: DailyBidVolume[] }>('/analytics/bids/daily', {
        params: { days },
      })
      const result = Array.isArray(raw) ? raw : (Array.isArray((raw as Record<string, unknown>)?.data) ? (raw as { data: DailyBidVolume[] }).data : [])
      dailyBidVolume.value = result
    } catch (err: unknown) {
      dailyBidVolume.value = []
      if (is404(err)) {
        handleGracefulDegradation('fetchDailyBidVolume')
      } else {
        handleApiError(err, 'Failed to load daily bid volume')
      }
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
