import Keycloak from 'keycloak-js'
import { useAuthStore } from '~/stores/auth'

const TOKEN_KEY = 'kc_token'
const REFRESH_TOKEN_KEY = 'kc_refresh_token'

export default defineNuxtPlugin(async (nuxtApp) => {
  const config = useRuntimeConfig()

  const keycloak = new Keycloak({
    url: config.public.keycloakUrl,
    realm: config.public.keycloakRealm,
    clientId: config.public.keycloakClientId,
  })

  let tokenRefreshInterval: ReturnType<typeof setInterval> | null = null

  function persistTokens() {
    if (keycloak.token) {
      localStorage.setItem(TOKEN_KEY, keycloak.token)
    }
    if (keycloak.refreshToken) {
      localStorage.setItem(REFRESH_TOKEN_KEY, keycloak.refreshToken)
    }
  }

  function clearPersistedTokens() {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  }

  function setupAuthSession(authStore: ReturnType<typeof useAuthStore>) {
    if (keycloak.token && keycloak.tokenParsed) {
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

      persistTokens()

      // Set up token refresh interval (every 30 seconds)
      if (tokenRefreshInterval) clearInterval(tokenRefreshInterval)
      tokenRefreshInterval = setInterval(async () => {
        try {
          const refreshed = await keycloak.updateToken(60)
          if (refreshed && keycloak.token) {
            authStore.updateToken(keycloak.token, keycloak.refreshToken || undefined)
            persistTokens()
          }
        } catch {
          authStore.clearSession()
          clearPersistedTokens()
          if (tokenRefreshInterval) {
            clearInterval(tokenRefreshInterval)
          }
        }
      }, 30000)
    }
  }

  try {
    // Restore persisted tokens if available
    const storedToken = localStorage.getItem(TOKEN_KEY)
    const storedRefreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)

    const initOptions: { pkceMethod: string; checkLoginIframe: boolean; token?: string; refreshToken?: string } = {
      pkceMethod: 'S256',
      checkLoginIframe: false,
    }

    if (storedToken && storedRefreshToken) {
      initOptions.token = storedToken
      initOptions.refreshToken = storedRefreshToken
    }

    console.log('[keycloak] init starting, hasStoredTokens:', !!storedToken)
    const authenticated = await keycloak.init(initOptions)
    console.log('[keycloak] init complete, authenticated:', authenticated)

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const authStore = useAuthStore(nuxtApp.$pinia as any)

    if (authenticated) {
      // If we restored from storage, ensure tokens are still valid
      try {
        await keycloak.updateToken(30)
        persistTokens()
      } catch {
        // Tokens expired, clear them
        clearPersistedTokens()
      }
      setupAuthSession(authStore)

      // Clean OIDC hash fragment and query params from URL after successful authentication
      // Use setTimeout to ensure cleanup runs after keycloak-js finishes internal processing
      setTimeout(() => {
        if ((window.location.hash && (window.location.hash.includes('state=') || window.location.hash.includes('session_state='))) ||
            (window.location.search && window.location.search.includes('code='))) {
          window.history.replaceState(null, '', window.location.pathname)
        }
      }, 0)
    } else {
      clearPersistedTokens()
    }

    // Handle token expiry
    keycloak.onTokenExpired = async () => {
      try {
        await keycloak.updateToken(30)
        if (keycloak.token) {
          authStore.updateToken(keycloak.token, keycloak.refreshToken || undefined)
          persistTokens()
        }
      } catch {
        authStore.clearSession()
        clearPersistedTokens()
      }
    }

    // Handle auth logout
    keycloak.onAuthLogout = () => {
      authStore.clearSession()
      clearPersistedTokens()
      if (tokenRefreshInterval) {
        clearInterval(tokenRefreshInterval)
      }
    }

    // Handle successful auth after login redirect
    keycloak.onAuthSuccess = () => {
      setupAuthSession(authStore)
    }
  } catch (error) {
    console.error('[keycloak] Initialization failed:', error)
  }

  return {
    provide: {
      keycloak,
    },
  }
})
