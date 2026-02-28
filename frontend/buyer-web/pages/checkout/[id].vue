<template>
  <div class="max-w-3xl mx-auto px-4 py-8">
    <!-- Header -->
    <div class="mb-8">
      <NuxtLink to="/my/purchases" class="text-sm text-gray-500 hover:text-primary flex items-center gap-1 mb-4">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
        {{ $t('checkout.backToPurchases') }}
      </NuxtLink>
      <h1 class="text-2xl font-bold text-gray-900">{{ $t('checkout.title') }}</h1>
    </div>

    <!-- Step Indicator -->
    <div class="flex items-center justify-between mb-10">
      <div
        v-for="(step, index) in steps"
        :key="index"
        class="flex items-center"
        :class="{ 'flex-1': index < steps.length - 1 }"
      >
        <div class="flex items-center gap-2">
          <div
            class="w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold transition-colors"
            :class="{
              'bg-primary text-white': index <= currentStep,
              'bg-gray-200 text-gray-500': index > currentStep,
            }"
          >
            <svg v-if="index < currentStep" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
            <span v-else>{{ index + 1 }}</span>
          </div>
          <span
            class="text-sm font-medium hidden sm:block"
            :class="index <= currentStep ? 'text-gray-900' : 'text-gray-400'"
          >
            {{ step.label }}
          </span>
        </div>
        <div
          v-if="index < steps.length - 1"
          class="flex-1 h-0.5 mx-4"
          :class="index < currentStep ? 'bg-primary' : 'bg-gray-200'"
        />
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="bg-white border rounded-xl p-8 animate-pulse">
      <div class="h-6 bg-gray-200 rounded w-1/3 mb-4" />
      <div class="h-4 bg-gray-200 rounded w-2/3 mb-2" />
      <div class="h-4 bg-gray-200 rounded w-1/2" />
    </div>

    <!-- Step 1: Verify Purchase Details -->
    <div v-else-if="currentStep === 0" class="space-y-6">
      <div class="bg-white border rounded-xl p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">{{ $t('checkout.verifyDetails') }}</h2>

        <div v-if="order" class="space-y-4">
          <!-- Lot Summary -->
          <div
            v-for="item in order.items"
            :key="item.id"
            class="flex items-start gap-4 p-4 bg-gray-50 rounded-lg"
          >
            <div class="w-20 h-20 rounded-lg bg-gray-200 overflow-hidden shrink-0">
              <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.title" class="w-full h-full object-cover">
            </div>
            <div class="flex-1 min-w-0">
              <h3 class="font-semibold text-gray-900 text-sm">{{ item.title }}</h3>
              <p class="text-xs text-gray-500 mt-0.5">{{ item.location }}</p>
            </div>
            <div class="text-right shrink-0">
              <p class="text-sm text-gray-500">{{ $t('checkout.winningBid') }}</p>
              <p class="font-bold text-gray-900">{{ formatCurrency(item.amount) }}</p>
            </div>
          </div>

          <!-- Totals -->
          <div class="border-t pt-4 space-y-2">
            <div class="flex justify-between text-sm">
              <span class="text-gray-500">{{ $t('checkout.subtotal') }}</span>
              <span class="font-medium">{{ formatCurrency(order.subtotal) }}</span>
            </div>
            <div class="flex justify-between text-sm">
              <span class="text-gray-500">{{ $t('checkout.buyersPremium') }}</span>
              <span class="font-medium">{{ formatCurrency(order.buyersPremium) }}</span>
            </div>
            <div class="flex justify-between text-sm">
              <span class="text-gray-500">{{ $t('checkout.vatEstimate') }}</span>
              <span class="font-medium">{{ formatCurrency(order.vatEstimate) }}</span>
            </div>
            <div class="flex justify-between text-lg font-bold border-t pt-2">
              <span>{{ $t('checkout.total') }}</span>
              <span class="text-primary">{{ formatCurrency(order.total) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="flex justify-end">
        <button
          class="px-8 py-3 bg-primary text-white font-semibold rounded-lg hover:bg-primary-800 transition-colors"
          @click="nextStep"
        >
          {{ $t('checkout.continue') }}
        </button>
      </div>
    </div>

    <!-- Step 2: VAT Information -->
    <div v-else-if="currentStep === 1" class="space-y-6">
      <div class="bg-white border rounded-xl p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">{{ $t('checkout.vatInformation') }}</h2>

        <div class="space-y-4">
          <!-- VAT Type -->
          <div>
            <label class="text-sm font-medium text-gray-700 mb-2 block">{{ $t('checkout.vatType') }}</label>
            <div class="flex gap-3">
              <label
                class="flex-1 flex items-center gap-3 p-4 border rounded-lg cursor-pointer transition-colors"
                :class="vatForm.type === 'business' ? 'border-primary bg-primary-50' : 'hover:bg-gray-50'"
              >
                <input v-model="vatForm.type" type="radio" value="business" class="text-primary focus:ring-primary">
                <div>
                  <p class="text-sm font-medium">{{ $t('checkout.businessVat') }}</p>
                  <p class="text-xs text-gray-500">{{ $t('checkout.businessVatHint') }}</p>
                </div>
              </label>
              <label
                class="flex-1 flex items-center gap-3 p-4 border rounded-lg cursor-pointer transition-colors"
                :class="vatForm.type === 'private' ? 'border-primary bg-primary-50' : 'hover:bg-gray-50'"
              >
                <input v-model="vatForm.type" type="radio" value="private" class="text-primary focus:ring-primary">
                <div>
                  <p class="text-sm font-medium">{{ $t('checkout.privateVat') }}</p>
                  <p class="text-xs text-gray-500">{{ $t('checkout.privateVatHint') }}</p>
                </div>
              </label>
            </div>
          </div>

          <!-- VAT Number (Business only) -->
          <div v-if="vatForm.type === 'business'">
            <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('checkout.vatNumber') }}</label>
            <div class="relative">
              <input
                v-model="vatForm.vatNumber"
                type="text"
                class="w-full px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
                :placeholder="$t('checkout.vatNumberPlaceholder')"
              >
              <button
                v-if="vatForm.vatNumber"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-medium"
                :class="vatValidated ? 'text-secondary' : 'text-primary hover:underline'"
                @click="validateVat"
              >
                {{ vatValidated ? $t('checkout.vatValid') : $t('checkout.validateVat') }}
              </button>
            </div>
            <p v-if="vatError" class="text-xs text-warning mt-1">{{ vatError }}</p>
          </div>

          <!-- Billing Address -->
          <div>
            <label class="text-sm font-medium text-gray-700 mb-1 block">{{ $t('checkout.billingAddress') }}</label>
            <input
              v-model="vatForm.companyName"
              type="text"
              class="w-full px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary mb-2"
              :placeholder="$t('checkout.companyName')"
            >
            <input
              v-model="vatForm.address"
              type="text"
              class="w-full px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary mb-2"
              :placeholder="$t('checkout.address')"
            >
            <div class="grid grid-cols-2 gap-2">
              <input
                v-model="vatForm.postalCode"
                type="text"
                class="px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
                :placeholder="$t('checkout.postalCode')"
              >
              <input
                v-model="vatForm.city"
                type="text"
                class="px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
                :placeholder="$t('checkout.city')"
              >
            </div>
            <select
              v-model="vatForm.country"
              class="w-full mt-2 px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
            >
              <option value="">{{ $t('checkout.selectCountry') }}</option>
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

      <div class="flex justify-between">
        <button
          class="px-6 py-3 border font-medium rounded-lg hover:bg-gray-50 transition-colors"
          @click="prevStep"
        >
          {{ $t('common.back') }}
        </button>
        <button
          class="px-8 py-3 bg-primary text-white font-semibold rounded-lg hover:bg-primary-800 transition-colors"
          @click="nextStep"
        >
          {{ $t('checkout.continue') }}
        </button>
      </div>
    </div>

    <!-- Step 3: Terms & Conditions -->
    <div v-else-if="currentStep === 2" class="space-y-6">
      <div class="bg-white border rounded-xl p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">{{ $t('checkout.termsConditions') }}</h2>

        <div class="prose prose-sm max-w-none text-gray-600 max-h-64 overflow-y-auto mb-6 p-4 bg-gray-50 rounded-lg">
          <h3>{{ $t('checkout.generalTermsTitle') }}</h3>
          <p>{{ $t('checkout.generalTermsText') }}</p>
          <h3>{{ $t('checkout.buyerObligationsTitle') }}</h3>
          <p>{{ $t('checkout.buyerObligationsText') }}</p>
          <h3>{{ $t('checkout.paymentTermsTitle') }}</h3>
          <p>{{ $t('checkout.paymentTermsText') }}</p>
          <h3>{{ $t('checkout.collectionTermsTitle') }}</h3>
          <p>{{ $t('checkout.collectionTermsText') }}</p>
        </div>

        <div class="space-y-3">
          <label class="flex items-start gap-3 cursor-pointer">
            <input
              v-model="termsAccepted.general"
              type="checkbox"
              class="w-4 h-4 mt-0.5 rounded border-gray-300 text-primary focus:ring-primary"
            >
            <span class="text-sm text-gray-700">
              {{ $t('checkout.acceptGeneralTerms') }}
            </span>
          </label>
          <label class="flex items-start gap-3 cursor-pointer">
            <input
              v-model="termsAccepted.auction"
              type="checkbox"
              class="w-4 h-4 mt-0.5 rounded border-gray-300 text-primary focus:ring-primary"
            >
            <span class="text-sm text-gray-700">
              {{ $t('checkout.acceptAuctionTerms') }}
            </span>
          </label>
          <label class="flex items-start gap-3 cursor-pointer">
            <input
              v-model="termsAccepted.privacy"
              type="checkbox"
              class="w-4 h-4 mt-0.5 rounded border-gray-300 text-primary focus:ring-primary"
            >
            <span class="text-sm text-gray-700">
              {{ $t('checkout.acceptPrivacy') }}
            </span>
          </label>
        </div>
      </div>

      <div class="flex justify-between">
        <button
          class="px-6 py-3 border font-medium rounded-lg hover:bg-gray-50 transition-colors"
          @click="prevStep"
        >
          {{ $t('common.back') }}
        </button>
        <button
          class="px-8 py-3 bg-primary text-white font-semibold rounded-lg hover:bg-primary-800 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="!allTermsAccepted"
          @click="nextStep"
        >
          {{ $t('checkout.continue') }}
        </button>
      </div>
    </div>

    <!-- Step 4: Payment -->
    <div v-else-if="currentStep === 3" class="space-y-6">
      <div class="bg-white border rounded-xl p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">{{ $t('checkout.payment') }}</h2>

        <!-- Order Summary -->
        <div class="p-4 bg-gray-50 rounded-lg mb-6">
          <div class="flex justify-between text-sm mb-1">
            <span class="text-gray-500">{{ $t('checkout.total') }}</span>
            <span class="text-2xl font-bold text-primary">{{ formatCurrency(order?.total || 0) }}</span>
          </div>
        </div>

        <!-- Payment Method -->
        <div class="space-y-3">
          <label class="text-sm font-medium text-gray-700 block">{{ $t('checkout.paymentMethod') }}</label>

          <label
            class="flex items-center gap-4 p-4 border rounded-lg cursor-pointer transition-colors"
            :class="paymentMethod === 'bank_transfer' ? 'border-primary bg-primary-50' : 'hover:bg-gray-50'"
          >
            <input v-model="paymentMethod" type="radio" value="bank_transfer" class="text-primary focus:ring-primary">
            <div class="flex items-center gap-3">
              <svg class="w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
              </svg>
              <div>
                <p class="text-sm font-medium">{{ $t('checkout.bankTransfer') }}</p>
                <p class="text-xs text-gray-500">{{ $t('checkout.bankTransferHint') }}</p>
              </div>
            </div>
          </label>

          <label
            class="flex items-center gap-4 p-4 border rounded-lg cursor-pointer transition-colors"
            :class="paymentMethod === 'ideal' ? 'border-primary bg-primary-50' : 'hover:bg-gray-50'"
          >
            <input v-model="paymentMethod" type="radio" value="ideal" class="text-primary focus:ring-primary">
            <div class="flex items-center gap-3">
              <svg class="w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
              </svg>
              <div>
                <p class="text-sm font-medium">iDEAL</p>
                <p class="text-xs text-gray-500">{{ $t('checkout.idealHint') }}</p>
              </div>
            </div>
          </label>

          <label
            class="flex items-center gap-4 p-4 border rounded-lg cursor-pointer transition-colors"
            :class="paymentMethod === 'card' ? 'border-primary bg-primary-50' : 'hover:bg-gray-50'"
          >
            <input v-model="paymentMethod" type="radio" value="card" class="text-primary focus:ring-primary">
            <div class="flex items-center gap-3">
              <svg class="w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
              </svg>
              <div>
                <p class="text-sm font-medium">{{ $t('checkout.creditCard') }}</p>
                <p class="text-xs text-gray-500">Visa, Mastercard, AMEX</p>
              </div>
            </div>
          </label>
        </div>
      </div>

      <div class="flex justify-between">
        <button
          class="px-6 py-3 border font-medium rounded-lg hover:bg-gray-50 transition-colors"
          @click="prevStep"
        >
          {{ $t('common.back') }}
        </button>
        <button
          class="px-8 py-3 bg-secondary text-white font-bold rounded-lg hover:bg-secondary-700 transition-colors disabled:opacity-50"
          :disabled="!paymentMethod || processing"
          @click="handlePayment"
        >
          <span v-if="processing" class="flex items-center gap-2">
            <svg class="animate-spin w-5 h-5" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
            </svg>
            {{ $t('checkout.processing') }}
          </span>
          <span v-else>
            {{ $t('checkout.payNow') }} {{ formatCurrency(order?.total || 0) }}
          </span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Order } from '~/types/checkout'
