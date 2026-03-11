import { inject, computed, readonly } from 'vue'
import type Keycloak from 'keycloak-js'

export function useAuth() {
  const _keycloak = inject<Keycloak>('keycloak')

  if (!_keycloak) {
    throw new Error('Keycloak instance not provided. Ensure keycloak is injected at app level.')
  }

  const keycloak: Keycloak = _keycloak

  const isAuthenticated = computed(() => keycloak.authenticated ?? false)

  const token = computed(() => keycloak.token ?? '')

  const userName = computed(() => {
    const parsed = keycloak.tokenParsed
    return parsed?.preferred_username ?? parsed?.name ?? 'Broker'
  })

  const userEmail = computed(() => {
    return keycloak.tokenParsed?.email ?? ''
  })

  const userRoles = computed<string[]>(() => {
    const realmRoles = keycloak.tokenParsed?.realm_access?.roles ?? []
    const clientRoles = keycloak.tokenParsed?.resource_access?.['broker-app']?.roles ?? []
    return [...realmRoles, ...clientRoles]
  })

  const brokerId = computed(() => {
    return keycloak.tokenParsed?.sub ?? ''
  })

  const companyName = computed(() => {
    return keycloak.tokenParsed?.company_name ?? keycloak.tokenParsed?.organization ?? ''
  })

  async function refreshToken(): Promise<boolean> {
    try {
      const refreshed = await keycloak.updateToken(30)
      return refreshed
    } catch {
      await keycloak.login()
      return false
    }
  }

  async function logout() {
    const redirectUri = window.location.origin
    try {
      localStorage.removeItem('kc_token')
      localStorage.removeItem('kc_refresh_token')
      await keycloak.logout({ redirectUri })
    } catch {
      const logoutUrl = `${keycloak.authServerUrl}/realms/${keycloak.realm}/protocol/openid-connect/logout?client_id=broker-app&post_logout_redirect_uri=${encodeURIComponent(redirectUri)}`
      window.location.href = logoutUrl
    }
  }

  function hasRole(role: string): boolean {
    return userRoles.value.includes(role)
  }

  function isBroker(): boolean {
    return hasRole('broker_active') || hasRole('broker')
  }

  return {
    keycloak,
    isAuthenticated: readonly(isAuthenticated),
    token: readonly(token),
    userName: readonly(userName),
    userEmail: readonly(userEmail),
    userRoles: readonly(userRoles),
    brokerId: readonly(brokerId),
    companyName: readonly(companyName),
    refreshToken,
    logout,
    hasRole,
    isBroker,
  }
}
