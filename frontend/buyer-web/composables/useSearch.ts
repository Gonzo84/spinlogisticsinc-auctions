import type { SearchFilters, SearchResult, SearchAggregations, SearchSuggestion } from '~/types/search'
import { unwrapApiResponse } from '~/utils/api-response'
import { mapAuctionResponse } from '~/utils/auction-mapper'

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
      if (filters.featured !== undefined) params.featured = filters.featured
      if (filters.page) params.page = filters.page - 1
      if (filters.limit) params.limit = filters.limit

      let result: SearchResult

      // When there's a keyword query, use catalog-service directly for reliable
      // SQL-based text search. Search-service (Elasticsearch) may not have indexed
      // lots properly, so catalog is the authoritative source for keyword filtering.
      if (filters.q) {
        try {
          const catalogParams: Record<string, string | number> = {
            page: filters.page ? filters.page - 1 : 0,
            pageSize: filters.limit || 20,
          }
          catalogParams.search = filters.q
          if (filters.category) catalogParams.categorySlug = filters.category

          const raw = await api<Record<string, unknown>>('/lots', { params: catalogParams })
          const data = unwrapApiResponse(raw)

          result = {
            items: (Array.isArray(data.items) ? data.items : []) as Record<string, unknown>[],
            total: (typeof data.totalItems === 'number' ? data.totalItems : typeof data.total === 'number' ? data.total : 0),
            totalPages: (typeof data.totalPages === 'number' ? data.totalPages : 0),
            page: (typeof data.page === 'number' ? data.page : 1),
          }
        } catch {
          // Catalog search failed — fall back to search-service
          result = { items: [], total: 0, totalPages: 0, page: 1 }
        }

        // If catalog returned results, use them; otherwise try search-service
        if (result.items.length === 0) {
          try {
            const raw = await api<Record<string, unknown>>('/search/lots', { params })
            const data = unwrapApiResponse(raw)

            result = {
              items: (Array.isArray(data.items) ? data.items : []) as Record<string, unknown>[],
              total: (typeof data.total === 'number' ? data.total : typeof data.totalCount === 'number' ? data.totalCount : 0),
              totalPages: (typeof data.totalPages === 'number' ? data.totalPages : 0),
              page: (typeof data.page === 'number' ? data.page : 1),
              aggregations: data.aggregations as SearchAggregations | undefined,
            }
          } catch {
            // Search-service also failed — keep empty result
          }
        }
      } else {
        // No keyword query — use search-service for browse/filter
        try {
          const raw = await api<Record<string, unknown>>('/search/lots', { params })
          const data = unwrapApiResponse(raw)

          result = {
            items: (Array.isArray(data.items) ? data.items : []) as Record<string, unknown>[],
            total: (typeof data.total === 'number' ? data.total : typeof data.totalCount === 'number' ? data.totalCount : 0),
            totalPages: (typeof data.totalPages === 'number' ? data.totalPages : 0),
            page: (typeof data.page === 'number' ? data.page : 1),
            aggregations: data.aggregations as SearchAggregations | undefined,
          }
        } catch {
          // Search-service unavailable — fall back to catalog browse
          result = { items: [], total: 0, totalPages: 0, page: 1 }
        }

        // Fallback for browse: if search-service returned nothing, try catalog
        if (result.items.length === 0) {
          try {
            const catalogParams: Record<string, string | number> = {
              page: filters.page ? filters.page - 1 : 0,
              pageSize: filters.limit || 20,
            }
            if (filters.category) catalogParams.categorySlug = filters.category

            const raw = await api<Record<string, unknown>>('/lots', { params: catalogParams })
            const data = unwrapApiResponse(raw)

            result = {
              items: (Array.isArray(data.items) ? data.items : []) as Record<string, unknown>[],
              total: (typeof data.totalItems === 'number' ? data.totalItems : typeof data.total === 'number' ? data.total : 0),
              totalPages: (typeof data.totalPages === 'number' ? data.totalPages : 0),
              page: (typeof data.page === 'number' ? data.page : 1),
            }
          } catch {
            // Both services failed — return empty
          }
        }
      }

      // Normalize items through mapAuctionResponse so that field names
      // (e.g. currentHighBid → currentBid, startingBid → startingPrice)
      // match what LotCard.vue expects.
      result.items = result.items.map((item) =>
        mapAuctionResponse(item) as unknown as Record<string, unknown>
      )

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
    if (suggestAbortController) {
      suggestAbortController.abort()
    }

    if (suggestDebounceTimer) {
      clearTimeout(suggestDebounceTimer)
    }

    if (!prefix || prefix.length < 2) {
      suggestions.value = []
      return
    }

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
