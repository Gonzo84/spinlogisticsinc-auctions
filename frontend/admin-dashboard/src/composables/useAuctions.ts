import { ref, reactive, readonly } from 'vue'
import axios from 'axios'
import { useApi } from './useApi'
import { useErrorHandler } from './useErrorHandler'
import type {
  Auction,
  AuctionCreatePayload,
  AuctionLot,
  AuctionFilters,
  BidActivity,
  RawAuctionResponse,
} from '@/types/auction'
import type { ApiResponse, PagedResponse, PaginationParams } from '@/types/api'

export type { Auction, AuctionCreatePayload, AuctionLot, AuctionFilters, BidActivity } from '@/types/auction'

function extractErrorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    const msg = (err.response?.data as Record<string, unknown> | undefined)?.message
    return typeof msg === 'string' ? msg : fallback
  }
  return err instanceof Error ? err.message : fallback
}

function normalizeAuction(a: RawAuctionResponse): Auction {
  return {
    id: a.id ?? a.auctionId ?? '',
    title: a.title ?? `Auction ${(a.id ?? a.auctionId ?? '').substring(0, 8)}`,
    brand: a.brand ?? '',
    description: a.description ?? '',
    country: a.country ?? '',
    buyerPremiumPercent: a.buyerPremiumPercent ?? 0,
    startDate: a.startDate ?? a.startTime ?? '',
    endDate: a.endDate ?? a.endTime ?? '',
    status: ((a.status ?? 'draft').toLowerCase()) as Auction['status'],
    lotCount: a.lotCount ?? 1,
    totalBids: a.totalBids ?? a.bidCount ?? 0,
    createdAt: a.createdAt ?? '',
    updatedAt: a.updatedAt ?? '',
  }
}

export function useAuctions() {
  const { get, post, patch } = useApi()
  const { handleGracefulDegradation, is404 } = useErrorHandler()

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
      const params: PaginationParams = {
        page: filters.page,
        pageSize: filters.pageSize,
        size: filters.pageSize,
      }
      if (filters.status) params.status = filters.status.toUpperCase()
      if (filters.brand) params.brand = filters.brand
      if (filters.dateFrom) params.dateFrom = filters.dateFrom
      if (filters.dateTo) params.dateTo = filters.dateTo
      if (filters.sortBy) params.sortBy = filters.sortBy
      if (filters.sortDir) params.sortDir = filters.sortDir

      const response = await get<PagedResponse<RawAuctionResponse>>('/auctions', { params })
      const items = response.items ?? []
      auctions.value = items.map(normalizeAuction)
      totalCount.value = response.total ?? 0
    } catch (err: unknown) {
      auctions.value = []
      totalCount.value = 0
      if (is404(err)) {
        handleGracefulDegradation('fetchAuctions')
      } else {
        error.value = extractErrorMessage(err, 'Failed to load auctions')
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchAuction(id: string): Promise<void> {
    loading.value = true
    error.value = null
    try {
      currentAuction.value = await get<Auction>(`/auctions/${id}`)
    } catch (err: unknown) {
      error.value = extractErrorMessage(err, 'Failed to fetch auction')
    } finally {
      loading.value = false
    }
  }

  async function createAuction(payload: AuctionCreatePayload): Promise<Auction | null> {
    loading.value = true
    error.value = null
    try {
      const raw = await post<ApiResponse<RawAuctionResponse>>('/auctions', payload)
      // Extract auction ID from response (may be wrapped in ApiResponse)
      const auctionData: RawAuctionResponse = (raw?.data ?? raw) as RawAuctionResponse
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
        status: ((auctionData?.status ?? 'scheduled').toLowerCase()) as Auction['status'],
        lotCount: 1,
        totalBids: 0,
        createdAt: auctionData?.createdAt ?? '',
        updatedAt: auctionData?.updatedAt ?? '',
      }

      // Assign the lot to this auction in catalog-service (transitions lot to ACTIVE).
      // This is a separate step -- if it fails, the auction was still created successfully.
      if (auctionId && payload.lotId) {
        try {
          await post(`/lots/${payload.lotId}/assign-auction`, { auctionId })
        } catch {
          error.value = 'Auction created but lot assignment failed. Assign the lot manually.'
          return auction
        }
      }

      return auction
    } catch (err: unknown) {
      error.value = extractErrorMessage(err, 'Failed to create auction')
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
    } catch (err: unknown) {
      error.value = extractErrorMessage(err, 'Failed to cancel auction')
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
    } catch (err: unknown) {
      error.value = extractErrorMessage(err, 'Failed to close auction')
      return false
    } finally {
      loading.value = false
    }
  }

  async function fetchAuctionLots(auctionId: string): Promise<void> {
    try {
      const response = await get<{ items: AuctionLot[] }>(`/auctions/${auctionId}/lots`)
      auctionLots.value = response.items
    } catch (err: unknown) {
      error.value = extractErrorMessage(err, 'Failed to fetch lots')
    }
  }

  async function fetchLiveBids(auctionId: string): Promise<void> {
    try {
      const response = await get<{ items: BidActivity[] }>(`/auctions/${auctionId}/bids/live`)
      liveBids.value = response.items
    } catch (err: unknown) {
      error.value = extractErrorMessage(err, 'Failed to fetch live bids')
    }
  }

  return {
    auctions,
    currentAuction,
    auctionLots,
    liveBids,
    totalCount,
    loading: readonly(loading),
    error: readonly(error),
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