import { formatCurrency } from '~/utils/format'

definePageMeta({ middleware: 'auth' })

const { t } = useI18n()
const route = useRoute()

const orderId = computed(() => route.params.id as string)
const loading = ref(false)
const processing = ref(false)
const currentStep = ref(0)

const order = ref<Order | null>(null)

const vatForm = reactive({
  type: 'business' as 'business' | 'private',
  vatNumber: '',
  companyName: '',
  address: '',
  postalCode: '',
  city: '',
  country: '',
})

const vatValidated = ref(false)
const vatError = ref<string | null>(null)

const termsAccepted = reactive({
  general: false,
  auction: false,
  privacy: false,
})

const allTermsAccepted = computed(() => {
  return termsAccepted.general && termsAccepted.auction && termsAccepted.privacy
})

const paymentMethod = ref<string>('')

const steps = computed(() => [
  { label: t('checkout.stepVerify') },
  { label: t('checkout.stepVat') },
  { label: t('checkout.stepTerms') },
  { label: t('checkout.stepPayment') },
])

onMounted(async () => {
  await fetchOrder()
})

async function fetchOrder() {
  loading.value = true
  try {
    const { $api } = useNuxtApp()
    const api = $api as typeof $fetch
    order.value = await api<Order>(`/checkout/${orderId.value}`)
  } catch {
    navigateTo('/my/purchases')
  } finally {
    loading.value = false
  }
}

