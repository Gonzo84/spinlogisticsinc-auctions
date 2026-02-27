import { inject, computed } from 'vue'
import type Keycloak from 'keycloak-js'

export function useAuth() {
  const _keycloak = inject<Keycloak>('keycloak')

  if (!_keycloak) {
    throw new Error('Keycloak instance not provided. Ensure Keycloak is initialized in main.ts.')
  }

  const keycloak: Keycloak = _keycloak

  const token = computed(() => keycloak.token ?? '')
  const userName = computed(() => keycloak.tokenParsed?.preferred_username ?? 'Admin')
  const fullName = computed(() => {
    const parsed = keycloak.tokenParsed
    if (!parsed) return 'Admin User'
    return `${parsed.given_name ?? ''} ${parsed.family_name ?? ''}`.trim() || 'Admin User'
  })
  const email = computed(() => keycloak.tokenParsed?.email ?? '')
  const roles = computed<string[]>(() => {
    const realmRoles = keycloak.tokenParsed?.realm_access?.roles ?? []
    const clientRoles = keycloak.tokenParsed?.resource_access?.['admin-dashboard']?.roles ?? []
    return [...realmRoles, ...clientRoles]
  })
  const isAdmin = computed(() => roles.value.includes('admin_ops') || roles.value.includes('admin_super'))
  const isSuperAdmin = computed(() => roles.value.includes('admin_super'))

  async function refreshToken(): Promise<string> {
    try {
      const refreshed = await keycloak.updateToken(30)
      if (refreshed) {
        console.debug('Admin token refreshed')
      }
      return keycloak.token ?? ''
    } catch (err) {
      console.error('Failed to refresh admin token, re-authenticating', err)
      await keycloak.login()
      return keycloak.token ?? ''
    }
  }

  function logout() {
    localStorage.removeItem('kc_token')
    localStorage.removeItem('kc_refresh_token')
    try {
      keycloak.logout({ redirectUri: window.location.origin })
    } catch {
      // Fallback: manually redirect to OIDC logout endpoint
      const redirectUri = window.location.origin
      const logoutUrl = `${keycloak.authServerUrl}/realms/${keycloak.realm}/protocol/openid-connect/logout?client_id=admin-dashboard&post_logout_redirect_uri=${encodeURIComponent(redirectUri)}`
      window.location.href = logoutUrl
    }
  }

  function hasRole(role: string): boolean {
    return roles.value.includes(role)
  }

  return {
    keycloak,
    token,
    userName,
    fullName,
    email,
    roles,
    isAdmin,
    isSuperAdmin,
    refreshToken,
    logout,
    hasRole,
  }
}
