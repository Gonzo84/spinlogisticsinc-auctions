/** Wrapper returned by all backend REST endpoints. */
export interface ApiResponse<T> {
  data: T
  message?: string
  status?: string
}

/** Paginated list returned inside ApiResponse.data. */
export interface PagedResponse<T> {
  items: T[]
  total: number
  page?: number
  pageSize?: number
}

/** Query-string params shared by every list endpoint. */
export interface PaginationParams {
  page: number
  pageSize: number
  [key: string]: string | number | boolean
}
