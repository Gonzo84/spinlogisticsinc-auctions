import { ref, computed } from 'vue'
import { useApi } from './useApi'

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

export interface Lot {
  id: string
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
  status: 'draft' | 'pending_review' | 'active' | 'sold' | 'unsold' | 'rejected'
  location: {
    address: string
    city: string
    country: string
    lat: number
    lng: number
  }
  images: LotImage[]
  auctionStart: string | null
  auctionEnd: string | null
  hammerPrice: number | null
  createdAt: string
  updatedAt: string
}

export interface LotFormData {
  title: string
  description: string
  category: string
  specifications: Record<string, string>
  startingBid: number
  reservePrice: number | null
  location: {
    address: string
    city: string
    country: string
    lat: number
    lng: number
  }
  imageIds: string[]
}

export type LotStatus = 'draft' | 'pending_review' | 'active' | 'sold' | 'unsold' | 'rejected'

export interface LotsFilter {
  status?: LotStatus
  page?: number
  pageSize?: number
  search?: string
  sortBy?: string
  sortDir?: 'asc' | 'desc'
}

export interface PaginatedResponse<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

export function useLots() {
  const { get, post, put, del, loading, error } = useApi()

  const lots = ref<Lot[]>([])
  const currentLot = ref<Lot | null>(null)
  const pagination = ref({ total: 0, page: 1, pageSize: 20, totalPages: 0 })
  const lotBids = ref<LotBid[]>([])

  const statusCounts = ref<Record<LotStatus, number>>({
    draft: 0,
    pending_review: 0,
    active: 0,
    sold: 0,
    unsold: 0,
    rejected: 0,
  })

  const activeLots = computed(() => lots.value.filter((l) => l.status === 'active'))
  const draftLots = computed(() => lots.value.filter((l) => l.status === 'draft'))

  async function fetchLots(filters: LotsFilter = {}): Promise<void> {
    const params: Record<string, unknown> = {
      page: filters.page ?? 1,
      pageSize: filters.pageSize ?? 20,
    }
    if (filters.status) params.status = filters.status
    if (filters.search) params.search = filters.search
    if (filters.sortBy) params.sortBy = filters.sortBy
    if (filters.sortDir) params.sortDir = filters.sortDir

    const response = await get<PaginatedResponse<Lot>>('/seller/lots', params)
    lots.value = response.items
    pagination.value = {
      total: response.total,
      page: response.page,
      pageSize: response.pageSize,
      totalPages: response.totalPages,
    }
  }

  async function fetchLot(id: string): Promise<Lot> {
    const lot = await get<Lot>(`/seller/lots/${id}`)
    currentLot.value = lot
    return lot
  }

  async function fetchLotBids(lotId: string): Promise<LotBid[]> {
    const bids = await get<LotBid[]>(`/seller/lots/${lotId}/bids`)
    lotBids.value = bids
    return bids
  }

  async function createLot(data: LotFormData): Promise<Lot> {
    const lot = await post<Lot>('/seller/lots', data)
    return lot
  }

  async function updateLot(id: string, data: Partial<LotFormData>): Promise<Lot> {
    const lot = await put<Lot>(`/seller/lots/${id}`, data)
    currentLot.value = lot
    return lot
  }

  async function deleteLot(id: string): Promise<void> {
    await del(`/seller/lots/${id}`)
    lots.value = lots.value.filter((l) => l.id !== id)
  }

  async function submitForReview(id: string): Promise<Lot> {
    const lot = await post<Lot>(`/seller/lots/${id}/submit`)
    if (currentLot.value?.id === id) currentLot.value = lot
    const idx = lots.value.findIndex((l) => l.id === id)
    if (idx !== -1) lots.value[idx] = lot
    return lot
  }

  async function relistLot(id: string): Promise<Lot> {
    const lot = await post<Lot>(`/seller/lots/${id}/relist`)
    return lot
  }

  async function acceptBelowReserve(lotId: string, bidId: string): Promise<Lot> {
    const lot = await post<Lot>(`/seller/lots/${lotId}/accept-below-reserve`, { bidId })
    currentLot.value = lot
    return lot
  }

  async function fetchStatusCounts(): Promise<Record<LotStatus, number>> {
    const counts = await get<Record<LotStatus, number>>('/seller/lots/status-counts')
    statusCounts.value = counts
    return counts
  }

  async function getPresignedUploadUrl(
    filename: string,
    contentType: string,
  ): Promise<{ uploadUrl: string; imageId: string; publicUrl: string }> {
    return post('/seller/lots/images/presigned-url', { filename, contentType })
  }

  return {
    lots,
    currentLot,
    pagination,
    lotBids,
    statusCounts,
    activeLots,
    draftLots,
    loading,
    error,
    fetchLots,
    fetchLot,
    fetchLotBids,
    createLot,
    updateLot,
    deleteLot,
    submitForReview,
    relistLot,
    acceptBelowReserve,
    fetchStatusCounts,
    getPresignedUploadUrl,
  }
}
