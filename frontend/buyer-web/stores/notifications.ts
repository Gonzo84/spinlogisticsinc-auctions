import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { Notification } from '~/types/notification'

export const useNotificationsStore = defineStore('notifications', () => {
  const unreadCount = ref(0)
  const recentNotifications = ref<Notification[]>([])
  const pushRegistered = ref(false)

  const hasUnread = computed((): boolean => {
    return unreadCount.value > 0
  })

  const unreadNotifications = computed((): Notification[] => {
    return recentNotifications.value.filter((n) => !n.read)
  })

  const overbidNotifications = computed((): Notification[] => {
    return recentNotifications.value.filter((n) => n.type === 'overbid' && !n.read)
  })

  function setNotifications(notifications: Notification[]) {
    recentNotifications.value = notifications
    unreadCount.value = notifications.filter((n) => !n.read).length
  }

  function addNotification(notification: Notification) {
    recentNotifications.value = [notification, ...recentNotifications.value].slice(0, 50)
    if (!notification.read) {
      unreadCount.value++
    }
  }

  function markAsRead(notificationId: string) {
    const notification = recentNotifications.value.find((n) => n.id === notificationId)
    if (notification && !notification.read) {
      recentNotifications.value = recentNotifications.value.map((n) =>
        n.id === notificationId ? { ...n, read: true } : n,
      )
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
  }

  function markAllAsRead() {
    recentNotifications.value = recentNotifications.value.map((n) => ({ ...n, read: true }))
    unreadCount.value = 0
  }

  function setUnreadCount(count: number) {
    unreadCount.value = count
  }

  function setPushRegistered(value: boolean) {
    pushRegistered.value = value
  }

  function removeNotification(notificationId: string) {
    const notification = recentNotifications.value.find((n) => n.id === notificationId)
    if (notification) {
      if (!notification.read) {
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
      recentNotifications.value = recentNotifications.value.filter((n) => n.id !== notificationId)
    }
  }

  return {
    unreadCount,
    recentNotifications,
    pushRegistered,
    hasUnread,
    unreadNotifications,
    overbidNotifications,
    setNotifications,
    addNotification,
    markAsRead,
    markAllAsRead,
    setUnreadCount,
    setPushRegistered,
    removeNotification,
  }
})
