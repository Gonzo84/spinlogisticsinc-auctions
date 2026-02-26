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
  lotId: string
  brand: string
  startTime: string
  endTime: string
  startingBid: number
  currency: string
  sellerId: string
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

      const response = await get<any>('/auctions', { params })
      const items = response.items ?? []
      auctions.value = items.map((a: any) => ({
        id: a.id ?? a.auctionId ?? '',
        title: a.title ?? `Auction ${(a.id ?? a.auctionId ?? '').substring(0, 8)}`,
        brand: a.brand ?? '',
        description: a.description ?? '',
        country: a.country ?? '',
        buyerPremiumPercent: a.buyerPremiumPercent ?? 0,
        startDate: a.startDate ?? a.startTime ?? '',
        endDate: a.endDate ?? a.endTime ?? '',
        status: (a.status ?? 'draft').toLowerCase(),
        lotCount: a.lotCount ?? 1,
        totalBids: a.totalBids ?? a.bidCount ?? 0,
        createdAt: a.createdAt ?? '',
        updatedAt: a.updatedAt ?? '',
      }))
      totalCount.value = response.total ?? 0
    } catch {
      auctions.value = []
      totalCount.value = 0
      error.value = null
    } finally {
      loading.value = false
    }
  }

  async function fetchAuction(id: string): Promise<void> {
    loading.value = true
    error.value = null
    try {
      currentAuction.value = await get<Auction>(`/auctions/${id}`)
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
      const raw = await post<any>('/auctions', payload)
      // Extract auction ID from response (may be wrapped in ApiResponse)
      const auctionData = raw?.data ?? raw
      const auctionId = auctionData?.auctionId ?? auctionData?.id ?? ''

      const auction: Auction = {
        id: auctionId,
        title: `Auction ${String(auctionId).substring(0, 8)}`,
        brand: auctionData?.brand ?? payload.brand,
        description: '',
        country: '',
        buyerPremiumPercent: 0,
        startDate: auctionData?.startTime ?? payload.startTime,
        endDate: auctionData?.endTime ?? payload.endTime,
        status: (auctionData?.status ?? 'scheduled').toLowerCase(),
        lotCount: 1,
        totalBids: 0,
        createdAt: auctionData?.createdAt ?? '',
        updatedAt: auctionData?.updatedAt ?? '',
      }

      // Assign the lot to this auction in catalog-service (transitions lot to ACTIVE).
      // This is a separate step — if it fails, the auction was still created successfully.
      if (auctionId && payload.lotId) {
        try {
          await post(`/lots/${payload.lotId}/assign-auction`, { auctionId })
        } catch (assignErr: any) {
          error.value = 'Auction created but lot assignment failed. Assign the lot manually.'
          return auction
        }
      }

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
      await patch(`/auctions/${id}/cancel`, { reason })
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
      await patch(`/auctions/${id}/close`)
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
      const response = await get<{ items: AuctionLot[] }>(`/auctions/${auctionId}/lots`)
      auctionLots.value = response.items
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to fetch lots'
    }
  }

  async function fetchLiveBids(auctionId: string): Promise<void> {
    try {
      const response = await get<{ items: BidActivity[] }>(`/auctions/${auctionId}/bids/live`)
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
