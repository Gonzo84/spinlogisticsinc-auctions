import Keycloak from 'keycloak-js'
import { useAuthStore } from '~/stores/auth'

export default defineNuxtPlugin(async (nuxtApp) => {
  const config = useRuntimeConfig()

  const keycloak = new Keycloak({
    url: config.public.keycloakUrl,
    realm: config.public.keycloakRealm,
    clientId: config.public.keycloakClientId,
  })

  let tokenRefreshInterval: ReturnType<typeof setInterval> | null = null

  try {
    const authenticated = await keycloak.init({
      onLoad: 'check-sso',
      silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
      pkceMethod: 'S256',
      checkLoginIframe: false,
    })

    const authStore = useAuthStore(nuxtApp.$pinia as any)

    if (authenticated && keycloak.token && keycloak.tokenParsed) {
      authStore.setSession({
        user: {
          id: keycloak.tokenParsed.sub || '',
          email: keycloak.tokenParsed.email || '',
          firstName: keycloak.tokenParsed.given_name || '',
          lastName: keycloak.tokenParsed.family_name || '',
          company: keycloak.tokenParsed.company,
          vatNumber: keycloak.tokenParsed.vat_number,
          country: keycloak.tokenParsed.country,
          phone: keycloak.tokenParsed.phone_number,
          accountType: keycloak.tokenParsed.account_type || 'private',
          roles: keycloak.tokenParsed.realm_access?.roles || [],
          avatarUrl: keycloak.tokenParsed.avatar_url,
        },
        token: keycloak.token,
        refreshToken: keycloak.refreshToken || '',
      })

      // Set up token refresh interval (every 30 seconds)
      tokenRefreshInterval = setInterval(async () => {
        try {
          const refreshed = await keycloak.updateToken(60)
          if (refreshed && keycloak.token) {
            authStore.updateToken(keycloak.token, keycloak.refreshToken || undefined)
          }
        } catch {
          authStore.clearSession()
          if (tokenRefreshInterval) {
            clearInterval(tokenRefreshInterval)
          }
        }
      }, 30000)
    }

    // Handle token expiry
    keycloak.onTokenExpired = async () => {
      try {
        await keycloak.updateToken(30)
        if (keycloak.token) {
          authStore.updateToken(keycloak.token, keycloak.refreshToken || undefined)
        }
      } catch {
        authStore.clearSession()
      }
    }

    // Handle auth logout
    keycloak.onAuthLogout = () => {
      authStore.clearSession()
      if (tokenRefreshInterval) {
        clearInterval(tokenRefreshInterval)
      }
    }
  } catch (error) {
    console.error('Keycloak initialization failed:', error)
  }

  return {
    provide: {
      keycloak,
    },
  }
})
