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
    featured: a.featured ?? false,
    featuredAt: a.featuredAt ?? undefined,
    awardedAt: a.awardedAt ?? undefined,
    autoAwarded: a.autoAwarded ?? false,
    createdAt: a.createdAt ?? '',
    updatedAt: a.updatedAt ?? '',
  }
}

export function useAuctions() {
  const { get, post, patch, del } = useApi()
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

      const raw = await get<ApiResponse<PagedResponse<RawAuctionResponse>>>('/auctions', { params })
      // Unwrap ApiResponse wrapper: backend returns { success, data: { items, total } }
      const response = (raw.data ?? raw) as unknown as PagedResponse<RawAuctionResponse>
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
      const raw = await get<ApiResponse<RawAuctionResponse>>(`/auctions/${id}`)
      // Unwrap ApiResponse wrapper: backend returns { success, data: { ... } }
      const auctionData = (raw.data ?? raw) as unknown as RawAuctionResponse
      currentAuction.value = normalizeAuction(auctionData)
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
        featured: false,
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

  /**
   * Builds the auction lots list from the current auction detail.
   *
   * The auction-engine has a 1:1 auction-to-lot relationship, so there is no
   * separate `/auctions/{id}/lots` endpoint. Instead we derive a single
   * AuctionLot entry from the auction detail response that is already loaded
   * via `fetchAuction()`. Must be called *after* `fetchAuction()` resolves.
   */
  async function fetchAuctionLots(_auctionId: string): Promise<void> {
    try {
      // currentAuction may be the raw ApiResponse wrapper or the normalised object
      const auction = currentAuction.value as Record<string, unknown> | null
      if (!auction) {
        auctionLots.value = []
        return
      }
      // Unwrap ApiResponse if needed (backend wraps in { success, data })
      const data = (auction.data ?? auction) as Record<string, unknown>

      const lotId = (data.lotId ?? data.id ?? '') as string
      const lot: AuctionLot = {
        id: lotId,
        auctionId: (data.auctionId ?? data.id ?? '') as string,
        lotNumber: 1,
        title: (data.title as string) ?? `Lot ${String(lotId).substring(0, 8)}`,
        currentBid: Number(data.currentHighBid ?? data.currentBid ?? 0),
        bidCount: Number(data.totalBids ?? data.bidCount ?? 0),
        status: ((data.status as string) ?? 'active').toLowerCase() as AuctionLot['status'],
        extensionCount: Number(data.extensionCount ?? 0),
        closingTime: (data.endTime ?? data.endDate ?? '') as string,
      }
      auctionLots.value = [lot]
    } catch (err: unknown) {
      error.value = extractErrorMessage(err, 'Failed to fetch lots')
    }
  }

  /**
   * Fetches bid history for an auction from `GET /auctions/{id}/bids`.
   *
   * The backend returns `ApiResponse<List<BidResponse>>` — there is no
   * separate `/bids/live` sub-resource.
   */
  async function fetchLiveBids(auctionId: string): Promise<void> {
    try {
      const raw = await get<Record<string, unknown>>(`/auctions/${auctionId}/bids`)
      // Unwrap ApiResponse: { success, data: [...] }
      const bidsArray = (Array.isArray(raw) ? raw : (raw.data ?? [])) as Record<string, unknown>[]

      liveBids.value = (Array.isArray(bidsArray) ? bidsArray : []).map((b) => ({
        id: (b.bidId ?? b.id ?? '') as string,
        lotId: (b.lotId ?? '') as string,
        lotTitle: (b.lotTitle ?? '') as string,
        bidderName: (b.bidderName ?? maskBidderId((b.bidderId ?? '') as string)) as string,
        amount: Number(b.amount ?? 0),
        timestamp: (b.timestamp ?? '') as string,
      }))
    } catch (err: unknown) {
      if (is404(err)) {
        liveBids.value = []
      } else {
        error.value = extractErrorMessage(err, 'Failed to fetch live bids')
      }
    }
  }

  async function featureAuction(id: string): Promise<boolean> {
    loading.value = true
    error.value = null
    try {
      await post(`/auctions/${id}/feature`)
      if (currentAuction.value?.id === id) {
        currentAuction.value = { ...currentAuction.value, featured: true }
      }
      return true
    } catch (err: unknown) {
      error.value = extractErrorMessage(err, 'Failed to feature auction')
      return false
    } finally {
      loading.value = false
    }
  }

  async function unfeatureAuction(id: string): Promise<boolean> {
    loading.value = true
    error.value = null
    try {
      await del(`/auctions/${id}/feature`)
      if (currentAuction.value?.id === id) {
        currentAuction.value = { ...currentAuction.value, featured: false }
      }
      return true
    } catch (err: unknown) {
      error.value = extractErrorMessage(err, 'Failed to unfeature auction')
      return false
    } finally {
      loading.value = false
    }
  }

  async function revokeAward(id: string, reason: string): Promise<boolean> {
    loading.value = true
    error.value = null
    try {
      await post(`/auctions/${id}/revoke-award`, { reason })
      return true
    } catch (err: unknown) {
      error.value = extractErrorMessage(err, 'Failed to revoke award')
      return false
    } finally {
      loading.value = false
    }
  }

  /** Masks a bidder UUID for display (e.g. "abcd1234-...-5678" → "abcd****5678"). */
  function maskBidderId(bidderId: string): string {
    if (!bidderId || bidderId.length < 8) return bidderId
    const clean = bidderId.replace(/-/g, '')
    return `${clean.substring(0, 4)}****${clean.substring(clean.length - 4)}`
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
    featureAuction,
    unfeatureAuction,
    revokeAward,
  }
}
