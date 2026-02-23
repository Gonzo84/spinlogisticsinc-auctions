import { defineStore } from 'pinia'

export interface Notification {
  id: string
  type: 'overbid' | 'auction_won' | 'auction_closing' | 'payment_reminder' | 'system'
  title: string
  message: string
  auctionId?: string
  lotTitle?: string
  amount?: number
  read: boolean
  createdAt: string
  actionUrl?: string
}

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
      this.recentNotifications.unshift(notification)
      if (!notification.read) {
        this.unreadCount++
      }
      if (this.recentNotifications.length > 50) {
        this.recentNotifications = this.recentNotifications.slice(0, 50)
      }
    },

    markAsRead(notificationId: string) {
      const notification = this.recentNotifications.find((n) => n.id === notificationId)
      if (notification && !notification.read) {
        notification.read = true
        this.unreadCount = Math.max(0, this.unreadCount - 1)
      }
    },

    markAllAsRead() {
      this.recentNotifications.forEach((n) => (n.read = true))
      this.unreadCount = 0
    },

    setUnreadCount(count: number) {
      this.unreadCount = count
    },

    setPushRegistered(value: boolean) {
      this.pushRegistered = value
    },

    removeNotification(notificationId: string) {
      const index = this.recentNotifications.findIndex((n) => n.id === notificationId)
      if (index !== -1) {
        if (!this.recentNotifications[index].read) {
          this.unreadCount = Math.max(0, this.unreadCount - 1)
        }
        this.recentNotifications.splice(index, 1)
      }
    },
  },
})
