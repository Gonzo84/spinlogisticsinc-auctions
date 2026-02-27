import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ConfirmationService from 'primevue/confirmationservice'
import Aura from '@primeuix/themes/aura'
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

// Clean up OIDC hash/query fragments left by Keycloak after login redirect
// These interfere with Vue Router's createWebHistory() mode
setTimeout(() => {
  if (
    (window.location.hash && (window.location.hash.includes('state=') || window.location.hash.includes('session_state='))) ||
    (window.location.search && window.location.search.includes('code='))
  ) {
    window.history.replaceState(null, '', window.location.pathname)
  }
}, 100)

const app = createApp(App)
app.provide('keycloak', keycloak)
app.use(createPinia())
app.use(PrimeVue, { theme: { preset: Aura } })
app.use(ConfirmationService)
app.use(router)
app.mount('#app')
