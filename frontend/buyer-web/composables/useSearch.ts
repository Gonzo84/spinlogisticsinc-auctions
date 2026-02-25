export interface SearchFilters {
  q?: string
  category?: string
  country?: string[]
  priceMin?: number
  priceMax?: number
  distance?: number
  reserveStatus?: string
  sort?: string
  page?: number
  limit?: number
}

export interface SearchResult {
  items: Record<string, unknown>[]
  total: number
  totalPages: number
  page: number
  aggregations?: SearchAggregations
}

export interface SearchAggregations {
  categories: AggregationBucket[]
  countries: AggregationBucket[]
  priceRanges: AggregationBucket[]
}

export interface AggregationBucket {
  key: string
  label: string
  count: number
}

export interface SearchSuggestion {
  text: string
  type: 'lot' | 'category' | 'brand'
  id?: string
  imageUrl?: string
}

export function useSearch() {
  const { $api } = useNuxtApp()

  const loading = ref(false)
  const error = ref<string | null>(null)
  const suggestions = ref<SearchSuggestion[]>([])
  const aggregations = ref<SearchAggregations | null>(null)

  let suggestAbortController: AbortController | null = null
  let suggestDebounceTimer: ReturnType<typeof setTimeout> | null = null

  async function search(filters: SearchFilters = {}): Promise<SearchResult> {
    loading.value = true
    error.value = null

    try {
      const api = $api as typeof $fetch
      const params: Record<string, string | number | boolean> = {}

      if (filters.q) params.q = filters.q
      if (filters.category) params.category = filters.category
      if (filters.country?.length) params.country = filters.country.join(',')
      if (filters.priceMin !== undefined) params.priceMin = filters.priceMin
      if (filters.priceMax !== undefined) params.priceMax = filters.priceMax
      if (filters.distance) params.distance = filters.distance
      if (filters.reserveStatus) params.reserveStatus = filters.reserveStatus
      if (filters.sort) params.sort = filters.sort
      if (filters.page) params.page = filters.page - 1
      if (filters.limit) params.limit = filters.limit

      const raw = await api<Record<string, unknown>>('/search/lots', { params })

      // Unwrap ApiResponse wrapper if present
      const data = (raw && typeof raw === 'object' && 'data' in raw && raw.data && typeof raw.data === 'object')
        ? raw.data as Record<string, unknown>
        : raw as Record<string, unknown>

      const result: SearchResult = {
        items: (Array.isArray(data.items) ? data.items : []) as Record<string, unknown>[],
        total: (typeof data.total === 'number' ? data.total : typeof data.totalCount === 'number' ? data.totalCount : 0),
        totalPages: (typeof data.totalPages === 'number' ? data.totalPages : 0),
        page: (typeof data.page === 'number' ? data.page : 1),
        aggregations: data.aggregations as SearchAggregations | undefined,
      }

      if (result.aggregations) {
        aggregations.value = result.aggregations
      }

      return result
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Search failed'
      return { items: [], total: 0, totalPages: 0, page: 1 }
    } finally {
      loading.value = false
    }
  }

  function suggest(prefix: string) {
    // Cancel previous request
    if (suggestAbortController) {
      suggestAbortController.abort()
    }

    // Clear previous debounce
    if (suggestDebounceTimer) {
      clearTimeout(suggestDebounceTimer)
    }

    if (!prefix || prefix.length < 2) {
      suggestions.value = []
      return
    }

    // Debounce 300ms
    suggestDebounceTimer = setTimeout(async () => {
      suggestAbortController = new AbortController()

      try {
        const api = $api as typeof $fetch
        const result = await api<SearchSuggestion[]>('/search/lots/suggest', {
          params: { q: prefix },
          signal: suggestAbortController.signal,
        })

        suggestions.value = result
      } catch (e: unknown) {
        if (!(e instanceof Error) || e.name !== 'AbortError') {
          suggestions.value = []
        }
      }
    }, 300)
  }

  async function nearby(lat: number, lng: number, radiusKm: number = 100): Promise<SearchResult> {
    loading.value = true
    error.value = null

    try {
      const api = $api as typeof $fetch
      const result = await api<SearchResult>('/search/lots/nearby', {
        params: { lat, lng, radius: radiusKm },
      })
      return result
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Nearby search failed'
      return { items: [], total: 0, totalPages: 0, page: 1 }
    } finally {
      loading.value = false
    }
  }

  function clearSuggestions() {
    suggestions.value = []
    if (suggestDebounceTimer) {
      clearTimeout(suggestDebounceTimer)
    }
    if (suggestAbortController) {
      suggestAbortController.abort()
    }
  }

  return {
    loading: readonly(loading),
    error: readonly(error),
    suggestions: readonly(suggestions),
    aggregations: readonly(aggregations),
    search,
    suggest,
    nearby,
    clearSuggestions,
  }
}
