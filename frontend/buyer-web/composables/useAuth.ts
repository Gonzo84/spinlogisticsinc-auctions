import { useAuthStore } from '~/stores/auth'
import type { User } from '~/types/user'
import { unwrapApiResponse } from '~/utils/api-response'

interface KeycloakInstance {
  login(options?: { redirectUri?: string; prompt?: string }): Promise<void>
  logout(options?: { redirectUri?: string }): Promise<void>
  register(options?: Record<string, unknown>): Promise<void>
  updateToken(minValidity: number): Promise<boolean>
  token?: string
  refreshToken?: string
  authServerUrl?: string
  realm?: string
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
  const wrongRole = computed(() => authStore.wrongRole)

  async function login(redirectUri?: string) {
    const keycloak = $keycloak as KeycloakInstance | undefined
    const targetUri = redirectUri || window.location.href

    if (keycloak) {
      try {
        // keycloak.login() should set window.location.href to Keycloak auth URL.
        // If it silently does nothing (adapter not fully ready), the timeout
        // fallback below will trigger a manual redirect.
        const loginPromise = keycloak.login({
          redirectUri: targetUri,
        })

        // Safety net: if the page hasn't navigated within 500ms, keycloak.login()
        // likely failed silently. Fall back to a manual PKCE redirect.
        setTimeout(() => {
          redirectToKeycloakManually(targetUri)
        }, 500)

        await loginPromise
      } catch {
        // If login() throws (e.g., adapter not fully initialised after logout),
        // fall back to a manual redirect to the Keycloak auth endpoint with PKCE.
        redirectToKeycloakManually(targetUri)
      }
    } else {
      // No keycloak instance available — redirect manually
      redirectToKeycloakManually(targetUri)
    }
  }

  function redirectToKeycloakManually(redirectUri: string) {
    const config = useRuntimeConfig()
    // Generate PKCE code verifier + challenge for Keycloak (required for buyer-web client)
    const codeVerifier = generateCodeVerifier()
    // Store verifier so keycloak-js can use it on the callback (same key keycloak-js uses)
    sessionStorage.setItem('kc-callback-' + (config.public.keycloakClientId as string), JSON.stringify({
      redirectUri,
      pkceCodeVerifier: codeVerifier,
      prompt: undefined,
    }))

    generateCodeChallenge(codeVerifier).then((codeChallenge) => {
      const authUrl = `${config.public.keycloakUrl}/realms/${config.public.keycloakRealm}/protocol/openid-connect/auth`
      const state = generateRandomString(32)
      const nonce = generateRandomString(32)
      const params = new URLSearchParams({
        client_id: config.public.keycloakClientId as string,
        redirect_uri: redirectUri,
        response_type: 'code',
        scope: 'openid',
        state,
        nonce,
        code_challenge: codeChallenge,
        code_challenge_method: 'S256',
      })
      window.location.href = `${authUrl}?${params.toString()}`
    })
  }

  function generateRandomString(length: number): string {
    const array = new Uint8Array(length)
    crypto.getRandomValues(array)
    return Array.from(array, (b) => b.toString(36)).join('').substring(0, length)
  }

  function generateCodeVerifier(): string {
    const array = new Uint8Array(32)
    crypto.getRandomValues(array)
    return btoa(String.fromCharCode(...array))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '')
  }

  async function generateCodeChallenge(verifier: string): Promise<string> {
    const encoder = new TextEncoder()
    const data = encoder.encode(verifier)
    const digest = await crypto.subtle.digest('SHA-256', data)
    return btoa(String.fromCharCode(...new Uint8Array(digest)))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '')
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

  async function switchAccount() {
    const keycloak = $keycloak as KeycloakInstance | undefined
    authStore.clearWrongRole()
    // Logout SSO session first so the user gets a fresh Keycloak login form
    // with empty email/password fields (not re-auth for the same user)
    if (keycloak?.authServerUrl && keycloak?.realm) {
      const logoutUrl = `${keycloak.authServerUrl}/realms/${keycloak.realm}/protocol/openid-connect/logout?client_id=buyer-web&post_logout_redirect_uri=${encodeURIComponent(window.location.origin)}`
      window.location.href = logoutUrl
    } else {
      // Fallback: redirect to Keycloak login manually
      redirectToKeycloakManually(window.location.origin)
    }
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
    wrongRole,
    login,
    logout,
    register,
    switchAccount,
    refreshToken,
    hasRole,
    updateProfile,
    requireAuth,
  }
}
