<template>
  <div class="max-w-7xl mx-auto px-4 py-8">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">{{ $t('watchlistPage.title') }}</h1>
      <p class="text-gray-500 text-sm mt-1">{{ $t('watchlistPage.subtitle') }}</p>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="i in 6" :key="i" class="bg-white rounded-xl border p-4 animate-pulse">
        <div class="aspect-[4/3] bg-gray-200 rounded-lg mb-4" />
        <div class="h-4 bg-gray-200 rounded mb-2 w-3/4" />
        <div class="h-4 bg-gray-200 rounded mb-4 w-1/2" />
        <div class="h-6 bg-gray-200 rounded w-1/3" />
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="watchlist.length === 0" class="text-center py-16">
      <svg class="w-16 h-16 text-gray-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
      </svg>
      <h3 class="text-lg font-medium text-gray-900 mb-2">{{ $t('watchlistPage.empty') }}</h3>
      <p class="text-gray-500 mb-6">{{ $t('watchlistPage.emptyHint') }}</p>
      <NuxtLink to="/search" class="px-6 py-2.5 bg-primary text-white font-medium rounded-lg hover:bg-primary-800 transition-colors">
        {{ $t('watchlistPage.browseAuctions') }}
      </NuxtLink>
    </div>

    <!-- Watchlist Grid -->
    <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
      <div
        v-for="lot in watchlist"
        :key="lot.id"
        class="bg-white rounded-xl border overflow-hidden hover:shadow-md transition-shadow"
      >
        <NuxtLink :to="`/lots/${lot.id}`">
          <div class="relative aspect-[4/3] bg-gray-100">
            <img v-if="lot.imageUrl" :src="lot.imageUrl" :alt="lot.title" class="w-full h-full object-cover" loading="lazy">
          </div>
          <div class="p-4">
            <h3 class="font-semibold text-gray-900 line-clamp-2 mb-2">{{ lot.title }}</h3>
            <div class="flex items-center justify-between">
              <div>
                <p class="text-xs text-gray-500">Starting bid</p>
                <p class="text-lg font-bold text-primary">{{ formatCurrency(lot.startingBid) }}</p>
              </div>
              <button
                class="p-2 text-gray-400 hover:text-warning transition-colors"
                @click.prevent="removeFromWatchlist(lot.id)"
                :title="$t('watchlistPage.remove')"
              >
                <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                </svg>
              </button>
            </div>
          </div>
        </NuxtLink>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const { t } = useI18n()
const { requireAuth } = useAuth()

interface WatchlistLot {
  id: string
  title: string
  imageUrl?: string
  startingBid: number
  location?: string
}

const loading = ref(false)
const watchlist = ref<WatchlistLot[]>([])

onMounted(() => {
  if (!requireAuth('/my/watchlist')) return
  fetchWatchlist()
})

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-IE', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(amount)
}

function removeFromWatchlist(lotId: string) {
  watchlist.value = watchlist.value.filter((l) => l.id !== lotId)
}

async function fetchWatchlist() {
  loading.value = true
  try {
    const { $api } = useNuxtApp()
    const api = $api as typeof $fetch
    const raw = await api<Record<string, unknown>>('/users/me/watchlist')
    const data = (raw && typeof raw === 'object' && 'data' in raw && raw.data && typeof raw.data === 'object')
      ? raw.data as Record<string, unknown>
      : raw as Record<string, unknown>
    watchlist.value = (Array.isArray(data.items) ? data.items : []) as WatchlistLot[]
  } catch {
    watchlist.value = []
  } finally {
    loading.value = false
  }
}

useHead({
  title: t('watchlistPage.pageTitle'),
})
</script>
