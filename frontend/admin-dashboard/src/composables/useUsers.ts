import { ref, reactive } from 'vue'
import { useApi } from './useApi'

export interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  companyName: string
  accountType: 'buyer' | 'seller' | 'both'
  status: 'active' | 'blocked' | 'pending' | 'suspended'
  kycStatus: 'not_started' | 'pending' | 'approved' | 'rejected'
  depositStatus: 'none' | 'pending' | 'held' | 'released' | 'forfeited'
  registeredAt: string
  lastLoginAt: string
}

export interface UserDetail extends User {
  phone: string
  vatNumber: string
  address: {
    street: string
    city: string
    postalCode: string
    country: string
  }
  kycHistory: KycEvent[]
  bidHistory: BidRecord[]
  paymentHistory: PaymentRecord[]
}

export interface KycEvent {
  id: string
  status: string
  note: string
  performedBy: string
  timestamp: string
}

export interface BidRecord {
  id: string
  auctionTitle: string
  lotTitle: string
  amount: number
  status: 'active' | 'outbid' | 'won' | 'lost'
  timestamp: string
}

export interface PaymentRecord {
  id: string
  lotTitle: string
  amount: number
  status: 'pending' | 'paid' | 'overdue' | 'refunded'
  dueDate: string
  paidDate: string | null
}

export interface UserFilters {
  search: string
  accountType: string
  status: string
  kycStatus: string
  page: number
  pageSize: number
}

export function useUsers() {
  const { get, patch, post } = useApi()

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
      const params: Record<string, any> = {
        page: filters.page,
        pageSize: filters.pageSize,
      }
      if (filters.search) params.search = filters.search
      if (filters.accountType) params.accountType = filters.accountType
      if (filters.status) params.status = filters.status
      if (filters.kycStatus) params.kycStatus = filters.kycStatus

      const response = await get<{ items: User[]; total: number }>('/admin/users', { params })
      users.value = response.items
      totalCount.value = response.total
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to fetch users'
    } finally {
      loading.value = false
    }
  }

  async function fetchUser(id: string): Promise<void> {
    loading.value = true
    error.value = null
    try {
      currentUser.value = await get<UserDetail>(`/admin/users/${id}`)
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to fetch user'
    } finally {
      loading.value = false
    }
  }

  async function blockUser(id: string, reason: string): Promise<boolean> {
    loading.value = true
    error.value = null
    try {
      await patch(`/admin/users/${id}/block`, { reason })
      if (currentUser.value?.id === id) {
        currentUser.value.status = 'blocked'
      }
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to block user'
      return false
    } finally {
      loading.value = false
    }
  }

  async function unblockUser(id: string): Promise<boolean> {
    loading.value = true
    error.value = null
    try {
      await patch(`/admin/users/${id}/unblock`)
      if (currentUser.value?.id === id) {
        currentUser.value.status = 'active'
      }
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to unblock user'
      return false
    } finally {
      loading.value = false
    }
  }

  async function triggerGdprExport(userId: string): Promise<boolean> {
    try {
      await post(`/admin/users/${userId}/gdpr/export`)
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to trigger GDPR export'
      return false
    }
  }

  async function triggerGdprErasure(userId: string, reason: string): Promise<boolean> {
    try {
      await post(`/admin/users/${userId}/gdpr/erasure`, { reason })
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to trigger GDPR erasure'
      return false
    }
  }

  return {
    users,
    currentUser,
    totalCount,
    loading,
    error,
    filters,
    fetchUsers,
    fetchUser,
    blockUser,
    unblockUser,
    triggerGdprExport,
    triggerGdprErasure,
  }
}
