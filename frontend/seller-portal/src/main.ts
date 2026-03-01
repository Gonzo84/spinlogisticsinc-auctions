import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ConfirmationService from 'primevue/confirmationservice'
import ToastService from 'primevue/toastservice'
import { themeConfig } from '@auction-platform/design-tokens'
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
app.use(PrimeVue, { theme: themeConfig })
app.use(ConfirmationService)
app.use(ToastService)
app.use(router)
app.mount('#app')
