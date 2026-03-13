export interface Auction {
  id: string
  title: string
  brand: string
  description: string
  country: string
  buyerPremiumPercent: number
  startDate: string
  endDate: string
  status: 'draft' | 'scheduled' | 'active' | 'closing' | 'closed' | 'cancelled' | 'awarded'
  lotCount: number
  awardedAt?: string
  autoAwarded?: boolean
  totalBids: number
  featured: boolean
  featuredAt?: string
  createdAt: string
  updatedAt: string
}

export interface AuctionCreatePayload {
  lotId: string
  brand: string
  startTime: string
  endTime: string
  startingBid: number
  currency: string
  sellerId: string
}

export interface AuctionLot {
  id: string
  auctionId: string
  lotNumber: number
  title: string
  currentBid: number
  bidCount: number
  status: 'pending' | 'approved' | 'active' | 'sold' | 'unsold' | 'withdrawn'
  extensionCount: number
  closingTime: string
}

export interface AuctionFilters {
  status: string
  brand: string
  dateFrom: string
  dateTo: string
  page: number
  pageSize: number
  sortBy?: string
  sortDir?: 'asc' | 'desc'
}

export interface BidActivity {
  id: string
  lotId: string
  lotTitle: string
  bidderName: string
  amount: number
  timestamp: string
}

/**
 * Raw auction data shape returned by the backend API.
 * Field names differ from the normalised `Auction` interface
 * (e.g. `auctionId` vs `id`, `startTime` vs `startDate`).
 */
export interface RawAuctionResponse {
  id?: string
  auctionId?: string
  title?: string
  brand?: string
  description?: string
  country?: string
  buyerPremiumPercent?: number
  startDate?: string
  startTime?: string
  endDate?: string
  endTime?: string
  status?: string
  lotCount?: number
  totalBids?: number
  bidCount?: number
  featured?: boolean
  featuredAt?: string
  awardedAt?: string
  autoAwarded?: boolean
  createdAt?: string
  updatedAt?: string
}
