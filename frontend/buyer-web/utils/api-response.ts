/**
 * Unwraps an ApiResponse<T> wrapper ({ data: T, meta? }) if present.
 * Returns the inner data object, or the original value if not wrapped.
 */
export function unwrapApiResponse(raw: Record<string, unknown>): Record<string, unknown> {
  if (raw && typeof raw === 'object' && 'data' in raw && raw.data && typeof raw.data === 'object' && !Array.isArray(raw.data)) {
    return raw.data as Record<string, unknown>
  }
  return raw
}
