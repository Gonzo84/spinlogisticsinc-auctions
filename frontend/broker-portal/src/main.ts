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
  clientId: 'broker-app',
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

// Verify broker role
const roles = keycloak.tokenParsed?.realm_access?.roles ?? []
if (!roles.includes('broker_active') && !roles.includes('broker')) {
  const userEmail = keycloak.tokenParsed?.email || 'Unknown'
  const displayRoles = roles.filter((r: string) => !r.startsWith('default-roles-')).join(', ') || 'none'
  const logoutUrl = `${keycloak.authServerUrl}/realms/${keycloak.realm}/protocol/openid-connect/logout?client_id=broker-app&post_logout_redirect_uri=${encodeURIComponent(window.location.origin)}`

  const hasBuyerRole = roles.includes('buyer_active') || roles.includes('buyer_pending_kyc')
  const hasSellerRole = roles.includes('seller_verified') || roles.includes('seller_pending')
  const hasAdminRole = roles.includes('admin_ops') || roles.includes('admin_super')

  const buyerUrl = import.meta.env.VITE_BUYER_WEB_URL || 'http://localhost:3000'
  const sellerUrl = import.meta.env.VITE_SELLER_PORTAL_URL || 'http://localhost:5174'
  const adminUrl = import.meta.env.VITE_ADMIN_DASHBOARD_URL || 'http://localhost:5175'

  document.body.innerHTML = `
    <div style="display:flex;align-items:center;justify-content:center;height:100vh;font-family:system-ui,-apple-system,sans-serif">
      <div style="text-align:center;max-width:420px;padding:32px;border:1px solid #e5e7eb;border-radius:12px;background:#fff">
        <div style="font-size:2rem;color:#dc2626;margin-bottom:12px">&#128274;</div>
        <h2 style="margin:0 0 8px;font-size:1.25rem;color:#111827">Access Denied</h2>
        <p style="margin:0 0 4px;color:#6b7280;font-size:0.875rem">Signed in as <strong>${userEmail}</strong></p>
        <p style="margin:0 0 16px;color:#9ca3af;font-size:0.75rem">Roles: ${displayRoles}</p>
        <p style="margin:0 0 24px;color:#6b7280;font-size:0.875rem">The Broker Portal requires an active broker account to manage leads and lot intake.</p>
        <div style="display:flex;flex-direction:column;gap:8px">
          <button id="kc-switch-account" style="display:inline-block;padding:10px 24px;background:#4f46e5;color:white;border:none;border-radius:8px;cursor:pointer;font-size:0.875rem;font-weight:500;width:100%">Switch Account</button>
          <a href="${logoutUrl}" style="display:inline-block;padding:10px 24px;background:white;color:#4f46e5;border:1px solid #e5e7eb;border-radius:8px;cursor:pointer;text-decoration:none;font-size:0.875rem;font-weight:500">Sign Out</a>
        </div>
        ${hasBuyerRole || hasSellerRole || hasAdminRole ? `
        <div style="margin-top:24px;padding-top:16px;border-top:1px solid #e5e7eb">
          <p style="margin:0 0 8px;color:#9ca3af;font-size:0.75rem">Or go to your portal:</p>
          ${hasBuyerRole ? `<a href="${buyerUrl}" style="display:block;color:#4f46e5;font-size:0.875rem;margin:4px 0;text-decoration:none">Buyer Marketplace</a>` : ''}
          ${hasSellerRole ? `<a href="${sellerUrl}" style="display:block;color:#4f46e5;font-size:0.875rem;margin:4px 0;text-decoration:none">Seller Portal</a>` : ''}
          ${hasAdminRole ? `<a href="${adminUrl}" style="display:block;color:#4f46e5;font-size:0.875rem;margin:4px 0;text-decoration:none">Admin Dashboard</a>` : ''}
        </div>` : ''}
      </div>
    </div>`
  // Attach switch account handler — logout SSO session, then onLoad: 'login-required'
  // will show a fresh Keycloak login form on redirect back
  document.getElementById('kc-switch-account')?.addEventListener('click', () => {
    window.location.href = logoutUrl
  })
  throw new Error('Access denied: user does not have broker_active or broker role')
}

// Clean up OIDC hash/query fragments left by Keycloak after login redirect
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
