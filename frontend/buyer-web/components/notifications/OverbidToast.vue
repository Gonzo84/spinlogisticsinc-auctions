<template>
  <Teleport to="body">
    <TransitionGroup
      tag="div"
      class="fixed top-20 right-4 z-50 flex flex-col gap-3 w-96 max-w-[calc(100vw-2rem)]"
      enter-active-class="transition ease-out duration-300"
      enter-from-class="transform translate-x-full opacity-0"
      enter-to-class="transform translate-x-0 opacity-100"
      leave-active-class="transition ease-in duration-200"
      leave-from-class="transform translate-x-0 opacity-100"
      leave-to-class="transform translate-x-full opacity-0"
    >
      <div
        v-for="toast in activeToasts"
        :key="toast.id"
        class="bg-white rounded-xl shadow-xl border border-warning-200 overflow-hidden animate-slide-in"
      >
        <!-- Red Top Bar -->
        <div class="h-1 bg-warning" />

        <div class="p-4">
          <div class="flex items-start gap-3">
            <!-- Warning Icon -->
            <div class="w-10 h-10 rounded-full bg-warning-50 flex items-center justify-center shrink-0">
              <svg class="w-5 h-5 text-warning" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
              </svg>
            </div>

            <!-- Content -->
            <div class="flex-1 min-w-0">
              <h4 class="font-semibold text-gray-900 text-sm">{{ $t('notification.overbidTitle') }}</h4>
              <p class="text-xs text-gray-600 mt-0.5 line-clamp-2">
                {{ toast.lotTitle }}
              </p>
              <p v-if="toast.amount" class="text-sm font-bold text-warning mt-1">
                {{ $t('notification.newBid') }}: {{ formatCurrency(toast.amount) }}
              </p>
            </div>

            <!-- Close -->
            <button
              class="p-1 text-gray-400 hover:text-gray-600 shrink-0"
              @click="dismissToast(toast.id)"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- Actions -->
          <div class="flex items-center gap-2 mt-3">
            <NuxtLink
              :to="`/lots/${toast.auctionId}`"
              class="flex-1 py-2 bg-primary text-white text-sm font-semibold rounded-lg text-center hover:bg-primary-800 transition-colors"
              @click="dismissToast(toast.id)"
            >
              {{ $t('notification.rebid') }}
            </NuxtLink>
            <button
              class="px-4 py-2 text-sm text-gray-600 font-medium border rounded-lg hover:bg-gray-50 transition-colors"
              @click="dismissToast(toast.id)"
            >
              {{ $t('notification.dismiss') }}
            </button>
          </div>

          <!-- Auto-dismiss progress bar -->
          <div class="mt-3 h-0.5 bg-gray-100 rounded-full overflow-hidden">
            <div
              class="h-full bg-warning rounded-full transition-all duration-[10000ms] ease-linear"
              :style="{ width: toast.dismissing ? '0%' : '100%' }"
            />
          </div>
        </div>
      </div>
    </TransitionGroup>
  </Teleport>
</template>

<script setup lang="ts">
interface OverbidToast {
  id: string
  auctionId: string
  lotTitle: string
  amount?: number
  dismissing: boolean
  timeoutId?: ReturnType<typeof setTimeout>
}

const activeToasts = ref<OverbidToast[]>([])

const { overbidNotifications, dismissNotification } = useNotifications()

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-IE', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(amount)
}

function addToast(data: { id: string; auctionId: string; lotTitle: string; amount?: number }) {
  // Prevent duplicates
  if (activeToasts.value.some((t) => t.id === data.id)) return

  const toast: OverbidToast = {
    id: data.id,
    auctionId: data.auctionId,
    lotTitle: data.lotTitle,
    amount: data.amount,
    dismissing: false,
  }

  activeToasts.value.push(toast)

  // Trigger dismissing animation next tick
  nextTick(() => {
    const t = activeToasts.value.find((t) => t.id === data.id)
    if (t) t.dismissing = true
  })

  // Auto-dismiss after 10 seconds
  toast.timeoutId = setTimeout(() => {
    dismissToast(data.id)
  }, 10000)

  // Max 3 toasts at a time
  if (activeToasts.value.length > 3) {
    const oldest = activeToasts.value[0]
    dismissToast(oldest.id)
  }
}

function dismissToast(id: string) {
  const index = activeToasts.value.findIndex((t) => t.id === id)
  if (index !== -1) {
    const toast = activeToasts.value[index]
    if (toast.timeoutId) {
      clearTimeout(toast.timeoutId)
    }
    activeToasts.value.splice(index, 1)
    dismissNotification(id)
  }
}

// Watch for new overbid notifications
watch(overbidNotifications, (notifications) => {
  for (const notif of notifications) {
    if (!activeToasts.value.some((t) => t.id === notif.id)) {
      addToast({
        id: notif.id,
        auctionId: notif.auctionId || '',
        lotTitle: notif.lotTitle || notif.message,
        amount: notif.amount,
      })
    }
  }
}, { deep: true })

onUnmounted(() => {
  activeToasts.value.forEach((t) => {
    if (t.timeoutId) clearTimeout(t.timeoutId)
  })
})

defineExpose({ addToast })
</script>
