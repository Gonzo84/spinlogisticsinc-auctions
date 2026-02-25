import { ref, computed } from 'vue'
import { useApi } from './useApi'
import { useAuth } from './useAuth'

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

export interface LotFormData {
  brand: string
  title: string
  description: string
  categoryId: string
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
  const { sellerId } = useAuth()

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
    // Catalog-service uses 0-based pagination; frontend uses 1-based
    const frontendPage = filters.page ?? 1
    const params: Record<string, unknown> = {
      page: frontendPage - 1,
      pageSize: filters.pageSize ?? 20,
    }
    if (filters.status) params.status = filters.status
    if (filters.search) params.search = filters.search
    if (filters.sortBy) params.sortBy = filters.sortBy
    if (filters.sortDir) params.sortDir = filters.sortDir
    // Filter by current seller's ID so we only show their lots
    if (sellerId.value) params.sellerId = sellerId.value

    // Use catalog-service endpoint; seller-service may not have synced yet
    const raw = await get<any>('/lots', params)
    // Unwrap ApiResponse wrapper ({data: T, meta}) if present
    const response = raw?.data && typeof raw.data === 'object' && !Array.isArray(raw.data) ? raw.data : raw
    lots.value = (response.items ?? []).map(normalizeLot)
    pagination.value = {
      total: response.total ?? 0,
      page: response.page ?? 1,
      pageSize: response.pageSize ?? 20,
      totalPages: response.totalPages ?? 0,
    }
  }

  /** Normalize backend flat fields into the nested structure the UI expects */
  function normalizeLot(data: any): Lot {
    return {
      ...data,
      category: data.category ?? data.categoryId ?? '',
      specifications: data.specifications ?? {},
      currentBid: data.currentBid ?? null,
      bidCount: data.bidCount ?? 0,
      viewerCount: data.viewerCount ?? 0,
      currency: data.currency ?? 'EUR',
      status: (data.status ?? 'draft').toLowerCase(),
      location: data.location ?? {
        address: data.locationAddress ?? '',
        city: data.locationCity ?? '',
        country: data.locationCountry ?? '',
        lat: data.locationLat ?? 0,
        lng: data.locationLng ?? 0,
      },
      images: data.images ?? [],
      auctionStart: data.auctionStart ?? null,
      auctionEnd: data.auctionEnd ?? null,
      hammerPrice: data.hammerPrice ?? null,
    }
  }

  async function fetchLot(id: string): Promise<Lot> {
    // Use catalog-service endpoint for lot details (seller-service may not have synced yet)
    const raw = await get<any>(`/lots/${id}`)
    const data = raw?.data && typeof raw.data === 'object' && !Array.isArray(raw.data) ? raw.data : raw
    const lot = normalizeLot(data)
    currentLot.value = lot
    return lot
  }

  async function fetchLotBids(lotId: string): Promise<LotBid[]> {
    const bids = await get<LotBid[]>(`/auctions/${lotId}/bids`)
    lotBids.value = bids
    return bids
  }

  const categories = ref<Category[]>([])

  async function fetchCategories(): Promise<Category[]> {
    try {
      const raw = await get<any>('/categories')
      const data = raw?.data ?? raw
      categories.value = Array.isArray(data) ? data : (data?.items ?? [])
      return categories.value
    } catch {
      error.value = null
      return []
    }
  }

  /** Transform frontend LotFormData into the flat structure the backend expects */
  function toBackendPayload(data: LotFormData | Partial<LotFormData>) {
    return {
      brand: data.brand ?? '',
      title: data.title,
      description: data.description,
      categoryId: data.categoryId,
      specifications: data.specifications ?? {},
      startingBid: data.startingBid,
      reservePrice: data.reservePrice ?? null,
      locationAddress: data.location?.address ?? null,
      locationCity: data.location?.city ?? '',
      locationCountry: data.location?.country ?? '',
      locationLat: data.location?.lat ?? null,
      locationLng: data.location?.lng ?? null,
    }
  }

  async function createLot(data: LotFormData): Promise<Lot> {
    const raw = await post<any>('/lots', toBackendPayload(data))
    const lot = raw?.data && typeof raw.data === 'object' && !Array.isArray(raw.data) ? raw.data : raw
    return lot
  }

  async function updateLot(id: string, data: Partial<LotFormData>): Promise<Lot> {
    const raw = await put<any>(`/lots/${id}`, toBackendPayload(data))
    const lot = raw?.data && typeof raw.data === 'object' && !Array.isArray(raw.data) ? raw.data : raw
    currentLot.value = lot
    return lot
  }

  async function deleteLot(id: string): Promise<void> {
    await del(`/lots/${id}`)
    lots.value = lots.value.filter((l) => l.id !== id)
  }

  async function submitForReview(id: string): Promise<Lot> {
    const lot = await post<Lot>(`/lots/${id}/submit`)
    if (currentLot.value?.id === id) currentLot.value = lot
    const idx = lots.value.findIndex((l) => l.id === id)
    if (idx !== -1) lots.value[idx] = lot
    return lot
  }

  async function relistLot(id: string): Promise<Lot> {
    const lot = await post<Lot>(`/sellers/me/lots/${id}/relist`)
    return lot
  }

  async function acceptBelowReserve(lotId: string, bidId: string): Promise<Lot> {
    const lot = await post<Lot>(`/sellers/me/lots/${lotId}/accept-below-reserve`, { bidId })
    currentLot.value = lot
    return lot
  }

  async function fetchStatusCounts(): Promise<Record<LotStatus, number>> {
    // The /sellers/me/lots/status-counts endpoint doesn't exist.
    // Instead, fetch all seller lots and compute counts locally.
    try {
      const params: Record<string, unknown> = { page: 0, pageSize: 1000 }
      if (sellerId.value) params.sellerId = sellerId.value
      const raw = await get<any>('/lots', params)
      const response = raw?.data && typeof raw.data === 'object' && !Array.isArray(raw.data) ? raw.data : raw
      const allLots = (response.items ?? []) as any[]
      const counts: Record<LotStatus, number> = {
        draft: 0,
        pending_review: 0,
        active: 0,
        sold: 0,
        unsold: 0,
        rejected: 0,
      }
      for (const lot of allLots) {
        const s = (lot.status ?? 'draft').toLowerCase() as LotStatus
        if (s in counts) counts[s]++
      }
      statusCounts.value = counts
      return counts
    } catch {
      error.value = null
      return statusCounts.value
    }
  }

  async function getPresignedUploadUrl(
    filename: string,
    contentType: string,
  ): Promise<{ uploadUrl: string; imageId: string; publicUrl: string }> {
    return post('/media/upload/presigned', { filename, contentType })
  }

  return {
    lots,
    currentLot,
    pagination,
    lotBids,
    statusCounts,
    categories,
    activeLots,
    draftLots,
    loading,
    error,
    fetchLots,
    fetchLot,
    fetchLotBids,
    fetchCategories,
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
