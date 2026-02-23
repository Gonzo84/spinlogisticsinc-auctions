import { useNotificationsStore } from '~/stores/notifications'
import type { Notification } from '~/stores/notifications'

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
      const result = await api<{ items: Notification[]; unreadCount: number }>('/notifications', {
        params: { page, limit: 20 },
      })

      notificationsStore.setNotifications(result.items)
      notificationsStore.setUnreadCount(result.unreadCount)
      return result.items
    } catch (e: any) {
      error.value = e?.message || 'Failed to load notifications'
      return []
    } finally {
      loading.value = false
    }
  }

  async function markAsRead(notificationId: string): Promise<void> {
    try {
      const api = $api as typeof $fetch
      await api(`/notifications/${notificationId}/read`, {
        method: 'POST',
      })
      notificationsStore.markAsRead(notificationId)
    } catch (e: any) {
      console.error('Failed to mark notification as read:', e)
    }
  }

  async function markAllAsRead(): Promise<void> {
    try {
      const api = $api as typeof $fetch
      await api('/notifications/read-all', {
        method: 'POST',
      })
      notificationsStore.markAllAsRead()
    } catch (e: any) {
      console.error('Failed to mark all notifications as read:', e)
    }
  }

  async function registerPushToken(token: string): Promise<void> {
    try {
      const api = $api as typeof $fetch
      await api('/notifications/push-token', {
        method: 'POST',
        body: { token, platform: 'web' },
      })
      notificationsStore.setPushRegistered(true)
    } catch (e: any) {
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

    ws.onOverbid((data: any) => {
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
