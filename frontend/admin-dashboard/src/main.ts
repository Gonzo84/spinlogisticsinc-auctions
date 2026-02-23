import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Keycloak from 'keycloak-js'
import App from './App.vue'
import router from './router'
import './assets/main.css'

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  realm: 'auction-platform',
  clientId: 'admin-dashboard',
})

await keycloak.init({ onLoad: 'login-required', checkLoginIframe: false })

const app = createApp(App)
app.provide('keycloak', keycloak)
app.use(createPinia())
app.use(router)
app.mount('#app')
