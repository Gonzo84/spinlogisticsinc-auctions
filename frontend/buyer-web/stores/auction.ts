import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { Auction, Bid, AutoBidConfig } from '~/types/auction'

export const useAuctionStore = defineStore('auction', () => {
  const currentAuction = ref<Auction | null>(null)
  const bids = ref<Bid[]>([])
  const autoBidConfig = ref<AutoBidConfig | null>(null)
  const timerEndTime = ref<string | null>(null)
  const isExtended = ref(false)

  const currentBid = computed((): number => {
    return currentAuction.value?.currentBid ?? 0
  })

  const bidCount = computed((): number => {
    return bids.value.length || currentAuction.value?.bidCount || 0
  })

  const isActive = computed((): boolean => {
    return currentAuction.value?.status === 'active' || currentAuction.value?.status === 'extended'
  })

  const isClosed = computed((): boolean => {
    return currentAuction.value?.status === 'closed'
  })

  const reserveMet = computed((): boolean => {
    return currentAuction.value?.reserveMet ?? false
  })

  const hasAutoBid = computed((): boolean => {
    return autoBidConfig.value?.isActive ?? false
  })

  const minBidAmount = computed((): number => {
    if (!currentAuction.value) return 0
    if (currentAuction.value.currentBid > 0) {
      return currentAuction.value.currentBid + currentAuction.value.minIncrement
    }
    return currentAuction.value.startingPrice || currentAuction.value.minIncrement
  })

  function setAuction(auction: Auction) {
    currentAuction.value = auction
    bids.value = auction.bidHistory || []
    timerEndTime.value = auction.endTime
    isExtended.value = auction.status === 'extended'
  }

  function addBid(bid: Bid) {
    bids.value = [bid, ...bids.value]
    if (currentAuction.value) {
      currentAuction.value = {
        ...currentAuction.value,
        currentBid: bid.amount,
        bidCount: bids.value.length,
      }
    }
  }

  function extendAuction(newEndTime: string) {
    timerEndTime.value = newEndTime
    isExtended.value = true
    if (currentAuction.value) {
      currentAuction.value = {
        ...currentAuction.value,
        endTime: newEndTime,
        status: 'extended',
      }
    }
  }

  function closeAuction() {
    if (currentAuction.value) {
      currentAuction.value = {
        ...currentAuction.value,
        status: 'closed',
      }
    }
  }

  function setAutoBid(config: AutoBidConfig | null) {
    autoBidConfig.value = config
  }

  function clearAuction() {
    currentAuction.value = null
    bids.value = []
    autoBidConfig.value = null
    timerEndTime.value = null
    isExtended.value = false
  }

  return {
    currentAuction,
    bids,
    autoBidConfig,
    timerEndTime,
    isExtended,
    currentBid,
    bidCount,
    isActive,
    isClosed,
    reserveMet,
    hasAutoBid,
    minBidAmount,
    setAuction,
    addBid,
    extendAuction,
    closeAuction,
    setAutoBid,
    clearAuction,
  }
})
