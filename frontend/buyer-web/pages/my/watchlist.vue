<template>
  <div class="max-w-7xl mx-auto px-4 py-8">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">{{ $t('watchlistPage.title') }}</h1>
      <p class="text-gray-500 text-sm mt-1">{{ $t('watchlistPage.subtitle') }}</p>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-16">
      <ProgressSpinner style="width: 50px; height: 50px" />
    </div>

    <!-- Empty State -->
    <div v-else-if="watchlist.length === 0" class="text-center py-16">
      <i class="pi pi-heart text-gray-300 mb-4" style="font-size: 3.5rem" />
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
                <p class="text-xs text-gray-500">{{ $t('common.startingBid') }}</p>
                <p class="text-lg font-bold text-primary">{{ formatCurrency(lot.startingBid) }}</p>
              </div>
              <button
                class="p-2 hover:text-red-600 transition-colors"
                @click.prevent="removeFromWatchlist(lot.id)"
                :title="$t('watchlistPage.remove')"
              >
                <i class="pi pi-heart-fill text-lg text-red-500" />
              </button>
            </div>
          </div>
        </NuxtLink>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { WatchlistLot } from '~/types/auction'
import { formatCurrency } from '~/utils/format'
import { unwrapApiResponse } from '~/utils/api-response'

definePageMeta({ middleware: 'auth' })

const { t } = useI18n()

const loading = ref(false)
const watchlist = ref<WatchlistLot[]>([])

onMounted(() => {
  fetchWatchlist()
})

async function removeFromWatchlist(lotId: string) {
  try {
    const { $api } = useNuxtApp()
    const api = $api as typeof $fetch
    await api(`/users/me/watchlist/${lotId}`, { method: 'DELETE' })
  } catch {
    // Continue with UI removal even if API fails
  }
  watchlist.value = watchlist.value.filter((l) => l.id !== lotId)
}

async function fetchWatchlist() {
  loading.value = true
  try {
    const { $api } = useNuxtApp()
    const api = $api as typeof $fetch
    const raw = await api<Record<string, unknown>>('/users/me/watchlist')
    const data = unwrapApiResponse(raw)
    const items = (Array.isArray(data.items) ? data.items : []) as Array<{ lotId?: string; addedAt?: string }>

    // Fetch lot details for each watchlist item
    const lotDetails = await Promise.all(
      items
        .filter(item => item.lotId)
        .map(async (item) => {
          try {
            const lotRaw = await api<Record<string, unknown>>(`/lots/${item.lotId}`)
            const lot = unwrapApiResponse(lotRaw)
            return {
              id: (lot as Record<string, unknown>).id as string ?? item.lotId!,
              title: (lot as Record<string, unknown>).title as string ?? '',
              imageUrl: ((lot as Record<string, unknown>).primaryImageUrl as string) ?? ((lot as Record<string, unknown>).images as Array<{ url: string }> | undefined)?.[0]?.url ?? '',
              startingBid: (lot as Record<string, unknown>).startingBid as number ?? 0,
              location: `${(lot as Record<string, unknown>).locationCity ?? ''}, ${(lot as Record<string, unknown>).locationCountry ?? ''}`,
            } satisfies WatchlistLot
          } catch {
            return null
          }
        })
    )
    watchlist.value = lotDetails.filter((l): l is WatchlistLot => l !== null)
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
