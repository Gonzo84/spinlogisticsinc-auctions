<template>
  <div class="min-h-[80vh] flex items-center justify-center px-4 py-12">
    <div class="w-full max-w-lg">
      <!-- Header -->
      <div class="text-center mb-8">
        <NuxtLink to="/" class="inline-flex items-center gap-2 mb-6">
          <div class="w-10 h-10 bg-primary rounded-lg flex items-center justify-center">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          </div>
          <span class="text-xl font-bold text-gray-900">EU Auction</span>
        </NuxtLink>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('auth.createAccount') }}</h1>
        <p class="text-gray-500 mt-1">{{ $t('auth.registerSubtitle') }}</p>
      </div>

      <!-- Account Type Toggle -->
      <div class="bg-gray-100 rounded-xl p-1 flex mb-8">
        <button
          class="flex-1 py-3 rounded-lg text-sm font-semibold transition-colors"
          :class="accountType === 'business'
            ? 'bg-white text-primary shadow-sm'
            : 'text-gray-500 hover:text-gray-700'"
          @click="accountType = 'business'"
        >
          <svg class="w-5 h-5 inline-block mr-1.5 -mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
          </svg>
          {{ $t('auth.business') }}
        </button>
        <button
          class="flex-1 py-3 rounded-lg text-sm font-semibold transition-colors"
          :class="accountType === 'private'
            ? 'bg-white text-primary shadow-sm'
            : 'text-gray-500 hover:text-gray-700'"
          @click="accountType = 'private'"
        >
          <svg class="w-5 h-5 inline-block mr-1.5 -mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
          </svg>
          {{ $t('auth.private') }}
        </button>
      </div>

      <!-- Registration Form -->
      <form class="space-y-4" @submit.prevent="handleRegister">
        <!-- Business-specific fields -->
        <template v-if="accountType === 'business'">
          <div>
            <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('auth.companyName') }} *</label>
            <input
              v-model="form.companyName"
              type="text"
              required
              class="w-full px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
              :placeholder="$t('auth.companyNamePlaceholder')"
            >
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('auth.vatNumber') }}</label>
              <input
                v-model="form.vatNumber"
                type="text"
                class="w-full px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
                placeholder="NL123456789B01"
              >
            </div>
            <div>
              <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('auth.chamberOfCommerce') }}</label>
              <input
                v-model="form.cocNumber"
                type="text"
                class="w-full px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
                :placeholder="$t('auth.cocPlaceholder')"
              >
            </div>
          </div>
        </template>

        <!-- Name -->
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('auth.firstName') }} *</label>
            <input
              v-model="form.firstName"
              type="text"
              required
              class="w-full px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
            >
          </div>
          <div>
            <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('auth.lastName') }} *</label>
            <input
              v-model="form.lastName"
              type="text"
              required
              class="w-full px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
            >
          </div>
        </div>

        <!-- Email -->
        <div>
          <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('auth.email') }} *</label>
          <input
            v-model="form.email"
            type="email"
            required
            class="w-full px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
            :placeholder="$t('auth.emailPlaceholder')"
          >
        </div>

        <!-- Phone -->
        <div>
          <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('auth.phone') }} *</label>
          <input
            v-model="form.phone"
            type="tel"
            required
            class="w-full px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
            placeholder="+31 6 12345678"
          >
        </div>

        <!-- Country -->
        <div>
          <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('auth.country') }} *</label>
          <select
            v-model="form.country"
            required
            class="w-full px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
          >
            <option value="">{{ $t('auth.selectCountry') }}</option>
            <option value="NL">Netherlands</option>
            <option value="DE">Germany</option>
            <option value="FR">France</option>
            <option value="BE">Belgium</option>
            <option value="PL">Poland</option>
            <option value="IT">Italy</option>
            <option value="RO">Romania</option>
            <option value="ES">Spain</option>
            <option value="AT">Austria</option>
          </select>
        </div>

        <!-- Address (Business only) -->
        <template v-if="accountType === 'business'">
          <div>
            <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('auth.address') }}</label>
            <input
              v-model="form.address"
              type="text"
              class="w-full px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
              :placeholder="$t('auth.addressPlaceholder')"
            >
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('auth.postalCode') }}</label>
              <input
                v-model="form.postalCode"
                type="text"
                class="w-full px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
              >
            </div>
            <div>
              <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('auth.city') }}</label>
              <input
                v-model="form.city"
                type="text"
                class="w-full px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
              >
            </div>
          </div>
        </template>

        <!-- Terms -->
        <label class="flex items-start gap-3 cursor-pointer">
          <input
            v-model="form.termsAccepted"
            type="checkbox"
            required
            class="w-4 h-4 mt-0.5 rounded border-gray-300 text-primary focus:ring-primary"
          >
          <span class="text-sm text-gray-600">
            {{ $t('auth.acceptTerms') }}
            <a href="#" class="text-primary hover:underline">{{ $t('auth.termsLink') }}</a>
            {{ $t('auth.and') }}
            <a href="#" class="text-primary hover:underline">{{ $t('auth.privacyLink') }}</a>
          </span>
        </label>

        <!-- Error -->
        <div v-if="registerError" class="p-3 bg-warning-50 border border-warning-200 rounded-lg text-sm text-warning-800">
          {{ registerError }}
        </div>

        <!-- Submit -->
        <button
          type="submit"
          class="w-full py-3 bg-primary text-white font-bold rounded-lg hover:bg-primary-800 transition-colors disabled:opacity-50"
          :disabled="submitting"
        >
          <span v-if="submitting" class="flex items-center justify-center gap-2">
            <svg class="animate-spin w-5 h-5" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
            </svg>
            {{ $t('auth.registering') }}
          </span>
          <span v-else>{{ $t('auth.createAccountBtn') }}</span>
        </button>

        <!-- Login Link -->
        <p class="text-center text-sm text-gray-500 mt-4">
          {{ $t('auth.alreadyHaveAccount') }}
          <button type="button" class="text-primary font-medium hover:underline" @click="handleLogin">
            {{ $t('auth.loginHere') }}
          </button>
        </p>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
const { t } = useI18n()
const { register, login, isAuthenticated } = useAuth()

const accountType = ref<'business' | 'private'>('business')
const submitting = ref(false)
const registerError = ref<string | null>(null)

const form = reactive({
  companyName: '',
  vatNumber: '',
  cocNumber: '',
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  country: '',
  address: '',
  postalCode: '',
  city: '',
  termsAccepted: false,
})

// Redirect if already authenticated
watch(isAuthenticated, (auth) => {
  if (auth) {
    navigateTo('/my/purchases')
  }
}, { immediate: true })

async function handleRegister() {
  registerError.value = null
  submitting.value = true

  try {
    await register(accountType.value)
  } catch (e: any) {
    registerError.value = e?.message || t('auth.registrationFailed')
    submitting.value = false
  }
}

function handleLogin() {
  login()
}

useHead({
  title: t('auth.registerPageTitle'),
})
</script>
