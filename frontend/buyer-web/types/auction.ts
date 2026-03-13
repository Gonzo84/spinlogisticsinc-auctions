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
  status: 'upcoming' | 'active' | 'extended' | 'closed' | 'awarded'
  reservePrice?: number
  reserveMet: boolean
  co2Savings?: number
  specifications: Specification[]
  seller?: Seller
  depositRequired: boolean
  depositAmount?: number
  minIncrement: number
  featured?: boolean
  awardedAt?: string
  autoAwarded?: boolean
}

export interface AutoBidConfig {
  auctionId: string
  maxAmount: number
  isActive: boolean
}

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

export interface PlaceBidPayload {
  auctionId: string
  amount: number
}

export interface AutoBidPayload {
  auctionId: string
  maxAmount: number
}

export interface BidEntry {
  id: string
  lotId: string
  title: string
  imageUrl?: string
  amount: number
  currentBid: number
  status: string
  createdAt: string
}

export interface WatchlistLot {
  id: string
  title: string
  imageUrl?: string
  startingBid: number
  location?: string
}
