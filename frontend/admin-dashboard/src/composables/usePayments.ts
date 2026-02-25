import { ref, reactive } from 'vue'
import { useApi } from './useApi'

export type PaymentStatus = 'pending' | 'paid' | 'overdue' | 'refunded' | 'disputed'

export interface Payment {
  id: string
  auctionTitle: string
  lotTitle: string
  lotId: string
  buyerName: string
  buyerId: string
  sellerName: string
  sellerId: string
  amount: number
  buyerPremium: number
  totalAmount: number
  currency: string
  status: PaymentStatus
  dueDate: string
  paidDate: string | null
  createdAt: string
}

export interface PaymentFilters {
  status: string
  search: string
  dateFrom: string
  dateTo: string
  page: number
  pageSize: number
}

export interface PaymentSummary {
  totalPending: number
  totalOverdue: number
  totalPaid: number
  totalDisputed: number
  pendingCount: number
  overdueCount: number
}

export function usePayments() {
  const { get, post, patch } = useApi()

  const payments = ref<Payment[]>([])
  const summary = ref<PaymentSummary | null>(null)
  const totalCount = ref(0)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const filters = reactive<PaymentFilters>({
    status: '',
    search: '',
    dateFrom: '',
    dateTo: '',
    page: 1,
    pageSize: 20,
  })

  async function fetchPayments(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const params: Record<string, any> = {
        page: filters.page,
        pageSize: filters.pageSize,
      }
      if (filters.status) params.status = filters.status
      if (filters.search) params.search = filters.search
      if (filters.dateFrom) params.dateFrom = filters.dateFrom
      if (filters.dateTo) params.dateTo = filters.dateTo

      const response = await get<{ items: Payment[]; total: number }>('/payments', { params })
      payments.value = response.items ?? []
      totalCount.value = response.total ?? 0
    } catch {
      payments.value = []
      totalCount.value = 0
      error.value = null
    } finally {
      loading.value = false
    }
  }

  async function fetchSummary(): Promise<void> {
    try {
      summary.value = await get<PaymentSummary>('/payments/summary')
    } catch {
      error.value = null
    }
  }

  async function manualSettle(paymentId: string, bankReference: string): Promise<boolean> {
    loading.value = true
    error.value = null
    try {
      await patch(`/payments/${paymentId}/settle`, { bankReference })
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to settle payment'
      return false
    } finally {
      loading.value = false
    }
  }

  async function refundPayment(paymentId: string, reason: string): Promise<boolean> {
    loading.value = true
    error.value = null
    try {
      await post(`/payments/${paymentId}/refund`, { reason })
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to refund payment'
      return false
    } finally {
      loading.value = false
    }
  }

  async function sendReminder(paymentId: string): Promise<boolean> {
    try {
      await post(`/payments/${paymentId}/reminder`)
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to send reminder'
      return false
    }
  }

  return {
    payments,
    summary,
    totalCount,
    loading,
    error,
    filters,
    fetchPayments,
    fetchSummary,
    manualSettle,
    refundPayment,
    sendReminder,
  }
}
