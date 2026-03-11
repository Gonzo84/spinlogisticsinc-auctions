import { ref, reactive, readonly } from 'vue'
import axios from 'axios'
import { useApi } from './useApi'
import { useToast } from 'primevue/usetoast'
import type { Payment, PaymentFilters, PaymentSummary } from '@/types/payment'
import type { PaginationParams } from '@/types/api'

export type { Payment, PaymentStatus, PaymentFilters, PaymentSummary } from '@/types/payment'

function extractErrorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    const msg = (err.response?.data as Record<string, unknown> | undefined)?.message
    return typeof msg === 'string' ? msg : fallback
  }
  return err instanceof Error ? err.message : fallback
}

export function usePayments() {
  const { get, post, patch } = useApi()
  const toast = useToast()

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
      const params: PaginationParams = {
        page: filters.page,
        pageSize: filters.pageSize,
      }
      if (filters.status) params.status = filters.status
      if (filters.search) params.search = filters.search
      if (filters.dateFrom) params.dateFrom = filters.dateFrom
      if (filters.dateTo) params.dateTo = filters.dateTo

      const response = await get<{ items: Record<string, unknown>[]; total: number }>('/payments', { params })
      payments.value = (response.items ?? []).map((item) => ({
        ...item,
        id: (item.paymentId ?? item.id) as string,
        amount: (item.hammerPrice ?? item.amount ?? 0) as number,
        buyerPremium: (item.buyerPremium ?? 0) as number,
        totalAmount: (item.totalAmount ?? 0) as number,
      })) as unknown as Payment[]
      totalCount.value = response.total ?? 0
    } catch (err: unknown) {
      payments.value = []
      totalCount.value = 0
      if (axios.isAxiosError(err) && err.response?.status === 404) {
        console.debug('[usePayments] Payments endpoint not available, showing empty state')
      } else {
        error.value = extractErrorMessage(err, 'Failed to load payments')
        toast.add({ severity: 'error', summary: 'Error', detail: error.value, life: 5000 })
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchSummary(): Promise<void> {
    try {
      summary.value = await get<PaymentSummary>('/payments/summary')
    } catch (err: unknown) {
      if (axios.isAxiosError(err) && err.response?.status === 404) {
        console.debug('[usePayments] Payment summary endpoint not available, showing empty state')
      } else {
        error.value = extractErrorMessage(err, 'Failed to load payment summary')
        toast.add({ severity: 'error', summary: 'Error', detail: error.value, life: 5000 })
      }
    }
  }

  async function manualSettle(paymentId: string, bankReference: string): Promise<boolean> {
    loading.value = true
    error.value = null
    try {
      await patch(`/payments/${paymentId}/settle`, { bankReference })
      return true
    } catch (err: unknown) {
      error.value = extractErrorMessage(err, 'Failed to settle payment')
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
    } catch (err: unknown) {
      if (axios.isAxiosError(err) && err.response?.status === 404) {
        toast.add({ severity: 'info', summary: 'Not Available', detail: 'Refund feature is not yet available', life: 5000 })
      } else {
        error.value = extractErrorMessage(err, 'Failed to refund payment')
        toast.add({ severity: 'error', summary: 'Error', detail: error.value, life: 5000 })
      }
      return false
    } finally {
      loading.value = false
    }
  }

  async function sendReminder(paymentId: string): Promise<boolean> {
    try {
      await post(`/payments/${paymentId}/reminder`)
      return true
    } catch (err: unknown) {
      if (axios.isAxiosError(err) && err.response?.status === 404) {
        toast.add({ severity: 'info', summary: 'Not Available', detail: 'Payment reminder feature is not yet available', life: 5000 })
      } else {
        error.value = extractErrorMessage(err, 'Failed to send reminder')
        toast.add({ severity: 'error', summary: 'Error', detail: error.value, life: 5000 })
      }
      return false
    }
  }

  return {
    payments,
    summary,
    totalCount,
    loading: readonly(loading),
    error: readonly(error),
    filters,
    fetchPayments,
    fetchSummary,
    manualSettle,
    refundPayment,
    sendReminder,
  }
}
