import { ref, computed, readonly } from 'vue'
import { useApi } from './useApi'
import { useErrorHandler } from './useErrorHandler'
import type { Notification, ApiResponse, PagedResponse } from '@/types'

export function useNotifications() {
  const { get, put } = useApi()
  const { handleApiError, handleGracefulDegradation, is404 } = useErrorHandler()

  const notifications = ref<Notification[]>([])
  const unreadCount = ref(0)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const hasUnread = computed(() => unreadCount.value > 0)

  /** Unwrap ApiResponse wrapper if present, returning the inner data. */
  function unwrapApiResponse<T>(raw: unknown): T {
    const obj = raw as Record<string, unknown> | null
    if (obj?.data && typeof obj.data === 'object') {
      return obj.data as T
    }
    return raw as T
  }

  async function fetchNotifications(page: number = 1): Promise<Notification[]> {
    loading.value = true
    error.value = null
    try {
      const raw = await get<ApiResponse<PagedResponse<Notification>> | PagedResponse<Notification>>(
        '/notifications',
        { params: { page, size: 20 } },
      )
      const response = unwrapApiResponse<PagedResponse<Notification>>(raw)
      const items = Array.isArray(response.items) ? response.items : []
      notifications.value = items

      // Compute unread count from items
      unreadCount.value = items.filter((n) => !n.read).length
      return items
    } catch (err: unknown) {
      if (is404(err)) {
        handleGracefulDegradation('fetchNotifications')
      } else {
        error.value = handleApiError(err, 'Failed to load notifications')
      }
      return []
    } finally {
      loading.value = false
    }
  }

  async function markAsRead(notificationId: string): Promise<void> {
    try {
      await put(`/notifications/${notificationId}/read`)
      const idx = notifications.value.findIndex((n) => n.id === notificationId)
      if (idx !== -1) {
        notifications.value[idx] = { ...notifications.value[idx], read: true }
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
    } catch {
      // Silently handle error
    }
  }

  async function markAllAsRead(): Promise<void> {
    try {
      await put('/notifications/read-all')
      notifications.value = notifications.value.map((n) => ({ ...n, read: true }))
      unreadCount.value = 0
    } catch {
      // Silently handle error
    }
  }

  return {
    notifications: readonly(notifications),
    unreadCount: readonly(unreadCount),
    hasUnread,
    loading: readonly(loading),
    error: readonly(error),
    fetchNotifications,
    markAsRead,
    markAllAsRead,
  }
}
