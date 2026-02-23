import { useAuctionStore } from '~/stores/auction'
import type { Bid, AutoBidConfig } from '~/stores/auction'

export interface PlaceBidPayload {
  auctionId: string
  amount: number
}

export interface AutoBidPayload {
  auctionId: string
  maxAmount: number
}

export function useBid() {
  const { $api } = useNuxtApp()
  const auctionStore = useAuctionStore()
  const { isAuthenticated } = useAuth()

  const loading = ref(false)
  const error = ref<string | null>(null)
  const lastBid = ref<Bid | null>(null)

  const currentBid = computed(() => auctionStore.currentBid)
  const minBidAmount = computed(() => auctionStore.minBidAmount)
  const hasAutoBid = computed(() => auctionStore.hasAutoBid)
  const autoBidConfig = computed(() => auctionStore.autoBidConfig)

  async function placeBid(payload: PlaceBidPayload): Promise<Bid> {
    if (!isAuthenticated.value) {
      throw new Error('Authentication required to place a bid')
    }

    loading.value = true
    error.value = null

    try {
      const api = $api as typeof $fetch
      const bid = await api<Bid>(`/auctions/${payload.auctionId}/bids`, {
        method: 'POST',
        body: {
          amount: payload.amount,
        },
      })

      lastBid.value = bid
      auctionStore.addBid(bid)
      return bid
    } catch (e: any) {
      const message = e?.data?.message || e?.message || 'Failed to place bid'
      error.value = message
      throw new Error(message)
    } finally {
      loading.value = false
    }
  }

  async function setAutoBid(payload: AutoBidPayload): Promise<AutoBidConfig> {
    if (!isAuthenticated.value) {
      throw new Error('Authentication required to set auto-bid')
    }

    loading.value = true
    error.value = null

    try {
      const api = $api as typeof $fetch
      const config = await api<AutoBidConfig>(`/auctions/${payload.auctionId}/auto-bid`, {
        method: 'POST',
        body: {
          maxAmount: payload.maxAmount,
        },
      })

      auctionStore.setAutoBid(config)
      return config
    } catch (e: any) {
      const message = e?.data?.message || e?.message || 'Failed to set auto-bid'
      error.value = message
      throw new Error(message)
    } finally {
      loading.value = false
    }
  }

  async function cancelAutoBid(auctionId: string): Promise<void> {
    if (!isAuthenticated.value) {
      throw new Error('Authentication required')
    }

    loading.value = true
    error.value = null

    try {
      const api = $api as typeof $fetch
      await api(`/auctions/${auctionId}/auto-bid`, {
        method: 'DELETE',
      })

      auctionStore.setAutoBid(null)
    } catch (e: any) {
      const message = e?.data?.message || e?.message || 'Failed to cancel auto-bid'
      error.value = message
      throw new Error(message)
    } finally {
      loading.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  return {
    loading: readonly(loading),
    error: readonly(error),
    lastBid: readonly(lastBid),
    currentBid,
    minBidAmount,
    hasAutoBid,
    autoBidConfig,
    placeBid,
    setAutoBid,
    cancelAutoBid,
    clearError,
  }
}
