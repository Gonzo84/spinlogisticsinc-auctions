import { defineStore } from 'pinia'

export interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  company?: string
  vatNumber?: string
  country?: string
  phone?: string
  accountType: 'business' | 'private'
  roles: string[]
  avatarUrl?: string
}

export interface AuthState {
  user: User | null
  token: string | null
  refreshToken: string | null
  isAuthenticated: boolean
  roles: string[]
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    user: null,
    token: null,
    refreshToken: null,
    isAuthenticated: false,
    roles: [],
  }),

  getters: {
    fullName: (state): string => {
      if (!state.user) return ''
      return `${state.user.firstName} ${state.user.lastName}`.trim()
    },

    initials: (state): string => {
      if (!state.user) return ''
      const first = state.user.firstName?.charAt(0) || ''
      const last = state.user.lastName?.charAt(0) || ''
      return `${first}${last}`.toUpperCase()
    },

    isBusiness: (state): boolean => {
      return state.user?.accountType === 'business'
    },

    hasRole: (state) => (role: string): boolean => {
      return state.roles.includes(role)
    },
  },

  actions: {
    setSession(payload: { user: User; token: string; refreshToken: string }) {
      this.user = payload.user
      this.token = payload.token
      this.refreshToken = payload.refreshToken
      this.isAuthenticated = true
      this.roles = payload.user.roles || []
    },

    updateToken(token: string, refreshToken?: string) {
      this.token = token
      if (refreshToken) {
        this.refreshToken = refreshToken
      }
    },

    updateUser(userData: Partial<User>) {
      if (this.user) {
        this.user = { ...this.user, ...userData }
      }
    },

    clearSession() {
      this.user = null
      this.token = null
      this.refreshToken = null
      this.isAuthenticated = false
      this.roles = []
    },
  },
})
