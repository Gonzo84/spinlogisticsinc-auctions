import { useNotificationsStore } from '~/stores/notifications'
import type { Notification } from '~/types/notification'
import { unwrapApiResponse } from '~/utils/api-response'

export function useNotifications() {
  const { $api } = useNuxtApp()
  const notificationsStore = useNotificationsStore()
  const ws = useWebSocket()

  const loading = ref(false)
  const error = ref<string | null>(null)

  const unreadCount = computed(() => notificationsStore.unreadCount)
  const hasUnread = computed(() => notificationsStore.hasUnread)
  const recentNotifications = computed(() => notificationsStore.recentNotifications)
  const overbidNotifications = computed(() => notificationsStore.overbidNotifications)

  async function getNotifications(page: number = 1): Promise<Notification[]> {
    loading.value = true
    error.value = null

    try {
      const api = $api as typeof $fetch
      const raw = await api<Record<string, unknown>>('/notifications', {
        params: { page, size: 20 },
      })

      const data = unwrapApiResponse(raw)
      const items = (Array.isArray(data.items) ? data.items : []) as Notification[]
      const unreadCount = typeof data.unreadCount === 'number' ? data.unreadCount : 0

      notificationsStore.setNotifications(items)
      notificationsStore.setUnreadCount(unreadCount)
      return items
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to load notifications'
      return []
    } finally {
      loading.value = false
    }
  }

  async function markAsRead(notificationId: string): Promise<void> {
    try {
      const api = $api as typeof $fetch
      await api(`/notifications/${notificationId}/read`, {
        method: 'PUT',
      })
      notificationsStore.markAsRead(notificationId)
    } catch (e: unknown) {
      console.error('Failed to mark notification as read:', e)
    }
  }

  async function markAllAsRead(): Promise<void> {
    try {
      const api = $api as typeof $fetch
      await api('/notifications/read-all', {
        method: 'PUT',
      })
      notificationsStore.markAllAsRead()
    } catch (e: unknown) {
      console.error('Failed to mark all notifications as read:', e)
    }
  }

  async function registerPushToken(token: string): Promise<void> {
    try {
      const api = $api as typeof $fetch
      await api('/notifications/device-token', {
        method: 'POST',
        body: { token, platform: 'web' },
      })
      notificationsStore.setPushRegistered(true)
    } catch (e: unknown) {
      console.error('Failed to register push token:', e)
    }
  }

  function subscribeToRealtimeNotifications() {
    if (!ws.isConnected.value) {
      ws.connect()
    }

    ws.onNotification((data: Notification) => {
      notificationsStore.addNotification(data)
    })

    ws.onOverbid((data: { lotTitle?: string; auctionId?: string; newBidAmount?: number }) => {
      const notification: Notification = {
        id: `overbid-${Date.now()}`,
        type: 'overbid',
        title: 'You\'ve been outbid!',
        message: `Someone placed a higher bid on "${data.lotTitle}"`,
        auctionId: data.auctionId,
        lotTitle: data.lotTitle,
        amount: data.newBidAmount,
        read: false,
        createdAt: new Date().toISOString(),
        actionUrl: `/lots/${data.auctionId}`,
      }
      notificationsStore.addNotification(notification)
    })
  }

  function dismissNotification(notificationId: string) {
    notificationsStore.removeNotification(notificationId)
  }

  return {
    loading: readonly(loading),
    error: readonly(error),
    unreadCount,
    hasUnread,
    recentNotifications,
    overbidNotifications,
    getNotifications,
    markAsRead,
    markAllAsRead,
    registerPushToken,
    subscribeToRealtimeNotifications,
    dismissNotification,
  }
}
