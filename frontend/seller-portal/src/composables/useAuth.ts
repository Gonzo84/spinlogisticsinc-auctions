import { inject, computed } from 'vue'
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
    return parsed?.preferred_username ?? parsed?.name ?? 'Seller'
  })

  const userEmail = computed(() => {
    return keycloak.tokenParsed?.email ?? ''
  })

  const userRoles = computed<string[]>(() => {
    const realmRoles = keycloak.tokenParsed?.realm_access?.roles ?? []
    const clientRoles = keycloak.tokenParsed?.resource_access?.['seller-portal']?.roles ?? []
    return [...realmRoles, ...clientRoles]
  })

  const sellerId = computed(() => {
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
      // Clear any locally stored tokens
      localStorage.removeItem('kc_token')
      localStorage.removeItem('kc_refresh_token')
      // keycloak.logout() redirects to Keycloak's end_session_endpoint,
      // which terminates the server-side SSO session and then redirects back
      await keycloak.logout({ redirectUri })
    } catch {
      // Fallback: if keycloak.logout() fails, manually redirect to the OIDC logout endpoint
      const logoutUrl = `${keycloak.authServerUrl}/realms/${keycloak.realm}/protocol/openid-connect/logout?client_id=seller-portal&post_logout_redirect_uri=${encodeURIComponent(redirectUri)}`
      window.location.href = logoutUrl
    }
  }

  function hasRole(role: string): boolean {
    return userRoles.value.includes(role)
  }

  return {
    keycloak,
    isAuthenticated,
    token,
    userName,
    userEmail,
    userRoles,
    sellerId,
    companyName,
    refreshToken,
    logout,
    hasRole,
  }
}
