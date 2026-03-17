<template>
  <div class="min-h-[80vh] flex items-center justify-center px-4 py-12">
    <div class="w-full max-w-lg">
      <!-- Header -->
      <div class="text-center mb-8">
        <NuxtLink to="/" class="inline-flex items-center gap-2 mb-6">
          <img src="/images/spc-logo.png" alt="SPC" class="h-10 w-auto" />
          <span class="text-xl font-bold text-gray-900">SPC Aukcije</span>
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
          <i class="pi pi-building inline-block mr-1.5 -mt-0.5" />
          {{ $t('auth.business') }}
        </button>
        <button
          class="flex-1 py-3 rounded-lg text-sm font-semibold transition-colors"
          :class="accountType === 'private'
            ? 'bg-white text-primary shadow-sm'
            : 'text-gray-500 hover:text-gray-700'"
          @click="accountType = 'private'"
        >
          <i class="pi pi-user inline-block mr-1.5 -mt-0.5" />
          {{ $t('auth.private') }}
        </button>
      </div>

      <!-- Registration Form -->
      <form class="space-y-4" @submit.prevent="handleRegister">
        <!-- Business-specific fields -->
        <template v-if="accountType === 'business'">
          <div>
            <label class="label">{{ $t('auth.companyName') }} *</label>
            <InputText
              v-model="form.companyName"
              required
              class="w-full"
              :placeholder="$t('auth.companyNamePlaceholder')"
            />
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">{{ $t('auth.vatNumber') }}</label>
              <InputText
                v-model="form.vatNumber"
                class="w-full"
                placeholder="NL123456789B01"
              />
            </div>
            <div>
              <label class="label">{{ $t('auth.chamberOfCommerce') }}</label>
              <InputText
                v-model="form.cocNumber"
                class="w-full"
                :placeholder="$t('auth.cocPlaceholder')"
              />
            </div>
          </div>
        </template>

        <!-- Name -->
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="label">{{ $t('auth.firstName') }} *</label>
            <InputText
              v-model="form.firstName"
              required
              class="w-full"
            />
          </div>
          <div>
            <label class="label">{{ $t('auth.lastName') }} *</label>
            <InputText
              v-model="form.lastName"
              required
              class="w-full"
            />
          </div>
        </div>

        <!-- Email -->
        <div>
          <label class="label">{{ $t('auth.email') }} *</label>
          <InputText
            v-model="form.email"
            type="email"
            required
            class="w-full"
            :placeholder="$t('auth.emailPlaceholder')"
          />
        </div>

        <!-- Phone -->
        <div>
          <label class="label">{{ $t('auth.phone') }} *</label>
          <InputText
            v-model="form.phone"
            type="tel"
            required
            class="w-full"
            placeholder="+31 6 12345678"
          />
        </div>

        <!-- Country -->
        <div>
          <label class="label">{{ $t('auth.country') }} *</label>
          <Select
            v-model="form.country"
            :options="countryOptions"
            optionLabel="label"
            optionValue="value"
            :placeholder="$t('auth.selectCountry')"
            class="w-full"
          />
        </div>

        <!-- Address (Business only) -->
        <template v-if="accountType === 'business'">
          <div>
            <label class="label">{{ $t('auth.address') }}</label>
            <InputText
              v-model="form.address"
              class="w-full"
              :placeholder="$t('auth.addressPlaceholder')"
            />
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">{{ $t('auth.postalCode') }}</label>
              <InputText
                v-model="form.postalCode"
                class="w-full"
              />
            </div>
            <div>
              <label class="label">{{ $t('auth.city') }}</label>
              <InputText
                v-model="form.city"
                class="w-full"
              />
            </div>
          </div>
        </template>

        <!-- Terms -->
        <label class="flex items-start gap-3 cursor-pointer">
          <Checkbox
            v-model="form.termsAccepted"
            :binary="true"
          />
          <span class="text-sm text-gray-600">
            {{ $t('auth.acceptTerms') }}
            <a href="#" class="text-primary hover:underline">{{ $t('auth.termsLink') }}</a>
            {{ $t('auth.and') }}
            <a href="#" class="text-primary hover:underline">{{ $t('auth.privacyLink') }}</a>
          </span>
        </label>

        <!-- Error -->
        <Message v-if="registerError" severity="error" :closable="false">
          {{ registerError }}
        </Message>

        <!-- Submit -->
        <Button
          type="submit"
          :label="submitting ? $t('auth.registering') : $t('auth.createAccountBtn')"
          :loading="submitting"
          :disabled="submitting"
          class="w-full"
          size="large"
        />

        <!-- Login Link -->
        <p class="text-center text-sm text-gray-500 mt-4">
          {{ $t('auth.alreadyHaveAccount') }}
          <Button type="button" :label="$t('auth.loginHere')" link @click="handleLogin" />
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

const countryOptions = [
  { label: 'Netherlands', value: 'NL' },
  { label: 'Germany', value: 'DE' },
  { label: 'France', value: 'FR' },
  { label: 'Belgium', value: 'BE' },
  { label: 'Poland', value: 'PL' },
  { label: 'Italy', value: 'IT' },
  { label: 'Romania', value: 'RO' },
  { label: 'Spain', value: 'ES' },
  { label: 'Austria', value: 'AT' },
]

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
  } catch (e: unknown) {
    registerError.value = e instanceof Error ? e.message : t('auth.registrationFailed')
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
