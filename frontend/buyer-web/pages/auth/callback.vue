<template>
  <div class="min-h-[80vh] flex items-center justify-center px-4">
    <div class="text-center">
      <!-- Loading State -->
      <div v-if="processing" class="space-y-4">
        <div class="mx-auto">
          <ProgressSpinner style="width: 64px; height: 64px" />
        </div>
        <h2 class="text-lg font-semibold text-gray-900">{{ $t('auth.processing') }}</h2>
        <p class="text-gray-500 text-sm">{{ $t('auth.pleaseWait') }}</p>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="space-y-4">
        <div class="w-16 h-16 mx-auto bg-warning-50 rounded-full flex items-center justify-center">
          <i class="pi pi-exclamation-triangle text-warning" style="font-size: 2rem" />
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
const { t } = useI18n()
const route = useRoute()
const { login, isAuthenticated } = useAuth()

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
        if (isAuthenticated.value) {
          resolve()
        } else if (keycloak.authenticated) {
          resolve()
        } else {
          setTimeout(checkAuth, 100)
        }
      }

      // Timeout after 10 seconds
      setTimeout(() => {
        resolve()
      }, 10000)

      checkAuth()
    })

    if (isAuthenticated.value) {
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
