<template>
  <div class="min-h-[80vh] flex items-center justify-center px-4 py-12">
    <div class="w-full max-w-lg">
      <!-- Header -->
      <div class="text-center mb-8">
        <NuxtLink to="/" class="inline-flex items-center gap-2 mb-6">
          <img src="/images/spin-logo.png" alt="Spin Logistics" class="h-10 w-auto" />
          <span class="text-xl font-bold text-gray-900">Spin Logistics</span>
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

          <div>
            <label class="label">{{ $t('auth.entityType') }} *</label>
            <Select
              v-model="form.entityType"
              :options="entityTypeOptions"
              optionLabel="label"
              optionValue="value"
              :placeholder="$t('auth.selectEntityType')"
              class="w-full"
            />
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">{{ $t('auth.ein') }}</label>
              <InputText
                v-model="form.ein"
                class="w-full"
                placeholder="XX-XXXXXXX"
              />
            </div>
            <div>
              <label class="label">{{ $t('auth.stateRegistration') }}</label>
              <InputText
                v-model="form.stateRegistration"
                class="w-full"
                :placeholder="$t('auth.stateRegistrationPlaceholder')"
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
            placeholder="+1 (312) 555-0123"
          />
        </div>

        <!-- State -->
        <div>
          <label class="label">{{ $t('auth.state') }} *</label>
          <Select
            v-model="form.state"
            :options="stateOptions"
            optionLabel="label"
            optionValue="value"
            :placeholder="$t('auth.selectState')"
            class="w-full"
            filter
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

const stateOptions = [
  { label: 'Alabama', value: 'AL' },
  { label: 'Alaska', value: 'AK' },
  { label: 'Arizona', value: 'AZ' },
  { label: 'Arkansas', value: 'AR' },
  { label: 'California', value: 'CA' },
  { label: 'Colorado', value: 'CO' },
  { label: 'Connecticut', value: 'CT' },
  { label: 'Delaware', value: 'DE' },
  { label: 'District of Columbia', value: 'DC' },
  { label: 'Florida', value: 'FL' },
  { label: 'Georgia', value: 'GA' },
  { label: 'Hawaii', value: 'HI' },
  { label: 'Idaho', value: 'ID' },
  { label: 'Illinois', value: 'IL' },
  { label: 'Indiana', value: 'IN' },
  { label: 'Iowa', value: 'IA' },
  { label: 'Kansas', value: 'KS' },
  { label: 'Kentucky', value: 'KY' },
  { label: 'Louisiana', value: 'LA' },
  { label: 'Maine', value: 'ME' },
  { label: 'Maryland', value: 'MD' },
  { label: 'Massachusetts', value: 'MA' },
  { label: 'Michigan', value: 'MI' },
  { label: 'Minnesota', value: 'MN' },
  { label: 'Mississippi', value: 'MS' },
  { label: 'Missouri', value: 'MO' },
  { label: 'Montana', value: 'MT' },
  { label: 'Nebraska', value: 'NE' },
  { label: 'Nevada', value: 'NV' },
  { label: 'New Hampshire', value: 'NH' },
  { label: 'New Jersey', value: 'NJ' },
  { label: 'New Mexico', value: 'NM' },
  { label: 'New York', value: 'NY' },
  { label: 'North Carolina', value: 'NC' },
  { label: 'North Dakota', value: 'ND' },
  { label: 'Ohio', value: 'OH' },
  { label: 'Oklahoma', value: 'OK' },
  { label: 'Oregon', value: 'OR' },
  { label: 'Pennsylvania', value: 'PA' },
  { label: 'Rhode Island', value: 'RI' },
  { label: 'South Carolina', value: 'SC' },
  { label: 'South Dakota', value: 'SD' },
  { label: 'Tennessee', value: 'TN' },
  { label: 'Texas', value: 'TX' },
  { label: 'Utah', value: 'UT' },
  { label: 'Vermont', value: 'VT' },
  { label: 'Virginia', value: 'VA' },
  { label: 'Washington', value: 'WA' },
  { label: 'West Virginia', value: 'WV' },
  { label: 'Wisconsin', value: 'WI' },
  { label: 'Wyoming', value: 'WY' },
]

const entityTypeOptions = [
  { label: t('auth.entityLLC'), value: 'LLC' },
  { label: t('auth.entityCCorp'), value: 'C-Corp' },
  { label: t('auth.entitySCorp'), value: 'S-Corp' },
  { label: t('auth.entityLP'), value: 'LP' },
  { label: t('auth.entitySoleProp'), value: 'Sole Proprietorship' },
]

const form = reactive({
  companyName: '',
  ein: '',
  stateRegistration: '',
  entityType: '',
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  state: '',
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
