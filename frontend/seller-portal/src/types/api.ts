/**
 * API-level types for request/response handling.
 */

/** Standard API response wrapper returned by all backend endpoints. */
export interface ApiResponse<T> {
  data: T
  meta?: Record<string, unknown>
}

/** Paginated response from the catalog-service and similar endpoints. */
export interface PagedResponse<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

/** Options for composable API requests. */
export interface RequestOptions {
  params?: Record<string, unknown>
  headers?: Record<string, string>
}
