<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useApi } from '@/composables/useApi'

const { userName, userEmail, companyName } = useAuth()
const { get, put, loading, error } = useApi()

const activeTab = ref<'company' | 'bank' | 'notifications'>('company')

const companyForm = reactive({
  companyName: '',
  vatNumber: '',
  registrationNumber: '',
  address: '',
  city: '',
  postalCode: '',
  country: '',
  phone: '',
  website: '',
  contactEmail: '',
})

const bankForm = reactive({
  accountHolder: '',
  iban: '',
  bic: '',
  bankName: '',
  currency: 'EUR',
})

const notificationPrefs = reactive({
  emailOnNewBid: true,
  emailOnLotApproved: true,
  emailOnLotSold: true,
  emailOnSettlementPaid: true,
  emailDailyDigest: false,
  emailWeeklyReport: true,
  pushOnNewBid: true,
  pushOnLotSold: true,
})

const saveSuccess = ref(false)
const saveError = ref<string | null>(null)

const countries = [
  'Austria', 'Belgium', 'Bulgaria', 'Croatia', 'Cyprus', 'Czech Republic',
  'Denmark', 'Estonia', 'Finland', 'France', 'Germany', 'Greece',
  'Hungary', 'Ireland', 'Italy', 'Latvia', 'Lithuania', 'Luxembourg',
  'Malta', 'Netherlands', 'Poland', 'Portugal', 'Romania', 'Slovakia',
  'Slovenia', 'Spain', 'Sweden',
]

onMounted(async () => {
  try {
    const profile = await get<any>('/seller/profile')
    if (profile.company) {
      Object.assign(companyForm, profile.company)
    }
    if (profile.bankDetails) {
      Object.assign(bankForm, profile.bankDetails)
    }
    if (profile.notifications) {
      Object.assign(notificationPrefs, profile.notifications)
    }
  } catch {
    // Use defaults if profile not yet created
  }
})

async function saveCompany() {
  saveSuccess.value = false
  saveError.value = null
  try {
    await put('/seller/profile/company', companyForm)
    saveSuccess.value = true
    setTimeout(() => { saveSuccess.value = false }, 3000)
  } catch (err: any) {
    saveError.value = err?.response?.data?.message ?? 'Failed to save company details.'
  }
}

async function saveBank() {
  saveSuccess.value = false
  saveError.value = null
  try {
    await put('/seller/profile/bank', bankForm)
    saveSuccess.value = true
    setTimeout(() => { saveSuccess.value = false }, 3000)
  } catch (err: any) {
    saveError.value = err?.response?.data?.message ?? 'Failed to save bank details.'
  }
}

async function saveNotifications() {
  saveSuccess.value = false
  saveError.value = null
  try {
    await put('/seller/profile/notifications', notificationPrefs)
    saveSuccess.value = true
    setTimeout(() => { saveSuccess.value = false }, 3000)
  } catch (err: any) {
    saveError.value = err?.response?.data?.message ?? 'Failed to save notification preferences.'
  }
}
</script>

