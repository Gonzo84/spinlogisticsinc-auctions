<template>
  <div class="max-w-3xl mx-auto px-4 py-8">
    <!-- Header -->
    <div class="mb-8">
      <NuxtLink to="/my/purchases" class="text-sm text-gray-500 hover:text-primary flex items-center gap-1 mb-4">
        <i class="pi pi-chevron-left text-xs" />
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
            <i v-if="index < currentStep" class="pi pi-check text-sm" />
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
    <div v-if="loading" class="flex justify-center py-16">
      <ProgressSpinner style="width: 50px; height: 50px" />
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
                <RadioButton v-model="vatForm.type" value="business" />
                <div>
                  <p class="text-sm font-medium">{{ $t('checkout.businessVat') }}</p>
                  <p class="text-xs text-gray-500">{{ $t('checkout.businessVatHint') }}</p>
                </div>
              </label>
              <label
                class="flex-1 flex items-center gap-3 p-4 border rounded-lg cursor-pointer transition-colors"
                :class="vatForm.type === 'private' ? 'border-primary bg-primary-50' : 'hover:bg-gray-50'"
              >
                <RadioButton v-model="vatForm.type" value="private" />
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
              <InputText
                v-model="vatForm.vatNumber"
                class="w-full"
                :placeholder="$t('checkout.vatNumberPlaceholder')"
              />
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
            <InputText
              v-model="vatForm.companyName"
              class="w-full mb-2"
              :placeholder="$t('checkout.companyName')"
            />
            <InputText
              v-model="vatForm.address"
              class="w-full mb-2"
              :placeholder="$t('checkout.address')"
            />
            <div class="grid grid-cols-2 gap-2">
              <InputText
                v-model="vatForm.postalCode"
                class="w-full"
                :placeholder="$t('checkout.postalCode')"
              />
              <InputText
                v-model="vatForm.city"
                class="w-full"
                :placeholder="$t('checkout.city')"
              />
            </div>
            <Select
              v-model="vatForm.country"
              :options="countryOptions"
              optionLabel="label"
              optionValue="value"
              :placeholder="$t('checkout.selectCountry')"
              class="w-full mt-2"
            />
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
            <Checkbox
              v-model="termsAccepted.general"
              :binary="true"
            />
            <span class="text-sm text-gray-700">
              {{ $t('checkout.acceptGeneralTerms') }}
            </span>
          </label>
          <label class="flex items-start gap-3 cursor-pointer">
            <Checkbox
              v-model="termsAccepted.auction"
              :binary="true"
            />
            <span class="text-sm text-gray-700">
              {{ $t('checkout.acceptAuctionTerms') }}
            </span>
          </label>
          <label class="flex items-start gap-3 cursor-pointer">
            <Checkbox
              v-model="termsAccepted.privacy"
              :binary="true"
            />
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
            <RadioButton v-model="paymentMethod" value="bank_transfer" />
            <div class="flex items-center gap-3">
              <i class="pi pi-building-columns text-xl text-gray-400" />
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
            <RadioButton v-model="paymentMethod" value="ideal" />
            <div class="flex items-center gap-3">
              <i class="pi pi-wallet text-xl text-gray-400" />
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
            <RadioButton v-model="paymentMethod" value="card" />
            <div class="flex items-center gap-3">
              <i class="pi pi-credit-card text-xl text-gray-400" />
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
            <i class="pi pi-spinner pi-spin" />
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
