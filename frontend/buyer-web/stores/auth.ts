import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { User } from '~/types/user'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(null)
  const refreshToken = ref<string | null>(null)
  const isAuthenticated = ref(false)
  const roles = ref<string[]>([])

  const fullName = computed((): string => {
    if (!user.value) return ''
    return `${user.value.firstName} ${user.value.lastName}`.trim()
  })

  const initials = computed((): string => {
    if (!user.value) return ''
    const first = user.value.firstName?.charAt(0) || ''
    const last = user.value.lastName?.charAt(0) || ''
    return `${first}${last}`.toUpperCase()
  })

  const isBusiness = computed((): boolean => {
    return user.value?.accountType === 'business'
  })

  function hasRole(role: string): boolean {
    return roles.value.includes(role)
  }

  function setSession(payload: { user: User; token: string; refreshToken: string }) {
    user.value = payload.user
    token.value = payload.token
    refreshToken.value = payload.refreshToken
    isAuthenticated.value = true
    roles.value = payload.user.roles || []
  }

  function updateToken(newToken: string, newRefreshToken?: string) {
    token.value = newToken
    if (newRefreshToken) {
      refreshToken.value = newRefreshToken
    }
  }

  function updateUser(userData: Partial<User>) {
    if (user.value) {
      user.value = { ...user.value, ...userData }
    }
  }

  function clearSession() {
    user.value = null
    token.value = null
    refreshToken.value = null
    isAuthenticated.value = false
    roles.value = []
  }

  return {
    user,
    token,
    refreshToken,
    isAuthenticated,
    roles,
    fullName,
    initials,
    isBusiness,
    hasRole,
    setSession,
    updateToken,
    updateUser,
    clearSession,
  }
})
