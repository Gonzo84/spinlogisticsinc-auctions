import { useAuctionStore } from '~/stores/auction'
import type { Auction, Bid, AuctionListParams, AuctionListResult } from '~/types/auction'
import { unwrapApiResponse } from '~/utils/api-response'
import { mapAuctionResponse } from '~/utils/auction-mapper'

export function useAuction() {
  const { $api } = useNuxtApp()
  const auctionStore = useAuctionStore()
  const ws = useWebSocket()

  const loading = ref(false)
  const error = ref<string | null>(null)

  // Store handler references for cleanup
  let bidHandler: ((data: { bid: Bid; auctionId: string }) => void) | null = null
  let extendedHandler: ((data: { auctionId: string; newEndTime: string }) => void) | null = null
  let closedHandler: ((data: { auctionId: string }) => void) | null = null

  async function getAuction(id: string): Promise<Auction> {
    loading.value = true
    error.value = null
    try {
      const api = $api as typeof $fetch
      const lotRaw = unwrapApiResponse(await api<Record<string, unknown>>(`/lots/${id}`))

      let auctionData: Record<string, unknown> | null = null
      if (lotRaw.auctionId) {
        try {
          auctionData = unwrapApiResponse(await api<Record<string, unknown>>(`/auctions/${lotRaw.auctionId}`))
        } catch {
          // Auction data optional - lot data alone is sufficient
        }
      }

      // If lot has no auctionId, try to find an auction by lotId
      if (!auctionData) {
        try {
          const lotUuid = lotRaw.id ?? id
          const auctionList = await api<Record<string, unknown>>(`/auctions`, {
            params: { lotId: lotUuid, size: 1 },
          })
          const items = (auctionList.items ?? []) as Record<string, unknown>[]
          if (items.length > 0) {
            // Fetch the full auction detail using the found auctionId
            const foundAuctionId = items[0].auctionId ?? items[0].id
            if (foundAuctionId) {
              try {
                auctionData = unwrapApiResponse(await api<Record<string, unknown>>(`/auctions/${foundAuctionId}`))
              } catch {
                // Use summary data as fallback
                auctionData = items[0]
              }
            }
          }
        } catch {
          // No auction found for this lot - that's fine
        }
      }

      const auction = mapAuctionResponse(auctionData ?? lotRaw, lotRaw)
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

    bidHandler = (data: { bid: Bid; auctionId: string }) => {
      if (data.auctionId === auctionId) {
        auctionStore.addBid(data.bid)
      }
    }

    extendedHandler = (data: { auctionId: string; newEndTime: string }) => {
      if (data.auctionId === auctionId) {
        auctionStore.extendAuction(data.newEndTime)
      }
    }

    closedHandler = (data: { auctionId: string }) => {
      if (data.auctionId === auctionId) {
        auctionStore.closeAuction()
      }
    }

    ws.onBidPlaced(bidHandler)
    ws.onAuctionExtended(extendedHandler)
    ws.onAuctionClosed(closedHandler)
  }

  function unsubscribeFromAuction(auctionId: string) {
    ws.unsubscribe(auctionId)

    // Remove specific handlers to prevent memory leaks
    if (bidHandler) {
      ws.off('bid_placed', bidHandler)
      bidHandler = null
    }
    if (extendedHandler) {
      ws.off('auction_extended', extendedHandler)
      extendedHandler = null
    }
    if (closedHandler) {
      ws.off('auction_closed', closedHandler)
      closedHandler = null
    }

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
