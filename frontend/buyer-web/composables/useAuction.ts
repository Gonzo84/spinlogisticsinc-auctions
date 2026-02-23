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
      const auction = await api<Auction>(`/auctions/${id}`)
      auctionStore.setAuction(auction)
      return auction
    } catch (e: any) {
      error.value = e?.message || 'Failed to load auction'
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
      const result = await api<AuctionListResult>('/auctions', {
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
      return result
    } catch (e: any) {
      error.value = e?.message || 'Failed to load auctions'
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
