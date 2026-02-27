import Keycloak from 'keycloak-js'
import { useAuthStore } from '~/stores/auth'

const TOKEN_KEY = 'kc_token'
const REFRESH_TOKEN_KEY = 'kc_refresh_token'

/**
 * Check if a JWT is expired by decoding its exp claim.
 * Returns true if expired or malformed (safe default: don't use bad tokens).
 */
function isJwtExpired(token: string, bufferSeconds = 30): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return typeof payload.exp !== 'number' || payload.exp < Math.ceil(Date.now() / 1000) + bufferSeconds
  } catch {
    return true
  }
}

export default defineNuxtPlugin(async (nuxtApp) => {
  const config = useRuntimeConfig()

  const keycloakConfig = {
    url: config.public.keycloakUrl,
    realm: config.public.keycloakRealm,
    clientId: config.public.keycloakClientId,
  }

  let keycloak = new Keycloak(keycloakConfig)

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
    // Token restoration strategy:
    //
    // 1. Decode the stored refresh token's `exp` claim BEFORE passing to init().
    //    If expired → discard (avoids a pointless 400 Bad Request from Keycloak).
    //    If valid → pass to init() for instant session restoration.
    //
    // 2. If stored tokens are rejected server-side (revoked, session killed),
    //    catch the error, create a fresh Keycloak instance (init() can only be
    //    called once per instance), and initialize unauthenticated.
    //
    // 3. No silentCheckSsoRedirectUri — the iframe SSO mechanism is incompatible
    //    with Nuxt 3's X-Frame-Options headers in dev. Buyer-web's primary mode is
    //    anonymous browsing, so SSO auto-login is not needed; users click "Log In".

    const storedToken = localStorage.getItem(TOKEN_KEY)
    const storedRefreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)

    const baseInitOptions = {
      pkceMethod: 'S256' as const,
      checkLoginIframe: false,
    }

    let authenticated = false

    if (storedToken && storedRefreshToken && !isJwtExpired(storedRefreshToken)) {
      // Refresh token is still valid — use stored tokens for instant auth
      if (import.meta.dev) {
        console.log('[keycloak] init with stored tokens (refresh token valid)')
      }
      try {
        authenticated = await keycloak.init({
          ...baseInitOptions,
          token: storedToken,
          refreshToken: storedRefreshToken,
        })
      } catch {
        // Tokens rejected server-side (revoked/session killed) — fresh start
        if (import.meta.dev) {
          console.warn('[keycloak] stored tokens rejected server-side, reinitializing')
        }
        clearPersistedTokens()
        keycloak = new Keycloak(keycloakConfig)
        authenticated = await keycloak.init(baseInitOptions)
      }
    } else {
      // No usable tokens — start unauthenticated (anonymous browsing)
      if (storedToken || storedRefreshToken) {
        if (import.meta.dev) {
          console.log('[keycloak] stored tokens expired, cleared')
        }
        clearPersistedTokens()
      }
      if (import.meta.dev) {
        console.log('[keycloak] init unauthenticated')
      }
      authenticated = await keycloak.init(baseInitOptions)
    }

    if (import.meta.dev) {
      console.log('[keycloak] init complete, authenticated:', authenticated)
    }

    const authStore = useAuthStore(nuxtApp.$pinia as ReturnType<typeof import('pinia').createPinia>)

    if (authenticated) {
      // Ensure tokens are fresh after restoration
      try {
        await keycloak.updateToken(30)
        persistTokens()
      } catch {
        clearPersistedTokens()
      }
      setupAuthSession(authStore)

      // Clean OIDC hash fragment and query params from URL after successful authentication
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
