import { ref, computed, readonly } from 'vue'
import { useApi } from './useApi'
import { useAuth } from './useAuth'
import type {
  Lot,
  LotBid,
  LotFormData,
  LotStatus,
  LotsFilter,
  Category,
  LotImage,
  RawLotData,
  ApiResponse,
  PagedResponse,
} from '@/types'

export type { Lot, LotBid, LotFormData, LotStatus, LotsFilter, Category, LotImage }

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
    approved: 0,
    active: 0,
    sold: 0,
    unsold: 0,
    rejected: 0,
    withdrawn: 0,
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
    if (filters.status) params.status = filters.status.toUpperCase()
    if (filters.search) params.search = filters.search
    if (filters.sortBy) params.sortBy = filters.sortBy
    if (filters.sortDir) params.sortDir = filters.sortDir
    // Filter by current seller's ID so we only show their lots
    if (sellerId.value) params.sellerId = sellerId.value

    // Use catalog-service endpoint; seller-service may not have synced yet
    const raw = await get<ApiResponse<PagedResponse<RawLotData>> | PagedResponse<RawLotData>>('/lots', params)
    // Unwrap ApiResponse wrapper ({data: T, meta}) if present
    const response = unwrapApiResponse<PagedResponse<RawLotData>>(raw)
    lots.value = (response.items ?? []).map(normalizeLot)
    pagination.value = {
      total: response.total ?? 0,
      page: response.page ?? 1,
      pageSize: response.pageSize ?? 20,
      totalPages: response.totalPages ?? 0,
    }
  }

  /** Unwrap ApiResponse wrapper if present, returning the inner data. */
  function unwrapApiResponse<T>(raw: unknown): T {
    const obj = raw as Record<string, unknown> | null
    if (obj?.data && typeof obj.data === 'object') {
      return obj.data as T
    }
    return raw as T
  }

  /** Normalize backend flat fields into the nested structure the UI expects */
  function normalizeLot(data: RawLotData): Lot {
    return {
      id: data.id ?? '',
      brand: data.brand ?? '',
      title: data.title ?? '',
      description: data.description ?? '',
      category: data.category ?? data.categoryId ?? '',
      categoryPath: data.categoryPath ?? [],
      specifications: data.specifications ?? {},
      startingBid: data.startingBid ?? 0,
      reservePrice: data.reservePrice ?? null,
      currentBid: data.currentBid ?? null,
      bidCount: data.bidCount ?? 0,
      viewerCount: data.viewerCount ?? 0,
      currency: data.currency ?? 'EUR',
      status: ((data.status ?? 'draft').toLowerCase()) as LotStatus,
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
      createdAt: data.createdAt ?? '',
      updatedAt: data.updatedAt ?? '',
    }
  }

  async function fetchLot(id: string): Promise<Lot> {
    // Use catalog-service endpoint for lot details (seller-service may not have synced yet)
    const raw = await get<ApiResponse<RawLotData> | RawLotData>(`/lots/${id}`)
    const data = unwrapApiResponse<RawLotData>(raw)
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

  /** Flatten a category tree into a flat list including all nested children */
  function flattenCategoryTree(nodes: Record<string, unknown>[]): Category[] {
    const result: Category[] = []
    for (const node of nodes) {
      result.push({
        id: (node.id as string) ?? '',
        parentId: (node.parentId as string) ?? null,
        name: (node.name as string) ?? '',
        slug: (node.slug as string) ?? '',
        icon: (node.icon as string) ?? '',
        level: (node.level as number) ?? 0,
        sortOrder: (node.sortOrder as number) ?? 0,
        active: (node.active as boolean) ?? true,
      })
      const children = node.children as Record<string, unknown>[] | undefined
      if (children && Array.isArray(children) && children.length > 0) {
        result.push(...flattenCategoryTree(children))
      }
    }
    return result
  }

  async function fetchCategories(): Promise<Category[]> {
    try {
      // Use /categories/tree to get the full hierarchy (including subcategories)
      // so we can resolve any categoryId (root or child) to its name
      const raw = await get<ApiResponse<Record<string, unknown>[]> | Record<string, unknown>[]>('/categories/tree')
      const data = unwrapApiResponse<Record<string, unknown>[] | { items?: Record<string, unknown>[] }>(raw)
      const treeNodes = Array.isArray(data) ? data : (data?.items ?? [])
      categories.value = flattenCategoryTree(treeNodes)
      return categories.value
    } catch {
      // Fallback to flat root categories if tree endpoint fails
      try {
        const raw = await get<ApiResponse<Category[]> | Category[]>('/categories')
        const data = unwrapApiResponse<Category[] | { items?: Category[] }>(raw)
        categories.value = Array.isArray(data) ? data : (data?.items ?? [])
        return categories.value
      } catch {
        error.value = null
        return []
      }
    }
  }

  /** Transform frontend LotFormData into the flat structure the backend expects */
  function toBackendPayload(data: LotFormData | Partial<LotFormData>): Record<string, unknown> {
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
    const raw = await post<ApiResponse<RawLotData> | RawLotData>('/lots', toBackendPayload(data))
    const lot = normalizeLot(unwrapApiResponse<RawLotData>(raw))
    return lot
  }

  async function updateLot(id: string, data: Partial<LotFormData>): Promise<Lot> {
    const raw = await put<ApiResponse<RawLotData> | RawLotData>(`/lots/${id}`, toBackendPayload(data))
    const lot = normalizeLot(unwrapApiResponse<RawLotData>(raw))
    currentLot.value = lot
    return lot
  }

  async function deleteLot(id: string): Promise<void> {
    await del(`/lots/${id}`)
    lots.value = lots.value.filter((l) => l.id !== id)
  }

  async function submitForReview(id: string): Promise<Lot> {
    const raw = await post<ApiResponse<RawLotData> | RawLotData>(`/lots/${id}/submit`)
    const lot = normalizeLot(unwrapApiResponse<RawLotData>(raw))
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
    // Always use catalog-service as the authoritative source for lot status
    // counts. The seller-service's seller_lots projection may have stale data
    // if NATS event sync is incomplete.
    try {
      return await fetchStatusCountsFromCatalog()
    } catch {
      // Fallback to seller-service if catalog-based counting fails
      try {
        const raw = await get<
          ApiResponse<Record<string, number>> | Record<string, number>
        >('/sellers/me/lots/status-counts')
        const data = unwrapApiResponse<Record<string, number>>(raw)
        const counts: Record<LotStatus, number> = {
          draft: 0,
          pending_review: 0,
          approved: 0,
          active: 0,
          sold: 0,
          unsold: 0,
          rejected: 0,
          withdrawn: 0,
        }
        for (const [key, value] of Object.entries(data)) {
          const normalizedKey = key.toLowerCase() as LotStatus
          if (normalizedKey in counts) {
            counts[normalizedKey] = value
          }
        }
        statusCounts.value = counts
        return counts
      } catch {
        error.value = null
        return statusCounts.value
      }
    }
  }

  /** Fallback: fetch all lots from catalog and compute counts client-side */
  async function fetchStatusCountsFromCatalog(): Promise<Record<LotStatus, number>> {
    const counts: Record<LotStatus, number> = {
      draft: 0,
      pending_review: 0,
      approved: 0,
      active: 0,
      sold: 0,
      unsold: 0,
      rejected: 0,
      withdrawn: 0,
    }
    const params: Record<string, unknown> = { page: 0, pageSize: 1000 }
    if (sellerId.value) params.sellerId = sellerId.value
    try {
      const raw = await get<ApiResponse<PagedResponse<RawLotData>> | PagedResponse<RawLotData>>('/lots', params)
      const response = unwrapApiResponse<PagedResponse<RawLotData>>(raw)
      const items = response.items ?? []
      for (const item of items) {
        const status = ((item.status ?? '').toLowerCase()) as LotStatus
        if (status in counts) {
          counts[status]++
        }
      }
    } catch {
      // ignore - return zero counts
    }
    statusCounts.value = counts
    return counts
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
    pagination: readonly(pagination),
    lotBids,
    statusCounts: readonly(statusCounts),
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
