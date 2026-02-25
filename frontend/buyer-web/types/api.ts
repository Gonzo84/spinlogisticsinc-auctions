export interface ApiResponse<T> {
  data: T
  meta?: Record<string, unknown>
}

export interface PagedResponse<T> {
  items: T[]
  total: number
  totalPages: number
  page: number
}

export interface ApiError {
  status: number
  title: string
  detail?: string
  type?: string
}
