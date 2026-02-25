<template>
  <div class="bg-white rounded-xl border shadow-sm overflow-hidden">
    <!-- Timer Section -->
    <div class="p-4 border-b bg-gray-50">
      <div class="flex items-center justify-between mb-2">
        <span class="text-sm text-gray-500">{{ $t('auction.timeRemaining') }}</span>
        <span
          v-if="lot.status === 'active' || lot.status === 'extended'"
          class="px-2 py-0.5 bg-secondary-100 text-secondary text-xs font-semibold rounded-full"
        >
          {{ $t('auction.live') }}
        </span>
        <span
          v-else-if="lot.status === 'closed'"
          class="px-2 py-0.5 bg-gray-100 text-gray-600 text-xs font-semibold rounded-full"
        >
          {{ $t('auction.closed') }}
        </span>
      </div>
      <AuctionTimer :end-time="lot.endTime" @expired="onAuctionExpired" />
    </div>

    <!-- Current Bid -->
    <div class="p-4 border-b">
      <div class="flex items-center justify-between mb-1">
        <span class="text-sm text-gray-500">{{ $t('auction.currentBid') }}</span>
        <span class="text-sm text-gray-500">
          {{ lot.bidCount }} {{ $t('auction.bids') }}
        </span>
      </div>
      <p class="text-3xl font-bold text-primary">{{ formatCurrency(currentBid) }}</p>

      <!-- Reserve Indicator -->
      <div v-if="lot.reservePrice !== undefined" class="mt-2">
        <div
          v-if="lot.reserveMet"
          class="flex items-center gap-1.5 text-sm text-secondary"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
          </svg>
          {{ $t('auction.reserveMet') }}
        </div>
        <div v-else class="flex items-center gap-1.5 text-sm text-accent">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
          </svg>
          {{ $t('auction.reserveNotMet') }}
        </div>
      </div>
    </div>

    <!-- Login Prompt (for unauthenticated users) -->
    <div v-if="!isAuthenticated" class="p-4">
      <div class="text-center py-4">
        <svg class="w-10 h-10 text-gray-300 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
        </svg>
        <p class="text-sm text-gray-600 mb-3">{{ $t('auction.loginToBid') }}</p>
        <button
          class="w-full py-2.5 bg-primary text-white font-semibold rounded-lg hover:bg-primary-800 transition-colors"
          @click="handleLogin"
        >
          {{ $t('nav.login') }}
        </button>
        <NuxtLink
          to="/auth/register"
          class="block mt-2 text-sm text-primary hover:underline"
        >
          {{ $t('auction.noAccount') }}
        </NuxtLink>
      </div>
    </div>

    <!-- Bid Section (for authenticated users, active auction) -->
    <div v-else-if="lot.status === 'active' || lot.status === 'extended'" class="p-4 space-y-4">
      <!-- Deposit Warning -->
      <div
        v-if="lot.depositRequired && !depositPaid"
        class="flex items-start gap-2 p-3 bg-accent-50 border border-accent-200 rounded-lg"
      >
        <svg class="w-5 h-5 text-accent shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
        </svg>
        <div>
          <p class="text-sm font-medium text-accent-800">{{ $t('auction.depositRequired') }}</p>
          <p class="text-xs text-accent-700 mt-0.5">
            {{ $t('auction.depositAmount', { amount: formatCurrency(lot.depositAmount || 0) }) }}
          </p>
        </div>
      </div>

      <!-- Bid Input -->
      <div>
        <label class="text-sm font-medium text-gray-700 mb-1.5 block">{{ $t('auction.yourBid') }}</label>
        <div class="relative">
          <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 font-medium">EUR</span>
          <input
            v-model.number="bidAmount"
            type="number"
            :min="minBidAmount"
            :step="lot.minIncrement"
            class="w-full pl-14 pr-4 py-3 border rounded-lg text-lg font-bold text-gray-900 focus:ring-2 focus:ring-primary focus:border-primary"
            :class="{ 'border-warning': bidError }"
            :placeholder="formatCurrency(minBidAmount)"
            @keydown.enter="handlePlaceBid"
          >
        </div>
        <p v-if="bidError" class="text-xs text-warning mt-1">{{ bidError }}</p>
        <p class="text-xs text-gray-400 mt-1">
          {{ $t('auction.minimumBid') }}: {{ formatCurrency(minBidAmount) }}
        </p>
      </div>

      <!-- Increment Buttons -->
      <div class="flex gap-2">
        <button
          v-for="increment in increments"
          :key="increment"
          class="flex-1 py-2 px-3 text-sm font-medium border rounded-lg hover:bg-primary-50 hover:border-primary hover:text-primary transition-colors"
          @click="addIncrement(increment)"
        >
          +{{ formatCurrency(increment) }}
        </button>
      </div>

      <!-- Place Bid Button -->
      <button
        class="w-full py-3 bg-primary text-white font-bold text-lg rounded-lg hover:bg-primary-800 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        :disabled="bidLoading || !canBid"
        @click="handlePlaceBid"
      >
        <span v-if="bidLoading" class="flex items-center justify-center gap-2">
          <svg class="animate-spin w-5 h-5" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
          </svg>
          {{ $t('auction.placingBid') }}
        </span>
        <span v-else>
          {{ $t('auction.placeBid') }} {{ bidAmount ? formatCurrency(bidAmount) : '' }}
        </span>
      </button>

      <!-- Success message -->
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
          class="flex items-center gap-2 p-3 bg-secondary-50 border border-secondary-200 rounded-lg text-secondary-800 text-sm"
        >
          <svg class="w-5 h-5 text-secondary shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
          </svg>
          {{ $t('auction.bidPlaced') }}
        </div>
      </Transition>

      <!-- Auto-Bid Section -->
      <div class="border-t pt-4">
        <div class="flex items-center justify-between mb-2">
          <label class="text-sm font-medium text-gray-700 flex items-center gap-2">
            <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            {{ $t('auction.autoBid') }}
          </label>
          <button
            class="relative w-11 h-6 rounded-full transition-colors"
            :class="autoBidEnabled ? 'bg-primary' : 'bg-gray-300'"
            @click="toggleAutoBid"
          >
            <span
              class="absolute top-0.5 w-5 h-5 bg-white rounded-full shadow transition-transform"
              :class="autoBidEnabled ? 'translate-x-[22px]' : 'translate-x-0.5'"
            />
          </button>
        </div>

        <Transition
          enter-active-class="transition ease-out duration-200"
          enter-from-class="opacity-0 max-h-0"
          enter-to-class="opacity-100 max-h-40"
          leave-active-class="transition ease-in duration-150"
          leave-from-class="opacity-100 max-h-40"
          leave-to-class="opacity-0 max-h-0"
        >
          <div v-if="autoBidEnabled" class="overflow-hidden">
            <div class="mt-2">
              <label class="text-xs text-gray-500 mb-1 block">{{ $t('auction.maxAutoBid') }}</label>
              <div class="relative">
                <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm">EUR</span>
                <input
                  v-model.number="autoBidMax"
                  type="number"
                  :min="minBidAmount"
                  class="w-full pl-14 pr-4 py-2 border rounded-lg text-sm font-medium focus:ring-2 focus:ring-primary focus:border-primary"
                  :placeholder="$t('auction.enterMaxBid')"
                >
              </div>
              <p class="text-xs text-gray-400 mt-1">{{ $t('auction.autoBidHint') }}</p>
              <button
                class="w-full mt-2 py-2 bg-primary-50 text-primary font-medium text-sm rounded-lg hover:bg-primary-100 transition-colors disabled:opacity-50"
                :disabled="!autoBidMax || autoBidMax < minBidAmount || bidLoading"
                @click="handleSetAutoBid"
              >
                {{ hasAutoBid ? $t('auction.updateAutoBid') : $t('auction.setAutoBid') }}
              </button>
              <button
                v-if="hasAutoBid"
                class="w-full mt-1 py-2 text-warning text-sm font-medium hover:bg-warning-50 rounded-lg transition-colors"
                @click="handleCancelAutoBid"
              >
                {{ $t('auction.cancelAutoBid') }}
              </button>
            </div>
          </div>
        </Transition>
      </div>
    </div>

    <!-- Closed Auction -->
    <div v-else-if="lot.status === 'closed'" class="p-4">
      <div class="text-center py-4">
        <p class="text-gray-500 text-sm">{{ $t('auction.auctionEnded') }}</p>
        <p class="text-2xl font-bold text-gray-900 mt-2">
          {{ $t('auction.finalPrice') }}: {{ formatCurrency(currentBid) }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Auction } from '~/stores/auction'

