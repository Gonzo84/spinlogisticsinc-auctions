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

await keycloak.init({ onLoad: 'login-required', checkLoginIframe: false })

// Clean up OIDC hash/query fragments left by Keycloak after login redirect
// These interfere with Vue Router's createWebHistory() mode
// Use 100ms delay to ensure keycloak-js has finished processing the fragment
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
