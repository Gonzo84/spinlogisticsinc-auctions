<template>
  <div class="max-w-3xl mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold text-gray-900 mb-6">{{ $t('profile.title') }}</h1>

    <!-- Loading -->
    <div v-if="!user" class="bg-white border rounded-xl p-8 animate-pulse">
      <div class="flex items-center gap-4 mb-6">
        <div class="w-16 h-16 bg-gray-200 rounded-full" />
        <div>
          <div class="h-5 bg-gray-200 rounded w-40 mb-2" />
          <div class="h-4 bg-gray-200 rounded w-32" />
        </div>
      </div>
    </div>

    <template v-else>
      <!-- Profile Header -->
      <div class="bg-white border rounded-xl p-6 mb-6">
        <div class="flex items-center gap-4">
          <Avatar :label="initials" size="xlarge" shape="circle" class="bg-primary-100 text-primary font-bold" />
          <div>
            <h2 class="text-lg font-semibold text-gray-900">{{ fullName }}</h2>
            <p class="text-sm text-gray-500">{{ user.email }}</p>
            <span class="inline-block mt-1 px-2 py-0.5 text-xs font-medium rounded-full" :class="user.accountType === 'business' ? 'bg-primary-50 text-primary' : 'bg-gray-100 text-gray-600'">
              {{ user.accountType === 'business' ? $t('profile.businessAccount') : $t('profile.privateAccount') }}
            </span>
          </div>
        </div>
      </div>

      <!-- Personal Info -->
      <form class="space-y-6" @submit.prevent="handleSave">
        <div class="bg-white border rounded-xl p-6">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">{{ $t('profile.personalInfo') }}</h3>

          <div class="space-y-4">
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('profile.firstName') }}</label>
                <InputText
                  v-model="form.firstName"
                  class="w-full"
                />
              </div>
              <div>
                <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('profile.lastName') }}</label>
                <InputText
                  v-model="form.lastName"
                  class="w-full"
                />
              </div>
            </div>

            <div>
              <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('profile.email') }}</label>
              <InputText
                :model-value="user.email"
                type="email"
                disabled
                class="w-full"
              />
              <p class="text-xs text-gray-400 mt-1">{{ $t('profile.emailHint') }}</p>
            </div>

            <div>
              <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('profile.phone') }}</label>
              <InputText
                v-model="form.phone"
                type="tel"
                class="w-full"
              />
            </div>

            <div>
              <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('profile.country') }}</label>
              <Select
                v-model="form.country"
                :options="countryOptions"
                optionLabel="label"
                optionValue="value"
                :placeholder="$t('profile.selectCountry')"
                class="w-full"
              />
            </div>
          </div>
        </div>

        <!-- Business Details -->
        <div v-if="user.accountType === 'business'" class="bg-white border rounded-xl p-6">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">{{ $t('profile.businessDetails') }}</h3>

          <div class="space-y-4">
            <div>
              <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('profile.companyName') }}</label>
              <InputText
                v-model="form.company"
                class="w-full"
              />
            </div>

            <div>
              <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('profile.vatNumber') }}</label>
              <InputText
                v-model="form.vatNumber"
                class="w-full"
                placeholder="NL123456789B01"
              />
            </div>
          </div>
        </div>

        <!-- Notification Preferences -->
        <div class="bg-white border rounded-xl p-6">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">{{ $t('profile.notifications') }}</h3>

          <div class="space-y-3">
            <label class="flex items-center justify-between cursor-pointer">
              <div>
                <p class="text-sm font-medium text-gray-700">{{ $t('profile.emailNotifications') }}</p>
                <p class="text-xs text-gray-500">{{ $t('profile.emailNotificationsHint') }}</p>
              </div>
              <ToggleSwitch v-model="notifPrefs.email" />
            </label>

            <label class="flex items-center justify-between cursor-pointer">
              <div>
                <p class="text-sm font-medium text-gray-700">{{ $t('profile.overbidAlerts') }}</p>
                <p class="text-xs text-gray-500">{{ $t('profile.overbidAlertsHint') }}</p>
              </div>
              <ToggleSwitch v-model="notifPrefs.overbid" />
            </label>

            <label class="flex items-center justify-between cursor-pointer">
              <div>
                <p class="text-sm font-medium text-gray-700">{{ $t('profile.auctionClosing') }}</p>
                <p class="text-xs text-gray-500">{{ $t('profile.auctionClosingHint') }}</p>
              </div>
              <ToggleSwitch v-model="notifPrefs.closing" />
            </label>

            <label class="flex items-center justify-between cursor-pointer">
              <div>
                <p class="text-sm font-medium text-gray-700">{{ $t('profile.pushNotifications') }}</p>
                <p class="text-xs text-gray-500">{{ $t('profile.pushNotificationsHint') }}</p>
              </div>
              <ToggleSwitch v-model="notifPrefs.push" />
            </label>
          </div>
        </div>

        <!-- Success Message -->
        <Message v-if="showSuccess" severity="success" :closable="false">
          {{ $t('profile.saved') }}
        </Message>

        <!-- Error Message -->
        <Message v-if="saveError" severity="error" :closable="false">
          {{ saveError }}
        </Message>

        <!-- Save Button -->
        <div class="flex justify-end">
          <button
            type="submit"
            class="px-8 py-3 bg-primary text-white font-semibold rounded-lg hover:bg-primary-800 transition-colors disabled:opacity-50"
            :disabled="saving"
          >
            <span v-if="saving" class="flex items-center gap-2">
              <i class="pi pi-spinner pi-spin" />
              {{ $t('common.saving') }}
            </span>
            <span v-else>{{ $t('profile.saveChanges') }}</span>
          </button>
        </div>
      </form>
    </template>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ middleware: 'auth' })

const { t } = useI18n()
const { user, fullName, initials, updateProfile } = useAuth()

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

const saving = ref(false)
const showSuccess = ref(false)
const saveError = ref<string | null>(null)

const form = reactive({
  firstName: '',
  lastName: '',
  phone: '',
  country: '',
  company: '',
  vatNumber: '',
})

const notifPrefs = reactive({
  email: true,
  overbid: true,
  closing: true,
  push: false,
})

watch(user, (newUser) => {
  if (newUser) {
    form.firstName = newUser.firstName || ''
    form.lastName = newUser.lastName || ''
    form.phone = newUser.phone || ''
    form.country = newUser.country || ''
    form.company = newUser.company || ''
    form.vatNumber = newUser.vatNumber || ''
  }
}, { immediate: true })

async function handleSave() {
  saving.value = true
  saveError.value = null
  showSuccess.value = false

  try {
    await updateProfile({
      firstName: form.firstName,
      lastName: form.lastName,
      phone: form.phone,
      country: form.country,
      company: form.company,
      vatNumber: form.vatNumber,
    })
    showSuccess.value = true
    setTimeout(() => {
      showSuccess.value = false
    }, 3000)
  } catch (e: unknown) {
    saveError.value = e instanceof Error ? e.message : t('profile.saveFailed')
  } finally {
    saving.value = false
  }
}

useHead({
  title: t('profile.pageTitle'),
})
</script>
