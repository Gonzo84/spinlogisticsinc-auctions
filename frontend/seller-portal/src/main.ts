import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ConfirmationService from 'primevue/confirmationservice'
import ToastService from 'primevue/toastservice'
import Tooltip from 'primevue/tooltip'
import { themeConfig, globalPT } from '@auction-platform/design-tokens'
import 'primeicons/primeicons.css'
import Keycloak from 'keycloak-js'
import App from './App.vue'
import router from './router'
import './assets/main.css'

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  realm: 'auction-platform',
  clientId: 'seller-portal',
})

try {
  await keycloak.init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false })
} catch (initError: unknown) {
  console.error('[keycloak] Initialization failed:', initError)
  document.body.innerHTML = `
    <div style="display:flex;align-items:center;justify-content:center;height:100vh;font-family:sans-serif">
      <div style="text-align:center;max-width:400px">
        <h2 style="margin-bottom:8px">Authentication Unavailable</h2>
        <p style="color:#666">Unable to connect to the authentication server. Please try again.</p>
        <button onclick="location.reload()" style="margin-top:16px;padding:8px 24px;background:#4f46e5;color:white;border:none;border-radius:6px;cursor:pointer">Retry</button>
      </div>
    </div>`
  throw initError
}

// Verify the authenticated user has seller role — reject cross-portal SSO sessions
const realmRoles: string[] = keycloak.tokenParsed?.realm_access?.roles ?? []
const hasSellerRole = realmRoles.includes('seller_verified') || realmRoles.includes('seller_pending')
if (!hasSellerRole) {
  const userEmail = keycloak.tokenParsed?.email || 'Unknown'
  const userName = [keycloak.tokenParsed?.given_name, keycloak.tokenParsed?.family_name].filter(Boolean).join(' ')
  const displayRoles = realmRoles.filter(r => !r.startsWith('default-roles-')).join(', ') || 'none'

  const logoutUrl = `${keycloak.authServerUrl}/realms/${keycloak.realm}/protocol/openid-connect/logout?client_id=seller-portal&post_logout_redirect_uri=${encodeURIComponent(window.location.origin)}`

  const hasBuyerRole = realmRoles.includes('buyer_active') || realmRoles.includes('buyer_pending_kyc')
  const hasAdminRole = realmRoles.includes('admin_ops') || realmRoles.includes('admin_super')
  const hasBrokerRole = realmRoles.includes('broker_active') || realmRoles.includes('broker')

  const buyerUrl = import.meta.env.VITE_BUYER_WEB_URL || 'http://localhost:3000'
  const adminUrl = import.meta.env.VITE_ADMIN_DASHBOARD_URL || 'http://localhost:5175'
  const brokerUrl = import.meta.env.VITE_BROKER_PORTAL_URL || 'http://localhost:3003'

  document.body.innerHTML = `
    <div style="display:flex;align-items:center;justify-content:center;height:100vh;font-family:system-ui,-apple-system,sans-serif">
      <div style="text-align:center;max-width:420px;padding:32px;border:1px solid #e5e7eb;border-radius:12px;background:#fff">
        <div style="font-size:2rem;color:#f59e0b;margin-bottom:12px">&#9888;</div>
        <h2 style="margin:0 0 8px;font-size:1.25rem;color:#111827">Wrong Account</h2>
        <p style="margin:0 0 4px;color:#6b7280;font-size:0.875rem">Signed in as <strong>${userEmail}</strong></p>
        ${userName ? `<p style="margin:0 0 4px;color:#6b7280;font-size:0.875rem">${userName}</p>` : ''}
        <p style="margin:0 0 16px;color:#9ca3af;font-size:0.75rem">Roles: ${displayRoles}</p>
        <p style="margin:0 0 24px;color:#6b7280;font-size:0.875rem">The Seller Portal requires a verified seller account to manage listings and auctions.</p>
        <div style="display:flex;flex-direction:column;gap:8px">
          <button id="kc-switch-account" style="display:inline-block;padding:10px 24px;background:#4f46e5;color:white;border:none;border-radius:8px;cursor:pointer;font-size:0.875rem;font-weight:500;width:100%">Switch Account</button>
          <a href="${logoutUrl}" style="display:inline-block;padding:10px 24px;background:white;color:#4f46e5;border:1px solid #e5e7eb;border-radius:8px;cursor:pointer;text-decoration:none;font-size:0.875rem;font-weight:500">Sign Out</a>
        </div>
        ${hasBuyerRole || hasAdminRole || hasBrokerRole ? `
        <div style="margin-top:24px;padding-top:16px;border-top:1px solid #e5e7eb">
          <p style="margin:0 0 8px;color:#9ca3af;font-size:0.75rem">Or go to your portal:</p>
          ${hasBuyerRole ? `<a href="${buyerUrl}" style="display:block;color:#4f46e5;font-size:0.875rem;margin:4px 0;text-decoration:none">Buyer Marketplace</a>` : ''}
          ${hasAdminRole ? `<a href="${adminUrl}" style="display:block;color:#4f46e5;font-size:0.875rem;margin:4px 0;text-decoration:none">Admin Dashboard</a>` : ''}
          ${hasBrokerRole ? `<a href="${brokerUrl}" style="display:block;color:#4f46e5;font-size:0.875rem;margin:4px 0;text-decoration:none">Broker Portal</a>` : ''}
        </div>` : ''}
      </div>
    </div>`
  // Attach switch account handler — logout first to clear SSO session, then Keycloak
  // login page will show a fresh form where the user can enter different credentials
  document.getElementById('kc-switch-account')?.addEventListener('click', () => {
    const logoutAndLogin = `${keycloak.authServerUrl}/realms/${keycloak.realm}/protocol/openid-connect/logout?client_id=seller-portal&post_logout_redirect_uri=${encodeURIComponent(window.location.origin)}`
    window.location.href = logoutAndLogin
  })
  throw new Error('Access denied: user does not have seller_verified or seller_pending role')
}

// Clean up OIDC hash/query fragments left by Keycloak after login redirect
// These interfere with Vue Router's createWebHistory() mode
if (
  (window.location.hash && (window.location.hash.includes('state=') || window.location.hash.includes('session_state='))) ||
  (window.location.search && window.location.search.includes('code='))
) {
  window.history.replaceState(null, '', window.location.pathname)
}

const app = createApp(App)
app.provide('keycloak', keycloak)
app.use(createPinia())
app.use(PrimeVue, { theme: themeConfig, pt: globalPT })
app.use(ConfirmationService)
app.use(ToastService)
app.directive('tooltip', Tooltip)
app.use(router)
app.mount('#app')
