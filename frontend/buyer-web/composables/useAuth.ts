import { useAuthStore } from '~/stores/auth'
import type { User } from '~/types/user'
import { unwrapApiResponse } from '~/utils/api-response'

interface KeycloakInstance {
  login(options?: { redirectUri?: string }): Promise<void>
  logout(options?: { redirectUri?: string }): Promise<void>
  register(options?: Record<string, unknown>): Promise<void>
  updateToken(minValidity: number): Promise<boolean>
  token?: string
  refreshToken?: string
}

export function useAuth() {
  const authStore = useAuthStore()
  const { $keycloak, $api } = useNuxtApp()

  const isAuthenticated = computed(() => authStore.isAuthenticated)
  const user = computed(() => authStore.user)
  const token = computed(() => authStore.token)
  const roles = computed(() => authStore.roles)
  const fullName = computed(() => authStore.fullName)
  const initials = computed(() => authStore.initials)
  const isBusiness = computed(() => authStore.isBusiness)

  async function login(redirectUri?: string) {
    const keycloak = $keycloak as KeycloakInstance | undefined
    if (keycloak) {
      await keycloak.login({
        redirectUri: redirectUri || window.location.href,
      })
    }
  }

  async function logout() {
    const keycloak = $keycloak as KeycloakInstance | undefined
    authStore.clearSession()
    if (keycloak) {
      await keycloak.logout({
        redirectUri: window.location.origin,
      })
    }
  }

  async function register(accountType?: 'business' | 'private') {
    const keycloak = $keycloak as KeycloakInstance | undefined
    if (keycloak) {
      await keycloak.register({
        redirectUri: `${window.location.origin}/auth/callback`,
        ...(accountType && { 'kc_account_type': accountType }),
      })
    }
  }

  function refreshToken(): Promise<boolean> {
    const keycloak = $keycloak as KeycloakInstance | undefined
    if (!keycloak) return Promise.resolve(false)

    return keycloak.updateToken(30).then((refreshed: boolean) => {
      if (refreshed && keycloak.token) {
        authStore.updateToken(keycloak.token, keycloak.refreshToken || undefined)
      }
      return refreshed
    }).catch(() => {
      authStore.clearSession()
      return false
    })
  }

  function hasRole(role: string): boolean {
    return authStore.hasRole(role)
  }

  async function updateProfile(data: Partial<User>): Promise<void> {
    const api = $api as typeof $fetch
    const raw = await api<Record<string, unknown>>('/users/me', {
      method: 'PUT',
      body: data,
    })
    const unwrapped = unwrapApiResponse(raw)
    authStore.updateUser(unwrapped as unknown as User)
  }

  function requireAuth(redirectTo?: string): boolean {
    if (!isAuthenticated.value) {
      login(redirectTo || window.location.href)
      return false
    }
    return true
  }

  return {
    isAuthenticated,
    user,
    token,
    roles,
    fullName,
    initials,
    isBusiness,
    login,
    logout,
    register,
    refreshToken,
    hasRole,
    updateProfile,
    requireAuth,
  }
}
