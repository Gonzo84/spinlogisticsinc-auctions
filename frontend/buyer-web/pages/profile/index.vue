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
          <div class="w-16 h-16 rounded-full bg-primary-100 flex items-center justify-center text-primary text-xl font-bold">
            {{ initials }}
          </div>
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
                <input
                  v-model="form.firstName"
                  type="text"
                  class="w-full px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
                >
              </div>
              <div>
                <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('profile.lastName') }}</label>
                <input
                  v-model="form.lastName"
                  type="text"
                  class="w-full px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
                >
              </div>
            </div>

            <div>
              <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('profile.email') }}</label>
              <input
                :value="user.email"
                type="email"
                disabled
                class="w-full px-4 py-2.5 border rounded-lg bg-gray-50 text-gray-500 cursor-not-allowed"
              >
              <p class="text-xs text-gray-400 mt-1">{{ $t('profile.emailHint') }}</p>
            </div>

            <div>
              <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('profile.phone') }}</label>
              <input
                v-model="form.phone"
                type="tel"
                class="w-full px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
              >
            </div>

            <div>
              <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('profile.country') }}</label>
              <select
                v-model="form.country"
                class="w-full px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
              >
                <option value="">{{ $t('profile.selectCountry') }}</option>
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
          </div>
        </div>

        <!-- Business Details -->
        <div v-if="user.accountType === 'business'" class="bg-white border rounded-xl p-6">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">{{ $t('profile.businessDetails') }}</h3>

          <div class="space-y-4">
            <div>
              <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('profile.companyName') }}</label>
              <input
                v-model="form.company"
                type="text"
                class="w-full px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
              >
            </div>

            <div>
              <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('profile.vatNumber') }}</label>
              <input
                v-model="form.vatNumber"
                type="text"
                class="w-full px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
                placeholder="NL123456789B01"
              >
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
              <button
                type="button"
                class="relative w-11 h-6 rounded-full transition-colors"
                :class="notifPrefs.email ? 'bg-primary' : 'bg-gray-300'"
                @click="notifPrefs.email = !notifPrefs.email"
              >
                <span
                  class="absolute top-0.5 w-5 h-5 bg-white rounded-full shadow transition-transform"
                  :class="notifPrefs.email ? 'translate-x-[22px]' : 'translate-x-0.5'"
                />
              </button>
            </label>

            <label class="flex items-center justify-between cursor-pointer">
              <div>
                <p class="text-sm font-medium text-gray-700">{{ $t('profile.overbidAlerts') }}</p>
                <p class="text-xs text-gray-500">{{ $t('profile.overbidAlertsHint') }}</p>
              </div>
              <button
                type="button"
                class="relative w-11 h-6 rounded-full transition-colors"
                :class="notifPrefs.overbid ? 'bg-primary' : 'bg-gray-300'"
                @click="notifPrefs.overbid = !notifPrefs.overbid"
              >
                <span
                  class="absolute top-0.5 w-5 h-5 bg-white rounded-full shadow transition-transform"
                  :class="notifPrefs.overbid ? 'translate-x-[22px]' : 'translate-x-0.5'"
                />
              </button>
            </label>

            <label class="flex items-center justify-between cursor-pointer">
              <div>
                <p class="text-sm font-medium text-gray-700">{{ $t('profile.auctionClosing') }}</p>
                <p class="text-xs text-gray-500">{{ $t('profile.auctionClosingHint') }}</p>
              </div>
              <button
                type="button"
                class="relative w-11 h-6 rounded-full transition-colors"
                :class="notifPrefs.closing ? 'bg-primary' : 'bg-gray-300'"
                @click="notifPrefs.closing = !notifPrefs.closing"
              >
                <span
                  class="absolute top-0.5 w-5 h-5 bg-white rounded-full shadow transition-transform"
                  :class="notifPrefs.closing ? 'translate-x-[22px]' : 'translate-x-0.5'"
                />
              </button>
            </label>

            <label class="flex items-center justify-between cursor-pointer">
              <div>
                <p class="text-sm font-medium text-gray-700">{{ $t('profile.pushNotifications') }}</p>
                <p class="text-xs text-gray-500">{{ $t('profile.pushNotificationsHint') }}</p>
              </div>
              <button
                type="button"
                class="relative w-11 h-6 rounded-full transition-colors"
                :class="notifPrefs.push ? 'bg-primary' : 'bg-gray-300'"
                @click="notifPrefs.push = !notifPrefs.push"
              >
                <span
                  class="absolute top-0.5 w-5 h-5 bg-white rounded-full shadow transition-transform"
                  :class="notifPrefs.push ? 'translate-x-[22px]' : 'translate-x-0.5'"
                />
              </button>
            </label>
          </div>
        </div>

        <!-- Success Message -->
        <Transition
          enter-active-class="transition ease-out duration-200"
          enter-from-class="opacity-0 -translate-y-2"
          enter-to-class="opacity-100 translate-y-0"
          leave-active-class="transition ease-in duration-150"
          leave-from-class="opacity-100"
          leave-to-class="opacity-0"
        >
          <div
            v-if="showSuccess"
            class="flex items-center gap-2 p-4 bg-secondary-50 border border-secondary-200 rounded-lg text-secondary-800"
          >
            <svg class="w-5 h-5 text-secondary shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
            <span class="text-sm font-medium">{{ $t('profile.saved') }}</span>
          </div>
        </Transition>

        <!-- Error Message -->
        <div v-if="saveError" class="p-4 bg-warning-50 border border-warning-200 rounded-lg text-sm text-warning-800">
          {{ saveError }}
        </div>

        <!-- Save Button -->
        <div class="flex justify-end">
          <button
            type="submit"
            class="px-8 py-3 bg-primary text-white font-semibold rounded-lg hover:bg-primary-800 transition-colors disabled:opacity-50"
            :disabled="saving"
          >
            <span v-if="saving" class="flex items-center gap-2">
              <svg class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
              </svg>
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
const { t } = useI18n()
const { user, fullName, initials, updateProfile, requireAuth } = useAuth()

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

onMounted(() => {
  if (!requireAuth('/profile')) return

  // Populate form with user data
  if (user.value) {
    form.firstName = user.value.firstName || ''
    form.lastName = user.value.lastName || ''
    form.phone = user.value.phone || ''
    form.country = user.value.country || ''
    form.company = user.value.company || ''
    form.vatNumber = user.value.vatNumber || ''
  }
})

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
  } catch (e: any) {
    saveError.value = e?.message || t('profile.saveFailed')
  } finally {
    saving.value = false
  }
}

useHead({
  title: t('profile.pageTitle'),
})
</script>