interface Props {
  lot: Auction
}

const props = defineProps<Props>()

const { isAuthenticated, login } = useAuth()
const { placeBid, setAutoBid, cancelAutoBid, loading: bidLoading, error: _bidApiError, currentBid, minBidAmount, hasAutoBid, clearError } = useBid()

const bidAmount = ref<number | null>(null)
const bidError = ref<string | null>(null)
const autoBidEnabled = ref(false)
const autoBidMax = ref<number | null>(null)
const showSuccess = ref(false)
const depositPaid = ref(true) // Assumed from API in real app

const increments = [50, 100, 500]

const canBid = computed(() => {
  if (!bidAmount.value) return false
  if (bidAmount.value < minBidAmount.value) return false
  if (props.lot.depositRequired && !depositPaid.value) return false
  return true
})

function addIncrement(amount: number) {
  const base = bidAmount.value || minBidAmount.value
  bidAmount.value = base + amount
}

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-EU', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(amount)
}

async function handlePlaceBid() {
  bidError.value = null
  clearError()

  if (!bidAmount.value || bidAmount.value < minBidAmount.value) {
    bidError.value = `Minimum bid is ${formatCurrency(minBidAmount.value)}`
    return
  }

  try {
    await placeBid({
      auctionId: props.lot.id,
      amount: bidAmount.value,
    })
    showSuccess.value = true
    bidAmount.value = null
    setTimeout(() => {
      showSuccess.value = false
    }, 3000)
  } catch (e: unknown) {
    bidError.value = e instanceof Error ? e.message : String(e)
  }
}

async function handleSetAutoBid() {
  if (!autoBidMax.value || autoBidMax.value < minBidAmount.value) return

  try {
    await setAutoBid({
      auctionId: props.lot.id,
      maxAmount: autoBidMax.value,
    })
  } catch (e: unknown) {
    bidError.value = e instanceof Error ? e.message : String(e)
  }
}

async function handleCancelAutoBid() {
  try {
    await cancelAutoBid(props.lot.id)
    autoBidEnabled.value = false
    autoBidMax.value = null
  } catch (e: unknown) {
    bidError.value = e instanceof Error ? e.message : String(e)
  }
}

async function toggleAutoBid() {
  if (autoBidEnabled.value && hasAutoBid.value) {
    await handleCancelAutoBid()
  } else {
    autoBidEnabled.value = !autoBidEnabled.value
  }
}

function handleLogin() {
  login()
}

function onAuctionExpired() {
  // Auction state will be updated via WebSocket
}

// Initialize bid amount to minimum
watch(minBidAmount, (val) => {
  if (!bidAmount.value && val > 0) {
    bidAmount.value = val
  }
}, { immediate: true })
</script>
