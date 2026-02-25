import { useAuthStore } from '~/stores/auth'

export default defineNuxtPlugin((nuxtApp) => {
  const config = useRuntimeConfig()
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const authStore = useAuthStore(nuxtApp.$pinia as any)

  const api = $fetch.create({
    baseURL: config.public.apiBaseUrl,

    onRequest({ options }) {
      const headers = new Headers(options.headers as HeadersInit || {})

      if (authStore.token) {
        headers.set('Authorization', `Bearer ${authStore.token}`)
      }

      headers.set('Accept', 'application/json')
      headers.set('Content-Type', 'application/json')

      options.headers = headers
    },

    onResponseError({ response }) {
      const status = response.status

      if (status === 401) {
        authStore.clearSession()
        const { $keycloak } = nuxtApp
        if ($keycloak) {
          ($keycloak as { login: (options: { redirectUri: string }) => void }).login({
            redirectUri: window.location.href,
          })
        }
      }

      if (status === 403) {
        navigateTo('/')
      }

      if (status >= 500) {
        console.error('API Server Error:', response.status, response._data)
      }
    },
  })

  return {
    provide: {
      api,
    },
  }
})