<template>
  <div>
    <!-- Page header -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">Profile Settings</h1>
      <p class="mt-1 text-sm text-gray-500">Manage your company details, bank settings, and notification preferences.</p>
    </div>

    <!-- User info card -->
    <div class="card mb-6">
      <div class="flex items-center gap-4">
        <div class="flex h-14 w-14 items-center justify-center rounded-full bg-seller-100 text-xl font-bold text-seller-700">
          {{ userName.charAt(0).toUpperCase() }}
        </div>
        <div>
          <p class="text-lg font-semibold text-gray-900">{{ userName }}</p>
          <p class="text-sm text-gray-500">{{ userEmail }}</p>
          <p v-if="companyName" class="text-sm text-gray-500">{{ companyName }}</p>
        </div>
      </div>
    </div>

    <!-- Success/Error banner -->
    <div v-if="saveSuccess" class="mb-4 rounded-lg border border-green-200 bg-green-50 p-3">
      <p class="text-sm font-medium text-green-800">Settings saved successfully.</p>
    </div>
    <div v-if="saveError" class="mb-4 rounded-lg border border-red-200 bg-red-50 p-3">
      <p class="text-sm font-medium text-red-800">{{ saveError }}</p>
    </div>

    <!-- Tabs -->
    <div class="mb-6 border-b border-gray-200">
      <nav class="-mb-px flex gap-6">
        <button
          :class="[
            'border-b-2 pb-3 pt-1 text-sm font-medium transition-colors',
            activeTab === 'company'
              ? 'border-primary-500 text-primary-600'
              : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700',
          ]"
          @click="activeTab = 'company'"
        >
          Company Details
        </button>
        <button
          :class="[
            'border-b-2 pb-3 pt-1 text-sm font-medium transition-colors',
            activeTab === 'bank'
              ? 'border-primary-500 text-primary-600'
              : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700',
          ]"
          @click="activeTab = 'bank'"
        >
          Bank Settings
        </button>
        <button
          :class="[
            'border-b-2 pb-3 pt-1 text-sm font-medium transition-colors',
            activeTab === 'notifications'
              ? 'border-primary-500 text-primary-600'
              : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700',
          ]"
          @click="activeTab = 'notifications'"
        >
          Notifications
        </button>
      </nav>
    </div>

    <!-- Company Details Tab -->
    <form v-if="activeTab === 'company'" class="card" @submit.prevent="saveCompany">
      <h2 class="mb-4 text-lg font-semibold text-gray-900">Company Details</h2>
      <div class="grid gap-4 sm:grid-cols-2">
        <div class="sm:col-span-2">
          <label class="label" for="company-name">Company Name</label>
          <input id="company-name" v-model="companyForm.companyName" type="text" class="input" placeholder="Acme Industries B.V." />
        </div>
        <div>
          <label class="label" for="vat-number">VAT Number</label>
          <input id="vat-number" v-model="companyForm.vatNumber" type="text" class="input" placeholder="NL123456789B01" />
        </div>
        <div>
          <label class="label" for="reg-number">Registration Number</label>
          <input id="reg-number" v-model="companyForm.registrationNumber" type="text" class="input" placeholder="12345678" />
        </div>
        <div class="sm:col-span-2">
          <label class="label" for="company-address">Address</label>
          <input id="company-address" v-model="companyForm.address" type="text" class="input" placeholder="123 Business Street" />
        </div>
        <div>
          <label class="label" for="company-city">City</label>
          <input id="company-city" v-model="companyForm.city" type="text" class="input" placeholder="Amsterdam" />
        </div>
        <div>
          <label class="label" for="company-postal">Postal Code</label>
          <input id="company-postal" v-model="companyForm.postalCode" type="text" class="input" placeholder="1012 AB" />
        </div>
        <div>
          <label class="label" for="company-country">Country</label>
          <select id="company-country" v-model="companyForm.country" class="input">
            <option value="">Select country</option>
            <option v-for="c in countries" :key="c" :value="c">{{ c }}</option>
          </select>
        </div>
        <div>
          <label class="label" for="company-phone">Phone</label>
          <input id="company-phone" v-model="companyForm.phone" type="tel" class="input" placeholder="+31 20 123 4567" />
        </div>
        <div>
          <label class="label" for="company-website">Website</label>
          <input id="company-website" v-model="companyForm.website" type="url" class="input" placeholder="https://www.example.com" />
        </div>
        <div>
          <label class="label" for="contact-email">Contact Email</label>
          <input id="contact-email" v-model="companyForm.contactEmail" type="email" class="input" placeholder="sales@example.com" />
        </div>
      </div>
      <div class="mt-6 flex justify-end">
        <button type="submit" class="btn-primary" :disabled="loading">
          {{ loading ? 'Saving...' : 'Save Company Details' }}
        </button>
      </div>
    </form>

    <!-- Bank Settings Tab -->
    <form v-if="activeTab === 'bank'" class="card" @submit.prevent="saveBank">
      <h2 class="mb-4 text-lg font-semibold text-gray-900">Bank Settings</h2>
      <p class="mb-4 text-sm text-gray-500">
        Your bank details are used for settlement payments. All information is stored securely and encrypted.
      </p>
      <div class="grid gap-4 sm:grid-cols-2">
        <div class="sm:col-span-2">
          <label class="label" for="account-holder">Account Holder</label>
          <input id="account-holder" v-model="bankForm.accountHolder" type="text" class="input" placeholder="Acme Industries B.V." />
        </div>
        <div>
          <label class="label" for="iban">IBAN</label>
          <input id="iban" v-model="bankForm.iban" type="text" class="input" placeholder="NL91 ABNA 0417 1643 00" />
        </div>
        <div>
          <label class="label" for="bic">BIC/SWIFT</label>
          <input id="bic" v-model="bankForm.bic" type="text" class="input" placeholder="ABNANL2A" />
        </div>
        <div>
          <label class="label" for="bank-name">Bank Name</label>
          <input id="bank-name" v-model="bankForm.bankName" type="text" class="input" placeholder="ABN AMRO" />
        </div>
        <div>
          <label class="label" for="currency">Preferred Currency</label>
          <select id="currency" v-model="bankForm.currency" class="input">
            <option value="EUR">EUR - Euro</option>
            <option value="GBP">GBP - British Pound</option>
            <option value="SEK">SEK - Swedish Krona</option>
            <option value="DKK">DKK - Danish Krone</option>
            <option value="PLN">PLN - Polish Zloty</option>
          </select>
        </div>
      </div>
      <div class="mt-4 rounded-lg bg-amber-50 p-3">
        <p class="text-xs text-amber-700">
          Changes to bank details require re-verification and may take up to 2 business days to take effect.
        </p>
      </div>
      <div class="mt-6 flex justify-end">
        <button type="submit" class="btn-primary" :disabled="loading">
          {{ loading ? 'Saving...' : 'Save Bank Settings' }}
        </button>
      </div>
    </form>

    <!-- Notifications Tab -->
    <form v-if="activeTab === 'notifications'" class="card" @submit.prevent="saveNotifications">
      <h2 class="mb-4 text-lg font-semibold text-gray-900">Notification Preferences</h2>

      <div class="space-y-6">
        <!-- Email Notifications -->
        <div>
          <h3 class="mb-3 text-sm font-semibold uppercase text-gray-500">Email Notifications</h3>
          <div class="space-y-3">
            <label class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
              <div>
                <p class="text-sm font-medium text-gray-900">New bid received</p>
                <p class="text-xs text-gray-500">Get notified when someone bids on your lots</p>
              </div>
              <input v-model="notificationPrefs.emailOnNewBid" type="checkbox" class="h-4 w-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500" />
            </label>
            <label class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
              <div>
                <p class="text-sm font-medium text-gray-900">Lot approved</p>
                <p class="text-xs text-gray-500">When your lot is approved and goes live</p>
              </div>
              <input v-model="notificationPrefs.emailOnLotApproved" type="checkbox" class="h-4 w-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500" />
            </label>
            <label class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
              <div>
                <p class="text-sm font-medium text-gray-900">Lot sold</p>
                <p class="text-xs text-gray-500">When your lot is sold at auction</p>
              </div>
              <input v-model="notificationPrefs.emailOnLotSold" type="checkbox" class="h-4 w-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500" />
            </label>
            <label class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
              <div>
                <p class="text-sm font-medium text-gray-900">Settlement paid</p>
                <p class="text-xs text-gray-500">When a payment is transferred to your account</p>
              </div>
              <input v-model="notificationPrefs.emailOnSettlementPaid" type="checkbox" class="h-4 w-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500" />
            </label>
            <label class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
              <div>
                <p class="text-sm font-medium text-gray-900">Daily digest</p>
                <p class="text-xs text-gray-500">A summary of daily activity on your lots</p>
              </div>
              <input v-model="notificationPrefs.emailDailyDigest" type="checkbox" class="h-4 w-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500" />
            </label>
            <label class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
              <div>
                <p class="text-sm font-medium text-gray-900">Weekly report</p>
                <p class="text-xs text-gray-500">A weekly summary of performance and analytics</p>
              </div>
              <input v-model="notificationPrefs.emailWeeklyReport" type="checkbox" class="h-4 w-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500" />
            </label>
          </div>
        </div>

        <!-- Push Notifications -->
        <div>
          <h3 class="mb-3 text-sm font-semibold uppercase text-gray-500">Push Notifications</h3>
          <div class="space-y-3">
            <label class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
              <div>
                <p class="text-sm font-medium text-gray-900">New bid received</p>
                <p class="text-xs text-gray-500">Real-time push notification for new bids</p>
              </div>
              <input v-model="notificationPrefs.pushOnNewBid" type="checkbox" class="h-4 w-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500" />
            </label>
            <label class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
              <div>
                <p class="text-sm font-medium text-gray-900">Lot sold</p>
                <p class="text-xs text-gray-500">Instant notification when a lot sells</p>
              </div>
              <input v-model="notificationPrefs.pushOnLotSold" type="checkbox" class="h-4 w-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500" />
            </label>
          </div>
        </div>
      </div>

      <div class="mt-6 flex justify-end">
        <button type="submit" class="btn-primary" :disabled="loading">
          {{ loading ? 'Saving...' : 'Save Preferences' }}
        </button>
      </div>
    </form>
  </div>
</template>