async function validateVat() {
  vatError.value = null
  try {
    const { $api } = useNuxtApp()
    const api = $api as typeof $fetch
    const result = await api<{ valid: boolean }>('/vat/validate', {
      method: 'POST',
      body: { vatNumber: vatForm.vatNumber },
    })
    if (result.valid) {
      vatValidated.value = true
    } else {
      vatError.value = t('checkout.vatInvalid')
    }
  } catch {
    vatError.value = t('checkout.vatValidationFailed')
  }
}

async function handlePayment() {
  processing.value = true
  try {
    const { $api } = useNuxtApp()
    const api = $api as typeof $fetch
    const result = await api<{ redirectUrl?: string; status: string }>(`/checkout/${orderId.value}/pay`, {
      method: 'POST',
      body: {
        paymentMethod: paymentMethod.value,
        vatInfo: vatForm,
      },
    })

    if (result.redirectUrl) {
      window.location.href = result.redirectUrl
    } else {
      navigateTo('/my/purchases')
    }
  } catch {
    processing.value = false
  }
}

function nextStep() {
  if (currentStep.value < 3) {
    currentStep.value++
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

function prevStep() {
  if (currentStep.value > 0) {
    currentStep.value--
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

useHead({
  title: t('checkout.pageTitle'),
})
</script>
