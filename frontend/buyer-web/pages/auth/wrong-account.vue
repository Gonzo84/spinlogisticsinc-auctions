<template>
  <div class="min-h-[70vh] flex items-center justify-center px-4">
    <div class="w-full max-w-md text-center">
      <div class="card">
        <i class="pi pi-exclamation-circle text-4xl text-orange-500 mb-4" />
        <h1 class="text-xl font-bold text-gray-900 mb-2">{{ $t('auth.wrongAccountTitle') }}</h1>
        <p class="text-sm text-gray-600 mb-4">
          {{ $t('auth.signedInAs') }} <strong>{{ wrongRole?.email }}</strong>
        </p>
        <div v-if="wrongRole?.roles?.length" class="flex flex-wrap justify-center gap-2 mb-4">
          <Tag v-for="role in wrongRole.roles" :key="role" :value="formatRole(role)" severity="secondary" />
        </div>
        <p class="text-sm text-gray-500 mb-6">
          {{ $t('auth.wrongAccountMessage') }}
        </p>
        <div class="flex flex-col gap-3">
          <Button :label="$t('auth.switchAccount')" icon="pi pi-refresh" @click="handleSwitchAccount" />
          <Button :label="$t('auth.registerAsBuyer')" icon="pi pi-user-plus" severity="secondary" outlined @click="handleRegister" />
        </div>
        <div class="mt-6 border-t pt-4">
          <p class="text-xs text-gray-400 mb-2">{{ $t('auth.goToCorrectPortal') }}</p>
          <div class="flex flex-col gap-1">
            <a v-if="hasSellerRole" :href="sellerPortalUrl" class="text-sm text-primary hover:underline">
              {{ $t('auth.sellerPortal') }}
            </a>
            <a v-if="hasAdminRole" :href="adminDashboardUrl" class="text-sm text-primary hover:underline">
              {{ $t('auth.adminDashboard') }}
            </a>
            <a v-if="hasBrokerRole" :href="brokerPortalUrl" class="text-sm text-primary hover:underline">
              {{ $t('auth.brokerPortal') }}
            </a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '~/stores/auth'

const authStore = useAuthStore()
const { wrongRole, switchAccount, register } = useAuth()
const config = useRuntimeConfig()

// Restore wrongRole from sessionStorage if Pinia state was lost (full page reload)
onMounted(() => {
  if (!wrongRole.value) {
    const stored = sessionStorage.getItem('kc_wrong_role')
    if (stored) {
      try {
        authStore.setWrongRole(JSON.parse(stored))
      } catch {
        navigateTo('/')
      }
    } else {
      navigateTo('/')
    }
  }
})

const sellerPortalUrl = (config.public.sellerPortalUrl as string) || 'http://localhost:5174'
const adminDashboardUrl = (config.public.adminDashboardUrl as string) || 'http://localhost:5175'
const brokerPortalUrl = (config.public.brokerPortalUrl as string) || 'http://localhost:3003'

const hasSellerRole = computed(() => wrongRole.value?.roles?.some(r => r.includes('seller')) ?? false)
const hasAdminRole = computed(() => wrongRole.value?.roles?.some(r => r.includes('admin')) ?? false)
const hasBrokerRole = computed(() => wrongRole.value?.roles?.some(r => r.includes('broker')) ?? false)

function formatRole(role: string): string {
  return role.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
}

function handleSwitchAccount() {
  sessionStorage.removeItem('kc_wrong_role')
  switchAccount()
}

function handleRegister() {
  sessionStorage.removeItem('kc_wrong_role')
  register()
}
</script>
