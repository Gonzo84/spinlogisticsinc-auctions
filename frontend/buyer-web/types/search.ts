export interface SearchFilters {
  q?: string
  category?: string
  country?: string[]
  priceMin?: number
  priceMax?: number
  distance?: number
  reserveStatus?: string
  featured?: boolean
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
