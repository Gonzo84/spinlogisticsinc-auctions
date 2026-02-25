import { defineStore } from 'pinia'

export interface Bid {
  id: string
  auctionId: string
  bidderId: string
  bidderLabel: string
  amount: number
  isAutoBid: boolean
  timestamp: string
}

export interface AuctionImage {
  url: string
  alt?: string
}

export interface Specification {
  key: string
  label: string
  value: string
}

export interface Seller {
  id: string
  name: string
  totalAuctions: number
  rating: number
}

export interface Auction {
  id: string
  title: string
  lotNumber: string
  description: string
  category: string
  country: string
  location: string
  address?: string
  images: AuctionImage[]
  currentBid: number
  startingPrice: number
  bidCount: number
  bidHistory: Bid[]
  endTime: string
  startTime: string
  status: 'upcoming' | 'active' | 'extended' | 'closed'
  reservePrice?: number
  reserveMet: boolean
  co2Savings?: number
  specifications: Specification[]
  seller?: Seller
  depositRequired: boolean
  depositAmount?: number
  minIncrement: number
  featured?: boolean
}

export interface AutoBidConfig {
  auctionId: string
  maxAmount: number
  isActive: boolean
}

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
      this.bids.unshift(bid)
      if (this.currentAuction) {
        this.currentAuction.currentBid = bid.amount
        this.currentAuction.bidCount = this.bids.length
      }
    },

    extendAuction(newEndTime: string) {
      this.timerEndTime = newEndTime
      this.isExtended = true
      if (this.currentAuction) {
        this.currentAuction.endTime = newEndTime
        this.currentAuction.status = 'extended'
      }
    },

    closeAuction() {
      if (this.currentAuction) {
        this.currentAuction.status = 'closed'
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
