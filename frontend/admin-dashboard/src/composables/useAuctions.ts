import { ref, reactive } from 'vue'
import { useApi } from './useApi'

export interface Auction {
  id: string
  title: string
  brand: string
  description: string
  country: string
  buyerPremiumPercent: number
  startDate: string
  endDate: string
  status: 'draft' | 'scheduled' | 'active' | 'closing' | 'closed' | 'cancelled'
  lotCount: number
  totalBids: number
  createdAt: string
  updatedAt: string
}

export interface AuctionCreatePayload {
  title: string
  brand: string
  description: string
  country: string
  buyerPremiumPercent: number
  startDate: string
  endDate: string
}

export interface AuctionLot {
  id: string
  auctionId: string
  lotNumber: number
  title: string
  currentBid: number
  bidCount: number
  status: 'pending' | 'approved' | 'active' | 'sold' | 'unsold' | 'withdrawn'
  extensionCount: number
  closingTime: string
}

export interface AuctionFilters {
  status: string
  brand: string
  dateFrom: string
  dateTo: string
  page: number
  pageSize: number
}

export interface BidActivity {
  id: string
  lotId: string
  lotTitle: string
  bidderName: string
  amount: number
  timestamp: string
}

export function useAuctions() {
  const { get, post, put, patch } = useApi()

  const auctions = ref<Auction[]>([])
  const currentAuction = ref<Auction | null>(null)
  const auctionLots = ref<AuctionLot[]>([])
  const liveBids = ref<BidActivity[]>([])
  const totalCount = ref(0)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const filters = reactive<AuctionFilters>({
    status: '',
    brand: '',
    dateFrom: '',
    dateTo: '',
    page: 1,
    pageSize: 20,
  })

  async function fetchAuctions(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const params: Record<string, any> = {
        page: filters.page,
        pageSize: filters.pageSize,
      }
      if (filters.status) params.status = filters.status
      if (filters.brand) params.brand = filters.brand
      if (filters.dateFrom) params.dateFrom = filters.dateFrom
      if (filters.dateTo) params.dateTo = filters.dateTo

      const response = await get<{ items: Auction[]; total: number }>('/admin/auctions', { params })
      auctions.value = response.items
      totalCount.value = response.total
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to fetch auctions'
    } finally {
      loading.value = false
    }
  }

  async function fetchAuction(id: string): Promise<void> {
    loading.value = true
    error.value = null
    try {
      currentAuction.value = await get<Auction>(`/admin/auctions/${id}`)
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to fetch auction'
    } finally {
      loading.value = false
    }
  }

  async function createAuction(payload: AuctionCreatePayload): Promise<Auction | null> {
    loading.value = true
    error.value = null
    try {
      const auction = await post<Auction>('/admin/auctions', payload)
      return auction
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to create auction'
      return null
    } finally {
      loading.value = false
    }
  }

  async function cancelAuction(id: string, reason: string): Promise<boolean> {
    loading.value = true
    error.value = null
    try {
      await patch(`/admin/auctions/${id}/cancel`, { reason })
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to cancel auction'
      return false
    } finally {
      loading.value = false
    }
  }

  async function closeAuction(id: string): Promise<boolean> {
    loading.value = true
    error.value = null
    try {
      await patch(`/admin/auctions/${id}/close`)
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to close auction'
      return false
    } finally {
      loading.value = false
    }
  }

  async function fetchAuctionLots(auctionId: string): Promise<void> {
    try {
      const response = await get<{ items: AuctionLot[] }>(`/admin/auctions/${auctionId}/lots`)
      auctionLots.value = response.items
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to fetch lots'
    }
  }

  async function fetchLiveBids(auctionId: string): Promise<void> {
    try {
      const response = await get<{ items: BidActivity[] }>(`/admin/auctions/${auctionId}/bids/live`)
      liveBids.value = response.items
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to fetch live bids'
    }
  }

  return {
    auctions,
    currentAuction,
    auctionLots,
    liveBids,
    totalCount,
    loading,
    error,
    filters,
    fetchAuctions,
    fetchAuction,
    createAuction,
    cancelAuction,
    closeAuction,
    fetchAuctionLots,
    fetchLiveBids,
  }
}
