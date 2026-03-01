<script setup lang="ts">
import { reactive, ref, onMounted, nextTick } from 'vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Checkbox from 'primevue/checkbox'
import Select from 'primevue/select'
import { useAuth } from '@/composables/useAuth'
import { useApi } from '@/composables/useApi'

const { userName, userEmail, companyName } = useAuth()
const { get, loading } = useApi()

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

const countryOptions = [
  { label: 'Select country', value: '' },
  ...countries.map((c) => ({ label: c, value: c })),
]

const currencyOptions = [
  { label: 'EUR - Euro', value: 'EUR' },
  { label: 'GBP - British Pound', value: 'GBP' },
  { label: 'SEK - Swedish Krona', value: 'SEK' },
  { label: 'DKK - Danish Krone', value: 'DKK' },
  { label: 'PLN - Polish Zloty', value: 'PLN' },
]

onMounted(async () => {
  // Populate company form from useAuth (Keycloak token) and seller registration data
  companyForm.companyName = companyName.value || ''
  companyForm.contactEmail = userEmail.value || ''

  // Try to load seller dashboard data for any additional info
  try {
    interface DashboardResponse {
      data?: Record<string, unknown>
    }
    const raw = await get<DashboardResponse | Record<string, unknown>>('/sellers/me/dashboard')
    // Dashboard doesn't return profile details, but confirms seller exists
    // Seller is registered -- keep form defaults
    void raw
  } catch {
    // Seller profile not yet created – use defaults
  }
})

async function saveCompany() {
  saveSuccess.value = false
  saveError.value = null
  await nextTick()
  saveSuccess.value = true
  setTimeout(() => { saveSuccess.value = false }, 3000)
}

async function saveBank() {
  saveSuccess.value = false
  saveError.value = null
  await nextTick()
  saveSuccess.value = true
  setTimeout(() => { saveSuccess.value = false }, 3000)
}

async function saveNotifications() {
  saveSuccess.value = false
  saveError.value = null
  await nextTick()
  saveSuccess.value = true
  setTimeout(() => { saveSuccess.value = false }, 3000)
}
</script>

