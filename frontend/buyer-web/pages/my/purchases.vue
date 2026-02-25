<template>
  <div class="max-w-7xl mx-auto px-4 py-8">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('purchases.title') }}</h1>
        <p class="text-gray-500 text-sm mt-1">{{ $t('purchases.subtitle') }}</p>
      </div>
      <button
        v-if="selectedLots.length > 0"
        class="px-6 py-2.5 bg-primary text-white font-semibold rounded-lg hover:bg-primary-800 transition-colors flex items-center gap-2"
        @click="handleBatchCheckout"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 100 4 2 2 0 000-4z" />
        </svg>
        {{ $t('purchases.checkout') }} ({{ selectedLots.length }})
      </button>
    </div>

    <!-- Status Filters -->
    <div class="flex items-center gap-2 mb-6 overflow-x-auto pb-2">
      <button
        v-for="status in statusFilters"
        :key="status.value"
        class="px-4 py-2 rounded-full text-sm font-medium border whitespace-nowrap transition-colors"
        :class="activeStatus === status.value
          ? 'bg-primary text-white border-primary'
          : 'bg-white text-gray-600 border-gray-200 hover:border-primary hover:text-primary'"
        @click="activeStatus = status.value"
      >
        {{ status.label }}
        <span
          v-if="status.count > 0"
          class="ml-1 px-1.5 py-0.5 rounded-full text-xs"
          :class="activeStatus === status.value ? 'bg-white/20' : 'bg-gray-100'"
        >
          {{ status.count }}
        </span>
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="space-y-4">
      <div v-for="i in 3" :key="i" class="bg-white border rounded-xl p-6 animate-pulse">
        <div class="flex gap-4">
          <div class="w-24 h-24 bg-gray-200 rounded-lg" />
          <div class="flex-1">
            <div class="h-4 bg-gray-200 rounded w-2/3 mb-2" />
            <div class="h-3 bg-gray-200 rounded w-1/3 mb-3" />
            <div class="h-6 bg-gray-200 rounded w-1/4" />
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="filteredPurchases.length === 0" class="text-center py-16">
      <svg class="w-16 h-16 text-gray-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z" />
      </svg>
      <h3 class="text-lg font-medium text-gray-900 mb-2">{{ $t('purchases.empty') }}</h3>
      <p class="text-gray-500 mb-6">{{ $t('purchases.emptyHint') }}</p>
      <NuxtLink to="/search" class="px-6 py-2.5 bg-primary text-white font-medium rounded-lg hover:bg-primary-800 transition-colors">
        {{ $t('purchases.browseAuctions') }}
      </NuxtLink>
    </div>

    <!-- Purchases Table -->
    <div v-else class="space-y-4">
      <!-- Select All -->
      <div class="flex items-center gap-3 px-4 py-2 bg-gray-50 rounded-lg">
        <input
          type="checkbox"
          :checked="allSelected"
          :indeterminate="someSelected && !allSelected"
          class="w-4 h-4 rounded border-gray-300 text-primary focus:ring-primary"
          @change="toggleSelectAll"
        >
        <span class="text-sm text-gray-600">
          {{ allSelected ? $t('purchases.deselectAll') : $t('purchases.selectAll') }}
          ({{ filteredPurchases.filter(p => p.status === 'pending_payment').length }} {{ $t('purchases.payable') }})
        </span>
      </div>

      <!-- Purchase Rows -->
      <div
        v-for="purchase in filteredPurchases"
        :key="purchase.id"
        class="bg-white border rounded-xl p-4 hover:shadow-sm transition-shadow"
      >
        <div class="flex items-start gap-4">
          <!-- Checkbox (only for pending_payment) -->
          <div class="pt-1">
            <input
              v-if="purchase.status === 'pending_payment'"
              type="checkbox"
              :checked="isLotSelected(purchase.id)"
              class="w-4 h-4 rounded border-gray-300 text-primary focus:ring-primary"
              @change="toggleLotSelection(purchase)"
            >
            <div v-else class="w-4 h-4" />
          </div>

          <!-- Image -->
          <NuxtLink :to="`/lots/${purchase.auctionId}`" class="shrink-0">
            <div class="w-24 h-24 rounded-lg bg-gray-100 overflow-hidden">
              <img
                v-if="purchase.imageUrl"
                :src="purchase.imageUrl"
                :alt="purchase.title"
                class="w-full h-full object-cover"
                loading="lazy"
              >
            </div>
          </NuxtLink>

          <!-- Info -->
          <div class="flex-1 min-w-0">
            <NuxtLink :to="`/lots/${purchase.auctionId}`" class="hover:text-primary transition-colors">
              <h3 class="font-semibold text-gray-900 line-clamp-1">{{ purchase.title }}</h3>
            </NuxtLink>
            <div class="flex items-center gap-3 mt-1 text-sm text-gray-500">
              <span>{{ purchase.location }}</span>
              <span class="w-1 h-1 rounded-full bg-gray-300" />
              <span>{{ purchase.country }}</span>
            </div>

            <!-- Price Breakdown -->
            <div class="mt-2 flex items-center gap-4 text-sm">
              <div>
                <span class="text-gray-500">{{ $t('purchases.winningBid') }}:</span>
                <span class="font-bold text-gray-900 ml-1">{{ formatCurrency(purchase.winningBid) }}</span>
              </div>
              <div>
                <span class="text-gray-500">{{ $t('purchases.premium') }}:</span>
                <span class="font-medium text-gray-700 ml-1">{{ formatCurrency(purchase.buyersPremium) }}</span>
              </div>
              <div>
                <span class="text-gray-500">{{ $t('purchases.total') }}:</span>
                <span class="font-bold text-primary ml-1">{{ formatCurrency(purchase.totalAmount) }}</span>
              </div>
            </div>
          </div>

          <!-- Status Badge -->
          <div class="shrink-0">
            <span
              class="px-3 py-1 rounded-full text-xs font-semibold"
              :class="statusBadgeClass(purchase.status)"
            >
              {{ statusLabel(purchase.status) }}
            </span>
          </div>

          <!-- Actions -->
          <div class="shrink-0">
            <NuxtLink
              v-if="purchase.status === 'pending_payment'"
              :to="`/checkout/${purchase.id}`"
              class="px-4 py-2 bg-primary text-white text-sm font-medium rounded-lg hover:bg-primary-800 transition-colors"
            >
              {{ $t('purchases.pay') }}
            </NuxtLink>
            <span
              v-else-if="purchase.status === 'paid'"
              class="text-sm text-secondary font-medium"
            >
              {{ $t('purchases.awaitingCollection') }}
            </span>
          </div>
        </div>
      </div>

      <!-- Batch Checkout Summary -->
      <div
        v-if="selectedLots.length > 0"
        class="sticky bottom-4 bg-white border-2 border-primary rounded-xl p-4 shadow-lg flex items-center justify-between"
      >
        <div>
          <p class="text-sm text-gray-500">
            {{ selectedLots.length }} {{ $t('purchases.lotsSelected') }}
          </p>
          <p class="text-xl font-bold text-primary">
            {{ formatCurrency(selectedTotal) }}
          </p>
        </div>
        <button
          class="px-8 py-3 bg-primary text-white font-bold rounded-lg hover:bg-primary-800 transition-colors"
          @click="handleBatchCheckout"
        >
          {{ $t('purchases.proceedToCheckout') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useCartStore } from '~/stores/cart'
import type { CartLot } from '~/types/cart'
import { formatCurrency } from '~/utils/format'
import { unwrapApiResponse } from '~/utils/api-response'

definePageMeta({ middleware: 'auth' })

const { t } = useI18n()
const cartStore = useCartStore()

const loading = ref(false)
const purchases = ref<CartLot[]>([])
const activeStatus = ref('all')

onMounted(() => {
  fetchPurchases()
})

const statusFilters = computed(() => [
  { value: 'all', label: t('purchases.all'), count: purchases.value.length },
  { value: 'pending_payment', label: t('purchases.pendingPayment'), count: purchases.value.filter((p) => p.status === 'pending_payment').length },
  { value: 'paid', label: t('purchases.paid'), count: purchases.value.filter((p) => p.status === 'paid').length },
  { value: 'collected', label: t('purchases.collected'), count: purchases.value.filter((p) => p.status === 'collected').length },
])

const filteredPurchases = computed(() => {
  if (activeStatus.value === 'all') return purchases.value
  return purchases.value.filter((p) => p.status === activeStatus.value)
})

const selectedLots = computed(() => cartStore.selectedLots)

const allSelected = computed(() => {
  const payable = filteredPurchases.value.filter((p) => p.status === 'pending_payment')
  return payable.length > 0 && payable.every((p) => cartStore.isLotSelected(p.id))
})

const someSelected = computed(() => {
  return filteredPurchases.value.some((p) => cartStore.isLotSelected(p.id))
})

const selectedTotal = computed(() => cartStore.totals.total)

function isLotSelected(lotId: string): boolean {
  return cartStore.isLotSelected(lotId)
}

function toggleLotSelection(purchase: CartLot) {
  cartStore.toggleLot(purchase)
}

function toggleSelectAll() {
  const payable = filteredPurchases.value.filter((p) => p.status === 'pending_payment')
  if (allSelected.value) {
    cartStore.clearAll()
  } else {
    cartStore.selectAll(payable)
  }
}

function handleBatchCheckout() {
  if (selectedLots.value.length === 0) return
  navigateTo(`/checkout/batch`)
}

function statusBadgeClass(status: string): string {
  switch (status) {
    case 'pending_payment':
      return 'bg-accent-50 text-accent'
    case 'paid':
      return 'bg-secondary-50 text-secondary'
    case 'collected':
      return 'bg-gray-100 text-gray-600'
    default:
      return 'bg-gray-100 text-gray-600'
  }
}

function statusLabel(status: string): string {
  switch (status) {
    case 'pending_payment':
      return t('purchases.pendingPayment')
    case 'paid':
      return t('purchases.paid')
    case 'collected':
      return t('purchases.collected')
    default:
      return status
  }
}

async function fetchPurchases() {
  loading.value = true
  try {
    const { $api } = useNuxtApp()
    const api = $api as typeof $fetch
    const raw = await api<Record<string, unknown>>('/users/me/purchases')
    const data = unwrapApiResponse(raw)
    purchases.value = (Array.isArray(data.items) ? data.items : []) as CartLot[]
  } catch {
    purchases.value = []
  } finally {
    loading.value = false
  }
}

useHead({
  title: t('purchases.pageTitle'),
})
</script>
