import { inject, computed } from 'vue'
import type Keycloak from 'keycloak-js'

export function useAuth() {
  const keycloak = inject<Keycloak>('keycloak')

  if (!keycloak) {
    throw new Error('Keycloak instance not provided. Ensure Keycloak is initialized in main.ts.')
  }

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
  const isAdmin = computed(() => roles.value.includes('admin') || roles.value.includes('platform-admin'))
  const isSuperAdmin = computed(() => roles.value.includes('super-admin'))

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
    keycloak.logout({ redirectUri: window.location.origin })
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
