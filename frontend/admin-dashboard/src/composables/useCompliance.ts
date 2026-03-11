import { ref, reactive, readonly } from 'vue'
import axios from 'axios'
import { useApi } from './useApi'
import { useErrorHandler } from './useErrorHandler'
import type { GdprRequest, GdprFilters, FraudAlert, FraudFilters } from '@/types/compliance'
import type { PaginationParams } from '@/types/api'

export type {
  GdprRequest,
  GdprRequestType,
  GdprRequestStatus,
  GdprFilters,
  FraudAlert,
  FraudSeverity,
  FraudAlertStatus,
  FraudAlertType,
  FraudFilters,
} from '@/types/compliance'

function extractErrorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    const msg = (err.response?.data as Record<string, unknown> | undefined)?.message
    return typeof msg === 'string' ? msg : fallback
  }
  return err instanceof Error ? err.message : fallback
}

export function useCompliance() {
  const { get, patch } = useApi()
  const { handleApiError, handleGracefulDegradation, is404 } = useErrorHandler()

  // GDPR state
  const gdprRequests = ref<GdprRequest[]>([])
  const gdprTotalCount = ref(0)
  const gdprFilters = reactive<GdprFilters>({
    type: '',
    status: '',
    page: 1,
    pageSize: 20,
  })

  // Fraud state
  const fraudAlerts = ref<FraudAlert[]>([])
  const fraudTotalCount = ref(0)
  const fraudFilters = reactive<FraudFilters>({
    severity: '',
    status: '',
    type: '',
    page: 1,
    pageSize: 20,
  })

  const loading = ref(false)
  const error = ref<string | null>(null)

  // GDPR methods
  async function fetchGdprRequests(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const params: PaginationParams = {
        page: gdprFilters.page,
        pageSize: gdprFilters.pageSize,
      }
      if (gdprFilters.type) params.type = gdprFilters.type
      if (gdprFilters.status) params.status = gdprFilters.status

      const response = await get<{ items: GdprRequest[]; total: number }>('/compliance/gdpr/requests', { params })
      gdprRequests.value = response.items ?? []
      gdprTotalCount.value = response.total ?? 0
    } catch (err: unknown) {
      gdprRequests.value = []
      gdprTotalCount.value = 0
      if (is404(err)) {
        handleGracefulDegradation('fetchGdprRequests')
      } else {
        error.value = handleApiError(err, 'Failed to load GDPR requests')
      }
    } finally {
      loading.value = false
    }
  }

  async function approveGdprRequest(requestId: string): Promise<boolean> {
    loading.value = true
    error.value = null
    try {
      await patch(`/compliance/gdpr/requests/${requestId}/approve`)
      return true
    } catch (err: unknown) {
      error.value = extractErrorMessage(err, 'Failed to approve GDPR request')
      return false
    } finally {
      loading.value = false
    }
  }

  async function rejectGdprRequest(requestId: string, reason: string): Promise<boolean> {
    loading.value = true
    error.value = null
    try {
      await patch(`/compliance/gdpr/requests/${requestId}/reject`, { reason })
      return true
    } catch (err: unknown) {
      error.value = extractErrorMessage(err, 'Failed to reject GDPR request')
      return false
    } finally {
      loading.value = false
    }
  }

  // Fraud methods
  async function fetchFraudAlerts(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const params: PaginationParams = {
        page: fraudFilters.page,
        pageSize: fraudFilters.pageSize,
      }
      if (fraudFilters.severity) params.severity = fraudFilters.severity
      if (fraudFilters.status) params.status = fraudFilters.status
      if (fraudFilters.type) params.type = fraudFilters.type

      const response = await get<{ items: FraudAlert[]; total: number }>('/compliance/fraud/alerts', { params })
      fraudAlerts.value = response.items ?? []
      fraudTotalCount.value = response.total ?? 0
    } catch (err: unknown) {
      fraudAlerts.value = []
      fraudTotalCount.value = 0
      if (is404(err)) {
        handleGracefulDegradation('fetchFraudAlerts')
      } else {
        error.value = handleApiError(err, 'Failed to load fraud alerts')
      }
    } finally {
      loading.value = false
    }
  }

  async function investigateAlert(alertId: string): Promise<boolean> {
    try {
      await patch(`/compliance/fraud/alerts/${alertId}/investigate`)
      return true
    } catch (err: unknown) {
      error.value = extractErrorMessage(err, 'Failed to update alert')
      return false
    }
  }

  async function resolveAlert(alertId: string, resolution: string, blockUsers: boolean): Promise<boolean> {
    loading.value = true
    error.value = null
    try {
      await patch(`/compliance/fraud/alerts/${alertId}/resolve`, { resolution, blockUsers })
      return true
    } catch (err: unknown) {
      error.value = extractErrorMessage(err, 'Failed to resolve alert')
      return false
    } finally {
      loading.value = false
    }
  }

  async function dismissAlert(alertId: string): Promise<boolean> {
    try {
      await patch(`/compliance/fraud/alerts/${alertId}/dismiss`)
      return true
    } catch (err: unknown) {
      error.value = extractErrorMessage(err, 'Failed to dismiss alert')
      return false
    }
  }

  return {
    // GDPR
    gdprRequests,
    gdprTotalCount,
    gdprFilters,
    fetchGdprRequests,
    approveGdprRequest,
    rejectGdprRequest,
    // Fraud
    fraudAlerts,
    fraudTotalCount,
    fraudFilters,
    fetchFraudAlerts,
    investigateAlert,
    resolveAlert,
    dismissAlert,
    // Shared
    loading: readonly(loading),
    error: readonly(error),
  }
}
