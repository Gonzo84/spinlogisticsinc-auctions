import { defineStore } from 'pinia'
import type { Notification } from '~/types/notification'

export interface NotificationsState {
  unreadCount: number
  recentNotifications: Notification[]
  pushRegistered: boolean
}

export const useNotificationsStore = defineStore('notifications', {
  state: (): NotificationsState => ({
    unreadCount: 0,
    recentNotifications: [],
    pushRegistered: false,
  }),

  getters: {
    hasUnread: (state): boolean => {
      return state.unreadCount > 0
    },

    unreadNotifications: (state): Notification[] => {
      return state.recentNotifications.filter((n) => !n.read)
    },

    overbidNotifications: (state): Notification[] => {
      return state.recentNotifications.filter((n) => n.type === 'overbid' && !n.read)
    },
  },

  actions: {
    setNotifications(notifications: Notification[]) {
      this.recentNotifications = notifications
      this.unreadCount = notifications.filter((n) => !n.read).length
    },

    addNotification(notification: Notification) {
      this.recentNotifications = [notification, ...this.recentNotifications].slice(0, 50)
      if (!notification.read) {
        this.unreadCount++
      }
    },

    markAsRead(notificationId: string) {
      const notification = this.recentNotifications.find((n) => n.id === notificationId)
      if (notification && !notification.read) {
        this.recentNotifications = this.recentNotifications.map((n) =>
          n.id === notificationId ? { ...n, read: true } : n,
        )
        this.unreadCount = Math.max(0, this.unreadCount - 1)
      }
    },

    markAllAsRead() {
      this.recentNotifications = this.recentNotifications.map((n) => ({ ...n, read: true }))
      this.unreadCount = 0
    },

    setUnreadCount(count: number) {
      this.unreadCount = count
    },

    setPushRegistered(value: boolean) {
      this.pushRegistered = value
    },

    removeNotification(notificationId: string) {
      const notification = this.recentNotifications.find((n) => n.id === notificationId)
      if (notification) {
        if (!notification.read) {
          this.unreadCount = Math.max(0, this.unreadCount - 1)
        }
        this.recentNotifications = this.recentNotifications.filter((n) => n.id !== notificationId)
      }
    },
  },
})
