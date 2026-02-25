import { useAuctionStore } from '~/stores/auction'
import type { Auction, Bid } from '~/stores/auction'

export interface AuctionListParams {
  category?: string
  country?: string
  sort?: string
  limit?: number
  page?: number
  featured?: boolean
  status?: string
}

export interface AuctionListResult {
  items: Auction[]
  total: number
  totalPages: number
  page: number
}

// Unwrap ApiResponse<T> wrapper ({data: T, meta}) if present
function unwrapApiResponse(raw: Record<string, unknown>): Record<string, unknown> {
  if (raw && typeof raw === 'object' && 'data' in raw && raw.data && typeof raw.data === 'object' && !Array.isArray(raw.data)) {
    return raw.data as Record<string, unknown>
  }
  return raw
}

// Computes the minimum bid increment based on the current bid amount,
// matching the backend's tiered increment rules in AuctionConstants.
function computeMinIncrement(currentBid: number): number {
  if (currentBid >= 100_000) return 500
  if (currentBid >= 50_000) return 250
  if (currentBid >= 10_000) return 100
  if (currentBid >= 5_000) return 50
  if (currentBid >= 1_000) return 25
  if (currentBid >= 500) return 10
  if (currentBid >= 100) return 5
  return 1
}

// Maps backend auction + optional lot catalog data to frontend Auction type
function mapAuctionResponse(auction: Record<string, unknown>, lot?: Record<string, unknown>): Auction {
  const lotImages = lot?.images as Array<Record<string, unknown>> | undefined
  return {
    id: (auction.auctionId ?? auction.id ?? '') as string,
    title: (lot?.title ?? auction.title ?? `Lot ${((auction.lotId ?? auction.auctionId ?? '') as string).substring(0, 8)}`) as string,
    lotNumber: (auction.lotId ?? lot?.id ?? '') as string,
    description: (lot?.description ?? auction.description ?? '') as string,
    category: (lot?.categoryId ?? auction.category ?? '') as string,
    country: (lot?.locationCountry ?? auction.country ?? '') as string,
    location: lot ? `${lot.locationCity ?? ''}, ${lot.locationCountry ?? ''}`.replace(/^, |, $/, '') : (auction.location ?? '') as string,
    images: lotImages?.map((img: Record<string, unknown>) => ({ url: img.imageUrl as string, thumbnail: img.thumbnailUrl as string })) ?? auction.images as Auction['images'] ?? [],
    currentBid: (auction.currentHighBid ?? auction.currentBid ?? 0) as number,
    startingPrice: (auction.startingBid ?? lot?.startingBid ?? auction.startingPrice ?? 0) as number,
    bidCount: (auction.bidCount ?? 0) as number,
    bidHistory: (auction.bidHistory ?? []) as Bid[],
    endTime: (auction.endTime ?? '') as string,
    startTime: (auction.startTime ?? '') as string,
    status: ((auction.status ?? 'active') as string).toLowerCase() as Auction['status'],
    reserveMet: (auction.reserveMet ?? false) as boolean,
    co2Savings: (lot?.co2AvoidedKg ?? auction.co2Savings) as number | undefined,
    specifications: lot?.specifications
      ? Object.entries(lot.specifications as Record<string, unknown>).map(([key, value]) => ({ key, label: key, value: String(value) }))
      : (auction.specifications ?? []) as Auction['specifications'],
    seller: auction.seller as Auction['seller'],
    depositRequired: (auction.depositRequired ?? false) as boolean,
    depositAmount: auction.depositAmount as number | undefined,
    minIncrement: (auction.minIncrement ?? computeMinIncrement((auction.currentHighBid ?? auction.currentBid ?? auction.startingBid ?? 0) as number)) as number,
    featured: auction.featured as boolean | undefined,
  }
}

export function useAuction() {
  const { $api } = useNuxtApp()
  const auctionStore = useAuctionStore()
  const ws = useWebSocket()

  const loading = ref(false)
  const error = ref<string | null>(null)

  async function getAuction(id: string): Promise<Auction> {
    loading.value = true
    error.value = null
    try {
      const api = $api as typeof $fetch
      const auctionRaw = unwrapApiResponse(await api<Record<string, unknown>>(`/auctions/${id}`))

      // Fetch lot catalog details if lotId is available
      let lotData: Record<string, unknown> | null = null
      if (auctionRaw.lotId) {
        try {
          lotData = unwrapApiResponse(await api<Record<string, unknown>>(`/lots/${auctionRaw.lotId}`))
        } catch {
          // Lot details optional - auction data alone is sufficient
        }
      }

      const auction = mapAuctionResponse(auctionRaw, lotData ?? undefined)
      auctionStore.setAuction(auction)
      return auction
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to load auction'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function listAuctions(params: AuctionListParams = {}): Promise<AuctionListResult> {
    loading.value = true
    error.value = null
    try {
      const api = $api as typeof $fetch
      const result = await api<Record<string, unknown>>('/auctions', {
        params: {
          ...(params.category && { category: params.category }),
          ...(params.country && { country: params.country }),
          ...(params.sort && { sort: params.sort }),
          ...(params.limit && { limit: params.limit }),
          ...(params.page && { page: params.page }),
          ...(params.featured !== undefined && { featured: params.featured }),
          ...(params.status && { status: params.status }),
        },
      })
      const items = (result.items ?? []) as Record<string, unknown>[]
      return {
        items: items.map((item) => mapAuctionResponse(item)),
        total: (result.total ?? 0) as number,
        totalPages: (result.totalPages ?? 0) as number,
        page: (result.page ?? 1) as number,
      }
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to load auctions'
      return { items: [], total: 0, totalPages: 0, page: 1 }
    } finally {
      loading.value = false
    }
  }

  function subscribeToAuction(auctionId: string) {
    if (!ws.isConnected.value) {
      ws.connect()
    }

    ws.subscribe(auctionId)

    ws.onBidPlaced((data: { bid: Bid; auctionId: string }) => {
      if (data.auctionId === auctionId) {
        auctionStore.addBid(data.bid)
      }
    })

    ws.onAuctionExtended((data: { auctionId: string; newEndTime: string }) => {
      if (data.auctionId === auctionId) {
        auctionStore.extendAuction(data.newEndTime)
      }
    })

    ws.onAuctionClosed((data: { auctionId: string }) => {
      if (data.auctionId === auctionId) {
        auctionStore.closeAuction()
      }
    })
  }

  function unsubscribeFromAuction(auctionId: string) {
    ws.unsubscribe(auctionId)
    auctionStore.clearAuction()
  }

  return {
    loading: readonly(loading),
    error: readonly(error),
    currentAuction: computed(() => auctionStore.currentAuction),
    bids: computed(() => auctionStore.bids),
    isActive: computed(() => auctionStore.isActive),
    isClosed: computed(() => auctionStore.isClosed),
    getAuction,
    listAuctions,
    subscribeToAuction,
    unsubscribeFromAuction,
  }
}
