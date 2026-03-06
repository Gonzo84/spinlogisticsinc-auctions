import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ConfirmationService from 'primevue/confirmationservice'
import ToastService from 'primevue/toastservice'
import Tooltip from 'primevue/tooltip'
import { themeConfig, globalPT } from '@auction-platform/design-tokens'
import Keycloak from 'keycloak-js'
import App from './App.vue'
import router from './router'
import 'primeicons/primeicons.css'
import './assets/main.css'

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  realm: 'auction-platform',
  clientId: 'admin-dashboard',
})

try {
  await keycloak.init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false })
} catch (error) {
  console.error('[keycloak] Initialization failed:', error)
  document.body.innerHTML = `
    <div style="display:flex;align-items:center;justify-content:center;height:100vh;font-family:sans-serif">
      <div style="text-align:center;max-width:400px">
        <h2 style="margin-bottom:8px">Authentication Unavailable</h2>
        <p style="color:#666">Unable to connect to the authentication server. Please try again.</p>
        <button onclick="location.reload()" style="margin-top:16px;padding:8px 24px;background:#4f46e5;color:white;border:none;border-radius:6px;cursor:pointer">Retry</button>
      </div>
    </div>`
  throw error
}

// Verify the authenticated user has admin role — reject non-admin users
const realmRoles: string[] = keycloak.tokenParsed?.realm_access?.roles ?? []
const hasAdminRole = realmRoles.includes('admin_ops') || realmRoles.includes('admin_super')
if (!hasAdminRole) {
  document.body.innerHTML = `
    <div style="display:flex;align-items:center;justify-content:center;height:100vh;font-family:sans-serif">
      <div style="text-align:center;max-width:400px">
        <h2 style="margin-bottom:8px;color:#dc2626">Access Denied</h2>
        <p style="color:#666">You do not have administrator privileges to access this dashboard. Please contact your system administrator if you believe this is an error.</p>
        <button onclick="window.location.href='${keycloak.authServerUrl}/realms/${keycloak.realm}/protocol/openid-connect/logout?client_id=admin-dashboard&post_logout_redirect_uri=' + encodeURIComponent(window.location.origin)" style="margin-top:16px;padding:8px 24px;background:#4f46e5;color:white;border:none;border-radius:6px;cursor:pointer">Sign out &amp; return</button>
      </div>
    </div>`
  throw new Error('Access denied: user does not have admin_ops or admin_super role')
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
