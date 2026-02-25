import { defineStore } from 'pinia'
import type { Auction, Bid, AutoBidConfig } from '~/types/auction'

export interface AuctionState {
  currentAuction: Auction | null
  bids: Bid[]
  autoBidConfig: AutoBidConfig | null
  timerEndTime: string | null
  isExtended: boolean
}

export const useAuctionStore = defineStore('auction', {
  state: (): AuctionState => ({
    currentAuction: null,
    bids: [],
    autoBidConfig: null,
    timerEndTime: null,
    isExtended: false,
  }),

  getters: {
    currentBid: (state): number => {
      return state.currentAuction?.currentBid ?? 0
    },

    bidCount: (state): number => {
      return state.bids.length || state.currentAuction?.bidCount || 0
    },

    isActive: (state): boolean => {
      return state.currentAuction?.status === 'active' || state.currentAuction?.status === 'extended'
    },

    isClosed: (state): boolean => {
      return state.currentAuction?.status === 'closed'
    },

    reserveMet: (state): boolean => {
      return state.currentAuction?.reserveMet ?? false
    },

    hasAutoBid: (state): boolean => {
      return state.autoBidConfig?.isActive ?? false
    },

    minBidAmount: (state): number => {
      if (!state.currentAuction) return 0
      if (state.currentAuction.currentBid > 0) {
        return state.currentAuction.currentBid + state.currentAuction.minIncrement
      }
      return state.currentAuction.startingPrice || state.currentAuction.minIncrement
    },
  },

  actions: {
    setAuction(auction: Auction) {
      this.currentAuction = auction
      this.bids = auction.bidHistory || []
      this.timerEndTime = auction.endTime
      this.isExtended = auction.status === 'extended'
    },

    addBid(bid: Bid) {
      this.bids = [bid, ...this.bids]
      if (this.currentAuction) {
        this.currentAuction = {
          ...this.currentAuction,
          currentBid: bid.amount,
          bidCount: this.bids.length,
        }
      }
    },

    extendAuction(newEndTime: string) {
      this.timerEndTime = newEndTime
      this.isExtended = true
      if (this.currentAuction) {
        this.currentAuction = {
          ...this.currentAuction,
          endTime: newEndTime,
          status: 'extended',
        }
      }
    },

    closeAuction() {
      if (this.currentAuction) {
        this.currentAuction = {
          ...this.currentAuction,
          status: 'closed',
        }
      }
    },

    setAutoBid(config: AutoBidConfig | null) {
      this.autoBidConfig = config
    },

    clearAuction() {
      this.currentAuction = null
      this.bids = []
      this.autoBidConfig = null
      this.timerEndTime = null
      this.isExtended = false
    },
  },
})
