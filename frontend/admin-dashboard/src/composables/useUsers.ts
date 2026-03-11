import { ref, reactive, readonly } from 'vue'
import axios from 'axios'
import { useApi } from './useApi'
import { useErrorHandler } from './useErrorHandler'
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
  const { handleGracefulDegradation, is404: isNotFound } = useErrorHandler()

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
      if (filters.status) params.status = filters.status

      const raw = await get<ApiResponse<PagedResponse<User>>>('/users', { params })
      // Unwrap ApiResponse wrapper: { data: { items: [...], total: N } }
      const response = raw?.data && typeof raw.data === 'object' ? raw.data : (raw as unknown as PagedResponse<User>)
      users.value = response.items ?? []
      totalCount.value = response.total ?? 0
    } catch (err: unknown) {
      users.value = []
      totalCount.value = 0
      if (isNotFound(err)) {
        // User-service may not support list endpoint yet -- show empty list
        handleGracefulDegradation('fetchUsers')
      } else {
        error.value = extractErrorMessage(err, 'Failed to load users')
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchUser(id: string): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const raw = await get<ApiResponse<Record<string, unknown>> | UserDetail>(`/users/${id}`)
      // Unwrap ApiResponse wrapper if present: { data: { user, company, deposit } }
      const unwrapped = (raw as ApiResponse<Record<string, unknown>>)?.data ?? raw
      const profile = unwrapped as Record<string, unknown>
      const user = (profile?.user ?? profile) as Record<string, unknown>
      currentUser.value = {
        id: (user?.id as string) ?? id,
        email: (user?.email as string) ?? '',
        firstName: (user?.firstName as string) ?? '',
        lastName: (user?.lastName as string) ?? '',
        companyName: (user?.companyName as string) ?? ((profile?.company as Record<string, unknown>)?.name as string) ?? '',
        accountType: (user?.accountType as UserDetail['accountType']) ?? 'buyer',
        status: (user?.status as UserDetail['status']) ?? 'active',
        kycStatus: (user?.kycStatus as UserDetail['kycStatus']) ?? 'not_started',
        depositStatus: (user?.depositStatus as UserDetail['depositStatus']) ?? 'none',
        registeredAt: (user?.registeredAt as string) ?? (user?.createdAt as string) ?? '',
        lastLoginAt: (user?.lastLoginAt as string) ?? '',
        phone: (user?.phone as string) ?? '',
        vatNumber: (user?.vatNumber as string) ?? ((profile?.company as Record<string, unknown>)?.vatNumber as string) ?? '',
        address: (user?.address as UserDetail['address']) ?? { street: '', city: '', postalCode: '', country: '' },
        kycHistory: (user?.kycHistory as UserDetail['kycHistory']) ?? [],
        bidHistory: (user?.bidHistory as UserDetail['bidHistory']) ?? [],
        paymentHistory: (user?.paymentHistory as UserDetail['paymentHistory']) ?? [],
      }
    } catch (err: unknown) {
      // If 404, try looking up by Keycloak ID (sellerId from catalog is a Keycloak UUID)
      if (axios.isAxiosError(err) && err.response?.status === 404) {
        try {
          const raw = await get<ApiResponse<Record<string, unknown>>>(`/users/by-keycloak-id/${id}`)
          // Response is ApiResponse<UserProfileResponse> with shape { data: { user, company, deposit } }
          const data = raw?.data ?? raw
          const profile = data as Record<string, unknown>
          const user = (profile?.user ?? profile) as Record<string, unknown>
          currentUser.value = {
            id: (user?.id as string) ?? id,
            email: (user?.email as string) ?? '',
            firstName: (user?.firstName as string) ?? '',
            lastName: (user?.lastName as string) ?? '',
            companyName: (user?.companyName as string) ?? ((profile?.company as Record<string, unknown>)?.name as string) ?? '',
            accountType: (user?.accountType as UserDetail['accountType']) ?? 'buyer',
            status: (user?.status as UserDetail['status']) ?? 'active',
            kycStatus: (user?.kycStatus as UserDetail['kycStatus']) ?? 'not_started',
            depositStatus: (user?.depositStatus as UserDetail['depositStatus']) ?? 'none',
            registeredAt: (user?.registeredAt as string) ?? (user?.createdAt as string) ?? '',
            lastLoginAt: (user?.lastLoginAt as string) ?? '',
            phone: (user?.phone as string) ?? '',
            vatNumber: (user?.vatNumber as string) ?? ((profile?.company as Record<string, unknown>)?.vatNumber as string) ?? '',
            address: (user?.address as UserDetail['address']) ?? { street: '', city: '', postalCode: '', country: '' },
            kycHistory: (user?.kycHistory as UserDetail['kycHistory']) ?? [],
            bidHistory: (user?.bidHistory as UserDetail['bidHistory']) ?? [],
            paymentHistory: (user?.paymentHistory as UserDetail['paymentHistory']) ?? [],
          }
          return
        } catch (fallbackErr: unknown) {
          error.value = extractErrorMessage(fallbackErr, 'Failed to fetch user')
          return
        }
      }
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
