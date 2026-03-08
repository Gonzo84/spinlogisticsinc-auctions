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
          {{ bidCount }} {{ bidCount === 1 ? $t('auction.bid') : $t('auction.bids') }}
        </span>
      </div>
      <p class="text-3xl font-bold text-primary">{{ formatCurrency(currentBid) }}</p>

      <!-- Reserve Indicator -->
      <div v-if="lot.reservePrice !== undefined" class="mt-2">
        <div
          v-if="lot.reserveMet"
          class="flex items-center gap-1.5 text-sm text-secondary"
        >
          <i class="pi pi-check text-sm" />
          {{ $t('auction.reserveMet') }}
        </div>
        <div v-else class="flex items-center gap-1.5 text-sm text-accent">
          <i class="pi pi-exclamation-triangle text-sm" />
          {{ $t('auction.reserveNotMet') }}
        </div>
      </div>
    </div>

    <!-- Login Prompt (for unauthenticated users) -->
    <div v-if="!isAuthenticated" class="p-4">
      <div class="text-center py-4">
        <i class="pi pi-lock text-4xl text-gray-300 mb-3" />
        <p class="text-sm text-gray-600 mb-3">{{ $t('auction.loginToBid') }}</p>
        <Button
          :label="$t('nav.login')"
          class="w-full"
          @click="handleLogin"
        />
        <NuxtLink
          to="/auth/register"
          class="block mt-2 text-sm text-primary hover:underline"
        >
          {{ $t('auction.noAccount') }}
        </NuxtLink>
      </div>
    </div>

    <!-- Non-buyer role warning -->
    <div v-else-if="!isBuyerRole && (lot.status === 'active' || lot.status === 'extended')" class="p-4">
      <div class="text-center py-4">
        <i class="pi pi-info-circle text-4xl text-gray-300 mb-3" />
        <p class="text-sm text-gray-600 mb-1">{{ $t('auction.biddingRestricted') }}</p>
        <p class="text-xs text-gray-400">{{ $t('auction.buyerAccountRequired') }}</p>
      </div>
    </div>

    <!-- Bid Section (for authenticated buyers, active auction) -->
    <div v-else-if="isBuyerRole && (lot.status === 'active' || lot.status === 'extended')" class="p-4 space-y-4">
      <!-- Deposit Warning -->
      <Message v-if="lot.depositRequired && !depositPaid" severity="warn" :closable="false">
        <p class="text-sm font-medium">{{ $t('auction.depositRequired') }}</p>
        <p class="text-xs mt-0.5">
          {{ $t('auction.depositAmount', { amount: formatCurrency(lot.depositAmount || 0) }) }}
        </p>
      </Message>

      <!-- Bid Input -->
      <div>
        <label class="text-sm font-medium text-gray-700 mb-1.5 block">{{ $t('auction.yourBid') }}</label>
        <InputNumber
          v-model="bidAmount"
          :min="minBidAmount"
          :step="lot.minIncrement"
          mode="currency"
          currency="EUR"
          locale="en-US"
          inputClass="w-full"
          :class="{ 'border-warning': bidError }"
          :placeholder="formatCurrency(minBidAmount)"
          @keydown.enter="handlePlaceBid"
        />
        <InlineMessage v-if="bidError" severity="error" class="mt-1">{{ bidError }}</InlineMessage>
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
      <Button
        :label="bidLoading ? $t('auction.placingBid') : `${$t('auction.placeBid')} ${bidAmount ? formatCurrency(bidAmount) : ''}`"
        :loading="bidLoading"
        :disabled="bidLoading || !canBid"
        icon="pi pi-dollar"
        class="w-full"
        size="large"
        @click="handlePlaceBid"
      />

      <!-- Success message (inline) -->
      <Message v-if="showSuccess" severity="success" :closable="false">
        {{ $t('auction.bidPlaced') }}
      </Message>

      <!-- Toast notification (fixed position, always visible) -->
      <Teleport to="body">
        <Transition
          enter-active-class="transition ease-out duration-300"
          enter-from-class="transform -translate-y-full opacity-0"
          enter-to-class="transform translate-y-0 opacity-100"
          leave-active-class="transition ease-in duration-200"
          leave-from-class="transform translate-y-0 opacity-100"
          leave-to-class="transform -translate-y-full opacity-0"
        >
          <div
            v-if="showSuccess"
            class="fixed top-4 left-1/2 -translate-x-1/2 z-50 flex items-center gap-3 px-6 py-3 bg-secondary-600 text-white rounded-xl shadow-xl"
          >
            <i class="pi pi-check-circle text-xl shrink-0" />
            <span class="font-semibold">{{ $t('auction.bidPlaced') }}</span>
          </div>
        </Transition>
      </Teleport>

      <!-- Auto-Bid Section -->
      <div class="border-t pt-4">
        <div class="flex items-center justify-between mb-2">
          <label class="text-sm font-medium text-gray-700 flex items-center gap-2">
            <i class="pi pi-sync text-sm text-gray-400" />
            {{ $t('auction.autoBid') }}
          </label>
          <ToggleSwitch v-model="autoBidEnabled" @update:model-value="onToggleAutoBid" />
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
              <InputNumber
                v-model="autoBidMax"
                :min="minBidAmount"
                mode="currency"
                currency="EUR"
                locale="en-US"
                inputClass="w-full"
                :placeholder="$t('auction.enterMaxBid')"
              />
              <p class="text-xs text-gray-400 mt-1">{{ $t('auction.autoBidHint') }}</p>
              <Button
                :label="hasAutoBid ? $t('auction.updateAutoBid') : $t('auction.setAutoBid')"
                outlined
                size="small"
                class="w-full mt-2"
                :disabled="!autoBidMax || autoBidMax < minBidAmount || bidLoading"
                @click="handleSetAutoBid"
              />
              <Button
                v-if="hasAutoBid"
                :label="$t('auction.cancelAutoBid')"
                text
                severity="danger"
                size="small"
                class="w-full mt-1"
                @click="handleCancelAutoBid"
              />
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
import type { Auction } from '~/types/auction'
import { formatCurrency } from '~/utils/format'

interface Props {
  lot: Auction
}

const props = defineProps<Props>()

const { t } = useI18n()
const { isAuthenticated, login, hasRole } = useAuth()
const { placeBid, setAutoBid, cancelAutoBid, loading: bidLoading, currentBid, bidCount, minBidAmount, hasAutoBid, clearError } = useBid()

const isBuyerRole = computed(() => hasRole('buyer_active'))

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

async function handlePlaceBid() {
  bidError.value = null
  clearError()

  if (!bidAmount.value || bidAmount.value < minBidAmount.value) {
    bidError.value = t('auction.minimumBid') + ': ' + formatCurrency(minBidAmount.value)
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

async function onToggleAutoBid(newVal: boolean) {
  if (!newVal && hasAutoBid.value) {
    await handleCancelAutoBid()
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