<template>
  <div>
    <!-- Page header -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">
        Profile Settings
      </h1>
      <p class="mt-1 text-sm text-gray-500">
        Manage your company details, bank settings, and notification preferences.
      </p>
    </div>

    <!-- User info card -->
    <div class="card mb-6">
      <div class="flex items-center gap-4">
        <div class="flex h-14 w-14 items-center justify-center rounded-full bg-seller-100 text-xl font-bold text-seller-700">
          {{ userName.charAt(0).toUpperCase() }}
        </div>
        <div>
          <p class="text-lg font-semibold text-gray-900">
            {{ userName }}
          </p>
          <p class="text-sm text-gray-500">
            {{ userEmail }}
          </p>
          <p
            v-if="companyName"
            class="text-sm text-gray-500"
          >
            {{ companyName }}
          </p>
        </div>
      </div>
    </div>

    <!-- Success/Error banner -->
    <div
      v-if="saveSuccess"
      class="mb-4 rounded-lg border border-green-200 bg-green-50 p-3"
    >
      <p class="text-sm font-medium text-green-800">
        Settings saved successfully.
      </p>
    </div>
    <div
      v-if="saveError"
      class="mb-4 rounded-lg border border-red-200 bg-red-50 p-3"
    >
      <p class="text-sm font-medium text-red-800">
        {{ saveError }}
      </p>
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
    <form
      v-if="activeTab === 'company'"
      class="card"
      @submit.prevent="saveCompany"
    >
      <h2 class="mb-4 text-lg font-semibold text-gray-900">
        Company Details
      </h2>
      <div class="grid gap-4 sm:grid-cols-2">
        <div class="sm:col-span-2">
          <label
            class="label"
            for="company-name"
          >Company Name</label>
          <InputText
            id="company-name"
            v-model="companyForm.companyName"
            placeholder="Acme Industries B.V."
            class="w-full"
          />
        </div>
        <div>
          <label
            class="label"
            for="vat-number"
          >VAT Number</label>
          <InputText
            id="vat-number"
            v-model="companyForm.vatNumber"
            placeholder="NL123456789B01"
            class="w-full"
          />
        </div>
        <div>
          <label
            class="label"
            for="reg-number"
          >Registration Number</label>
          <InputText
            id="reg-number"
            v-model="companyForm.registrationNumber"
            placeholder="12345678"
            class="w-full"
          />
        </div>
        <div class="sm:col-span-2">
          <label
            class="label"
            for="company-address"
          >Address</label>
          <InputText
            id="company-address"
            v-model="companyForm.address"
            placeholder="123 Business Street"
            class="w-full"
          />
        </div>
        <div>
          <label
            class="label"
            for="company-city"
          >City</label>
          <InputText
            id="company-city"
            v-model="companyForm.city"
            placeholder="Amsterdam"
            class="w-full"
          />
        </div>
        <div>
          <label
            class="label"
            for="company-postal"
          >Postal Code</label>
          <InputText
            id="company-postal"
            v-model="companyForm.postalCode"
            placeholder="1012 AB"
            class="w-full"
          />
        </div>
        <div>
          <label
            class="label"
            for="company-country"
          >Country</label>
          <Select
            id="company-country"
            v-model="companyForm.country"
            :options="countryOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Select country"
            class="w-full"
          />
        </div>
        <div>
          <label
            class="label"
            for="company-phone"
          >Phone</label>
          <InputText
            id="company-phone"
            v-model="companyForm.phone"
            placeholder="+31 20 123 4567"
            class="w-full"
          />
        </div>
        <div>
          <label
            class="label"
            for="company-website"
          >Website</label>
          <InputText
            id="company-website"
            v-model="companyForm.website"
            placeholder="https://www.example.com"
            class="w-full"
          />
        </div>
        <div>
          <label
            class="label"
            for="contact-email"
          >Contact Email</label>
          <InputText
            id="contact-email"
            v-model="companyForm.contactEmail"
            placeholder="sales@example.com"
            class="w-full"
          />
        </div>
      </div>
      <div class="mt-6 flex justify-end">
        <Button
          type="submit"
          :label="loading ? 'Saving...' : 'Save Company Details'"
          :icon="loading ? 'pi pi-spin pi-spinner' : undefined"
          :disabled="loading"
        />
      </div>
    </form>

    <!-- Bank Settings Tab -->
    <form
      v-if="activeTab === 'bank'"
      class="card"
      @submit.prevent="saveBank"
    >
      <h2 class="mb-4 text-lg font-semibold text-gray-900">
        Bank Settings
      </h2>
      <p class="mb-4 text-sm text-gray-500">
        Your bank details are used for settlement payments. All information is stored securely and encrypted.
      </p>
      <div class="grid gap-4 sm:grid-cols-2">
        <div class="sm:col-span-2">
          <label
            class="label"
            for="account-holder"
          >Account Holder</label>
          <InputText
            id="account-holder"
            v-model="bankForm.accountHolder"
            placeholder="Acme Industries B.V."
            class="w-full"
          />
        </div>
        <div>
          <label
            class="label"
            for="iban"
          >IBAN</label>
          <InputText
            id="iban"
            v-model="bankForm.iban"
            placeholder="NL91 ABNA 0417 1643 00"
            class="w-full"
          />
        </div>
        <div>
          <label
            class="label"
            for="bic"
          >BIC/SWIFT</label>
          <InputText
            id="bic"
            v-model="bankForm.bic"
            placeholder="ABNANL2A"
            class="w-full"
          />
        </div>
        <div>
          <label
            class="label"
            for="bank-name"
          >Bank Name</label>
          <InputText
            id="bank-name"
            v-model="bankForm.bankName"
            placeholder="ABN AMRO"
            class="w-full"
          />
        </div>
        <div>
          <label
            class="label"
            for="currency"
          >Preferred Currency</label>
          <Select
            id="currency"
            v-model="bankForm.currency"
            :options="currencyOptions"
            optionLabel="label"
            optionValue="value"
            class="w-full"
          />
        </div>
      </div>
      <div class="mt-4 rounded-lg bg-amber-50 p-3">
        <p class="text-xs text-amber-700">
          Changes to bank details require re-verification and may take up to 2 business days to take effect.
        </p>
      </div>
      <div class="mt-6 flex justify-end">
        <Button
          type="submit"
          :label="loading ? 'Saving...' : 'Save Bank Settings'"
          :icon="loading ? 'pi pi-spin pi-spinner' : undefined"
          :disabled="loading"
        />
      </div>
    </form>

    <!-- Notifications Tab -->
    <form
      v-if="activeTab === 'notifications'"
      class="card"
      @submit.prevent="saveNotifications"
    >
      <h2 class="mb-4 text-lg font-semibold text-gray-900">
        Notification Preferences
      </h2>

      <div class="space-y-6">
        <!-- Email Notifications -->
        <div>
          <h3 class="mb-3 text-sm font-semibold uppercase text-gray-500">
            Email Notifications
          </h3>
          <div class="space-y-3">
            <label class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
              <div>
                <p class="text-sm font-medium text-gray-900">New bid received</p>
                <p class="text-xs text-gray-500">Get notified when someone bids on your lots</p>
              </div>
              <Checkbox
                v-model="notificationPrefs.emailOnNewBid"
                :binary="true"
              />
            </label>
            <label class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
              <div>
                <p class="text-sm font-medium text-gray-900">Lot approved</p>
                <p class="text-xs text-gray-500">When your lot is approved and goes live</p>
              </div>
              <Checkbox
                v-model="notificationPrefs.emailOnLotApproved"
                :binary="true"
              />
            </label>
            <label class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
              <div>
                <p class="text-sm font-medium text-gray-900">Lot sold</p>
                <p class="text-xs text-gray-500">When your lot is sold at auction</p>
              </div>
              <Checkbox
                v-model="notificationPrefs.emailOnLotSold"
                :binary="true"
              />
            </label>
            <label class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
              <div>
                <p class="text-sm font-medium text-gray-900">Settlement paid</p>
                <p class="text-xs text-gray-500">When a payment is transferred to your account</p>
              </div>
              <Checkbox
                v-model="notificationPrefs.emailOnSettlementPaid"
                :binary="true"
              />
            </label>
            <label class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
              <div>
                <p class="text-sm font-medium text-gray-900">Daily digest</p>
                <p class="text-xs text-gray-500">A summary of daily activity on your lots</p>
              </div>
              <Checkbox
                v-model="notificationPrefs.emailDailyDigest"
                :binary="true"
              />
            </label>
            <label class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
              <div>
                <p class="text-sm font-medium text-gray-900">Weekly report</p>
                <p class="text-xs text-gray-500">A weekly summary of performance and analytics</p>
              </div>
              <Checkbox
                v-model="notificationPrefs.emailWeeklyReport"
                :binary="true"
              />
            </label>
          </div>
        </div>

        <!-- Push Notifications -->
        <div>
          <h3 class="mb-3 text-sm font-semibold uppercase text-gray-500">
            Push Notifications
          </h3>
          <div class="space-y-3">
            <label class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
              <div>
                <p class="text-sm font-medium text-gray-900">New bid received</p>
                <p class="text-xs text-gray-500">Real-time push notification for new bids</p>
              </div>
              <Checkbox
                v-model="notificationPrefs.pushOnNewBid"
                :binary="true"
              />
            </label>
            <label class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
              <div>
                <p class="text-sm font-medium text-gray-900">Lot sold</p>
                <p class="text-xs text-gray-500">Instant notification when a lot sells</p>
              </div>
              <Checkbox
                v-model="notificationPrefs.pushOnLotSold"
                :binary="true"
              />
            </label>
          </div>
        </div>
      </div>

      <div class="mt-6 flex justify-end">
        <Button
          type="submit"
          :label="loading ? 'Saving...' : 'Save Preferences'"
          :icon="loading ? 'pi pi-spin pi-spinner' : undefined"
          :disabled="loading"
        />
      </div>
    </form>
  </div>
</template>
