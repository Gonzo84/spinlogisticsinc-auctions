import { ref, reactive } from 'vue'
import { useApi } from './useApi'

// GDPR types
export type GdprRequestType = 'export' | 'erasure'
export type GdprRequestStatus = 'pending' | 'processing' | 'completed' | 'rejected'

export interface GdprRequest {
  id: string
  userId: string
  userName: string
  userEmail: string
  type: GdprRequestType
  status: GdprRequestStatus
  reason: string | null
  requestedAt: string
  processedAt: string | null
  processedBy: string | null
  downloadUrl: string | null
}

export interface GdprFilters {
  type: string
  status: string
  page: number
  pageSize: number
}

// Fraud types
export type FraudSeverity = 'high' | 'medium' | 'low'
export type FraudAlertStatus = 'new' | 'investigating' | 'resolved' | 'false_positive'
export type FraudAlertType = 'shill_bidding' | 'bid_manipulation' | 'account_takeover' | 'payment_fraud' | 'multiple_accounts'

export interface FraudAlert {
  id: string
  type: FraudAlertType
  severity: FraudSeverity
  status: FraudAlertStatus
  title: string
  description: string
  affectedUsers: { id: string; name: string; role: string }[]
  affectedAuction: { id: string; title: string } | null
  affectedLots: { id: string; title: string }[]
  evidence: string[]
  detectedAt: string
  resolvedAt: string | null
  resolvedBy: string | null
  resolution: string | null
}

export interface FraudFilters {
  severity: string
  status: string
  type: string
  page: number
  pageSize: number
}

export function useCompliance() {
  const { get, post, patch } = useApi()

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
      const params: Record<string, any> = {
        page: gdprFilters.page,
        pageSize: gdprFilters.pageSize,
      }
      if (gdprFilters.type) params.type = gdprFilters.type
      if (gdprFilters.status) params.status = gdprFilters.status

      const response = await get<{ items: GdprRequest[]; total: number }>('/compliance/gdpr/requests', { params })
      gdprRequests.value = response.items ?? []
      gdprTotalCount.value = response.total ?? 0
    } catch {
      gdprRequests.value = []
      gdprTotalCount.value = 0
      error.value = null
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
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to approve GDPR request'
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
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to reject GDPR request'
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
      const params: Record<string, any> = {
        page: fraudFilters.page,
        pageSize: fraudFilters.pageSize,
      }
      if (fraudFilters.severity) params.severity = fraudFilters.severity
      if (fraudFilters.status) params.status = fraudFilters.status
      if (fraudFilters.type) params.type = fraudFilters.type

      const response = await get<{ items: FraudAlert[]; total: number }>('/compliance/fraud/alerts', { params })
      fraudAlerts.value = response.items ?? []
      fraudTotalCount.value = response.total ?? 0
    } catch {
      fraudAlerts.value = []
      fraudTotalCount.value = 0
      error.value = null
    } finally {
      loading.value = false
    }
  }

  async function investigateAlert(alertId: string): Promise<boolean> {
    try {
      await patch(`/compliance/fraud/alerts/${alertId}/investigate`)
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to update alert'
      return false
    }
  }

  async function resolveAlert(alertId: string, resolution: string, blockUsers: boolean): Promise<boolean> {
    loading.value = true
    error.value = null
    try {
      await patch(`/compliance/fraud/alerts/${alertId}/resolve`, { resolution, blockUsers })
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to resolve alert'
      return false
    } finally {
      loading.value = false
    }
  }

  async function dismissAlert(alertId: string): Promise<boolean> {
    try {
      await patch(`/compliance/fraud/alerts/${alertId}/dismiss`)
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to dismiss alert'
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
    loading,
    error,
  }
}
