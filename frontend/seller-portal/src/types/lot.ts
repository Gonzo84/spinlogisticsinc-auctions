/**
 * Lot-related type definitions.
 */

export interface LotImage {
  id: string
  url: string
  thumbnailUrl: string
  isPrimary: boolean
  sortOrder: number
}

export interface LotBid {
  id: string
  bidderId: string
  bidderAlias: string
  amount: number
  currency: string
  timestamp: string
}

export interface LotLocation {
  address: string
  city: string
  country: string
  lat: number
  lng: number
}

export type LotStatus = 'draft' | 'pending_review' | 'approved' | 'active' | 'sold' | 'unsold' | 'rejected' | 'withdrawn'

export interface Lot {
  id: string
  auctionId: string | null
  brand: string
  title: string
  description: string
  category: string
  categoryPath: string[]
  specifications: Record<string, string>
  startingBid: number
  reservePrice: number | null
  currentBid: number | null
  bidCount: number
  viewerCount: number
  currency: string
  status: LotStatus
  location: LotLocation
  images: LotImage[]
  auctionStart: string | null
  auctionEnd: string | null
  hammerPrice: number | null
  createdAt: string
  updatedAt: string
}

export interface Category {
  id: string
  parentId: string | null
  name: string
  slug: string
  icon: string
  level: number
  sortOrder: number
  active: boolean
}

export interface LotFormImage {
  id: string
  url: string
}

export interface LotFormData {
  brand: string
  title: string
  description: string
  categoryId: string
  specifications: Record<string, string>
  startingBid: number
  reservePrice: number | null
  location: LotLocation
  imageIds: string[]
  images: LotFormImage[]
}

export interface LotsFilter {
  status?: LotStatus
  page?: number
  pageSize?: number
  search?: string
  sortBy?: string
  sortDir?: 'asc' | 'desc'
}

/**
 * Shape of the raw lot data from the backend before normalization.
 * Contains both the nested `location` object (if present) and the flat
 * `locationCity`, `locationCountry`, etc. fields the backend may return.
 */
export interface RawLotData {
  id?: string
  auctionId?: string | null
  title?: string
  description?: string
  category?: string
  categoryId?: string
  categoryPath?: string[]
  specifications?: Record<string, string>
  startingBid?: number
  reservePrice?: number | null
  currentBid?: number | null
  bidCount?: number
  viewerCount?: number
  currency?: string
  status?: string
  brand?: string
  location?: LotLocation
  locationAddress?: string
  locationCity?: string
  locationCountry?: string
  locationLat?: number
  locationLng?: number
  images?: LotImage[]
  auctionStart?: string | null
  auctionEnd?: string | null
  hammerPrice?: number | null
  createdAt?: string
  updatedAt?: string
}
