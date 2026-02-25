<template>
  <div class="min-h-[80vh] flex items-center justify-center px-4">
    <div class="text-center">
      <!-- Loading State -->
      <div v-if="processing" class="space-y-4">
        <div class="w-16 h-16 mx-auto">
          <svg class="animate-spin w-full h-full text-primary" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
          </svg>
        </div>
        <h2 class="text-lg font-semibold text-gray-900">{{ $t('auth.processing') }}</h2>
        <p class="text-gray-500 text-sm">{{ $t('auth.pleaseWait') }}</p>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="space-y-4">
        <div class="w-16 h-16 mx-auto bg-warning-50 rounded-full flex items-center justify-center">
          <svg class="w-8 h-8 text-warning" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
          </svg>
        </div>
        <h2 class="text-lg font-semibold text-gray-900">{{ $t('auth.loginFailed') }}</h2>
        <p class="text-gray-500 text-sm">{{ error }}</p>
        <div class="flex items-center justify-center gap-3 mt-4">
          <button
            class="px-6 py-2 bg-primary text-white font-medium rounded-lg hover:bg-primary-800 transition-colors"
            @click="retryLogin"
          >
            {{ $t('auth.tryAgain') }}
          </button>
          <NuxtLink
            to="/"
            class="px-6 py-2 border font-medium rounded-lg hover:bg-gray-50 transition-colors"
          >
            {{ $t('auth.backToHome') }}
          </NuxtLink>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '~/stores/auth'

const { t } = useI18n()
const route = useRoute()
const { login } = useAuth()
const authStore = useAuthStore()

const processing = ref(true)
const error = ref<string | null>(null)

onMounted(async () => {
  try {
    // The Keycloak plugin (check-sso) handles the token exchange automatically
    // on page load. This callback page just waits for that process.
    const { $keycloak } = useNuxtApp()
    const keycloak = $keycloak as { authenticated?: boolean } | undefined

    if (!keycloak) {
      throw new Error('Authentication service not available')
    }

    // Wait for keycloak to be ready
    await new Promise<void>((resolve) => {
      const checkAuth = () => {
        if (authStore.isAuthenticated) {
          resolve()
        } else if (keycloak.authenticated) {
          resolve()
        } else {
          setTimeout(checkAuth, 100)
        }
      }

      // Timeout after 10 seconds
      const _timeout = setTimeout(() => {
        resolve()
      }, 10000)

      checkAuth()
    })

    if (authStore.isAuthenticated) {
      // Redirect to the original page or default
      const redirectTo = (route.query.redirect as string) || '/my/purchases'
      navigateTo(redirectTo)
    } else {
      // Check for error in query params
      const errorCode = route.query.error as string
      const errorDescription = route.query.error_description as string

      if (errorCode) {
        error.value = errorDescription || t('auth.unknownError')
      } else {
        error.value = t('auth.authenticationFailed')
      }
    }
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : t('auth.unknownError')
  } finally {
    processing.value = false
  }
})

function retryLogin() {
  login(`${window.location.origin}/auth/callback`)
}

useHead({
  title: t('auth.callbackPageTitle'),
})
</script>
