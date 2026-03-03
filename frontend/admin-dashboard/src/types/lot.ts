export type LotStatus =
  | 'DRAFT'
  | 'PENDING_REVIEW'
  | 'APPROVED'
  | 'ACTIVE'
  | 'SOLD'
  | 'UNSOLD'
  | 'WITHDRAWN'

export interface Lot {
  id: string
  title: string
  description: string
  category: string
  categoryId?: string
  brand: string
  sellerId: string
  sellerName?: string
  startingBid: number
  reservePrice: number | null
  locationCity: string
  locationCountry: string
  status: LotStatus
  imageCount: number
  primaryImageUrl: string | null
  specifications: Record<string, string>
  createdAt: string
  updatedAt: string
}

export interface PendingLot {
  id: string
  title: string
  description: string
  brand: string
  category: string
  categoryId: string
  sellerName: string
  sellerId: string
  startingBid: number
  reservePrice: number | null
  location: { city: string; country: string; address?: string }
  imageCount: number
  primaryImage: string | null
  specifications: Record<string, string>
  submittedAt: string
  detailLoaded: boolean
}

export interface ApprovedLot {
  id: string
  title: string
  brand: string
  sellerId: string
  startingBid: number
  locationCountry: string
  locationCity: string
}
