import { ref, reactive, readonly } from 'vue'
import axios from 'axios'
import { useApi } from './useApi'
import type { User, UserDetail, UserFilters } from '@/types/user'
import type { ApiResponse, PagedResponse, PaginationParams } from '@/types/api'

export type { User, UserDetail, KycEvent, BidRecord, PaymentRecord, UserFilters } from '@/types/user'

function extractErrorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    const msg = (err.response?.data as Record<string, unknown> | undefined)?.message
    return typeof msg === 'string' ? msg : fallback
  }
  return err instanceof Error ? err.message : fallback
}

export function useUsers() {
  const { get, put, post } = useApi()

  const users = ref<User[]>([])
  const currentUser = ref<UserDetail | null>(null)
  const totalCount = ref(0)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const filters = reactive<UserFilters>({
    search: '',
    accountType: '',
    status: '',
    kycStatus: '',
    page: 1,
    pageSize: 20,
  })

  async function fetchUsers(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const params: PaginationParams = {
        page: filters.page,
        pageSize: filters.pageSize,
      }
      if (filters.search) params.search = filters.search
      if (filters.accountType) params.accountType = filters.accountType
      if (filters.status) params.status = filters.status
      if (filters.kycStatus) params.kycStatus = filters.kycStatus

      const raw = await get<ApiResponse<PagedResponse<User>>>('/users', { params })
      // Unwrap ApiResponse wrapper: { data: { items: [...], total: N } }
      const response = raw?.data && typeof raw.data === 'object' ? raw.data : (raw as unknown as PagedResponse<User>)
      users.value = response.items ?? []
      totalCount.value = response.total ?? 0
    } catch {
      // User-service may not support list endpoint yet -- show empty list
      users.value = []
      totalCount.value = 0
      error.value = null
    } finally {
      loading.value = false
    }
  }

  async function fetchUser(id: string): Promise<void> {
    loading.value = true
    error.value = null
    try {
      currentUser.value = await get<UserDetail>(`/users/${id}`)
    } catch (err: unknown) {
      error.value = extractErrorMessage(err, 'Failed to fetch user')
    } finally {
      loading.value = false
    }
  }

  async function blockUser(id: string, reason: string): Promise<boolean> {
    loading.value = true
    error.value = null
    try {
      await put(`/users/${id}/status`, { status: 'blocked', reason })
      if (currentUser.value?.id === id) {
        currentUser.value.status = 'blocked'
      }
      return true
    } catch (err: unknown) {
      error.value = extractErrorMessage(err, 'Failed to block user')
      return false
    } finally {
      loading.value = false
    }
  }

  async function unblockUser(id: string): Promise<boolean> {
    loading.value = true
    error.value = null
    try {
      await put(`/users/${id}/status`, { status: 'active' })
      if (currentUser.value?.id === id) {
        currentUser.value.status = 'active'
      }
      return true
    } catch (err: unknown) {
      error.value = extractErrorMessage(err, 'Failed to unblock user')
      return false
    } finally {
      loading.value = false
    }
  }

  async function triggerGdprExport(userId: string): Promise<boolean> {
    try {
      await post('/compliance/gdpr/export-request', { userId })
      return true
    } catch (err: unknown) {
      error.value = extractErrorMessage(err, 'Failed to trigger GDPR export')
      return false
    }
  }

  async function triggerGdprErasure(userId: string, reason: string): Promise<boolean> {
    try {
      await post('/compliance/gdpr/erasure-request', { userId, reason })
      return true
    } catch (err: unknown) {
      error.value = extractErrorMessage(err, 'Failed to trigger GDPR erasure')
      return false
    }
  }

  return {
    users,
    currentUser,
    totalCount,
    loading: readonly(loading),
    error: readonly(error),
    filters,
    fetchUsers,
    fetchUser,
    blockUser,
    unblockUser,
    triggerGdprExport,
    triggerGdprErasure,
  }
}
