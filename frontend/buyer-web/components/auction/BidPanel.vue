<template>
  <div class="bg-white rounded-xl border shadow-sm overflow-hidden">
    <!-- Timer Section -->
    <div class="p-4 border-b bg-gray-50">
      <div class="flex items-center justify-between mb-2">
        <span class="text-sm text-gray-500">{{ $t('auction.timeRemaining') }}</span>
        <span
          v-if="reactiveStatus === 'active' || reactiveStatus === 'extended'"
          class="px-2 py-0.5 bg-secondary-100 text-secondary text-xs font-semibold rounded-full"
        >
          {{ $t('auction.live') }}
        </span>
        <span
          v-else-if="reactiveStatus === 'closed'"
          class="px-2 py-0.5 bg-gray-100 text-gray-600 text-xs font-semibold rounded-full"
        >
          {{ $t('auction.closed') }}
        </span>
      </div>
      <AuctionTimer :end-time="reactiveEndTime" :extended="reactiveIsExtended" @expired="onAuctionExpired" />
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
    <div v-else-if="!isBuyerRole && (reactiveStatus === 'active' || reactiveStatus === 'extended')" class="p-4">
      <div class="text-center py-4">
        <i class="pi pi-info-circle text-4xl text-gray-300 mb-3" />
        <p class="text-sm text-gray-600 mb-1">{{ $t('auction.biddingRestricted') }}</p>
        <p class="text-xs text-gray-400">{{ $t('auction.buyerAccountRequired') }}</p>
      </div>
    </div>

    <!-- Bid Section (for authenticated buyers, active auction) -->
    <div v-else-if="isBuyerRole && (reactiveStatus === 'active' || reactiveStatus === 'extended')" class="p-4 space-y-4">
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

      <!-- Place Bid Button: native <button> ensures click always fires (PrimeVue
           Button component can silently swallow clicks in certain reactive states) -->
      <button
        ref="bidButtonRef"
        type="button"
        class="p-button p-component w-full text-lg py-3 font-semibold"
        :disabled="bidLoading || !canBid"
        @click="handlePlaceBid"
      >
        <i v-if="bidLoading" class="pi pi-spinner pi-spin mr-2" />
        <i v-else class="pi pi-dollar mr-2" />
        <span>
          {{ bidLoading ? $t('auction.placingBid') : `${$t('auction.placeBid')} ${bidAmount ? formatCurrency(bidAmount) : ''}` }}
        </span>
      </button>

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
    <div v-else-if="reactiveStatus === 'closed'" class="p-4">
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
import { useAuctionStore } from '~/stores/auction'

interface Props {
  lot: Auction
}

const props = defineProps<Props>()

const { t } = useI18n()
const { isAuthenticated, login, hasRole } = useAuth()
const { placeBid, setAutoBid, cancelAutoBid, loading: bidLoading, currentBid, bidCount, minBidAmount, hasAutoBid, clearError } = useBid()
const auctionStore = useAuctionStore()

// Use the store's reactive timerEndTime so anti-sniping extensions update the countdown
const reactiveEndTime = computed(() => auctionStore.timerEndTime ?? props.lot.endTime)
const reactiveIsExtended = computed(() => auctionStore.isExtended)
// Use the store's reactive status so WebSocket close/extend events update the UI
const reactiveStatus = computed(() => auctionStore.currentAuction?.status ?? props.lot.status)

const isBuyerRole = computed(() => hasRole('buyer_active'))

const bidButtonRef = ref<HTMLButtonElement | null>(null)
const bidAmount = ref<number | null>(null)
const bidError = ref<string | null>(null)
const autoBidEnabled = ref(false)
const autoBidMax = ref<number | null>(null)
const showSuccess = ref(false)
const depositPaid = ref(true) // Assumed from API in real app

const increments = [50, 100, 500]

const canBid = computed(() => {
  // Allow bidding when bidAmount is null but minBidAmount is valid (handlePlaceBid will use minBidAmount)
  const amount = bidAmount.value || minBidAmount.value
  if (!amount || amount <= 0) return false
  if (bidAmount.value && bidAmount.value < minBidAmount.value) return false
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

  // Guard: auctionId must be a non-empty string (lot.id = auctionId from mapAuctionResponse)
  const auctionId = props.lot.id
  if (!auctionId) {
    bidError.value = 'Cannot place bid: auction ID is missing'
    return
  }

  // Fall back to minimum bid if bidAmount is null (e.g., InputNumber didn't sync v-model)
  const effectiveAmount = bidAmount.value || minBidAmount.value
  if (!effectiveAmount || effectiveAmount < minBidAmount.value) {
    bidError.value = t('auction.minimumBid') + ': ' + formatCurrency(minBidAmount.value)
    return
  }

  // Ensure bidAmount ref is in sync for the button label
  bidAmount.value = effectiveAmount

  try {
    // Call the API directly when useBid().placeBid rejects due to
    // authentication state mismatch (keycloak plugin may report
    // isAuthenticated=false even though the user is logged in and
    // has a valid token in the auth store).
    const { $api } = useNuxtApp()
    const api = $api as typeof $fetch
    const response = await api<Record<string, unknown>>(`/auctions/${auctionId}/bids`, {
      method: 'POST',
      body: { amount: effectiveAmount },
    })
    const raw = (response?.data ?? response) as Record<string, unknown>
    const bid = {
      id: (raw.bidId ?? '') as string,
      auctionId,
      bidderId: '',
      bidderLabel: 'You',
      amount: (raw.newHighBid ?? raw.amount ?? effectiveAmount) as number,
      isAutoBid: false,
      timestamp: new Date().toISOString(),
    }
    auctionStore.addBid(bid)

    showSuccess.value = true
    bidAmount.value = minBidAmount.value
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
