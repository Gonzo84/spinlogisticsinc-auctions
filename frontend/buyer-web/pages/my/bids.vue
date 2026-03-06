<template>
  <div class="max-w-7xl mx-auto px-4 py-8">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">{{ $t('bids.title') }}</h1>
      <p class="text-gray-500 text-sm mt-1">{{ $t('bids.subtitle') }}</p>
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
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-16">
      <ProgressSpinner style="width: 50px; height: 50px" />
    </div>

    <!-- Empty State -->
    <div v-else-if="filteredBids.length === 0" class="text-center py-16">
      <i class="pi pi-clipboard text-gray-300 mb-4" style="font-size: 3.5rem" />
      <h3 class="text-lg font-medium text-gray-900 mb-2">{{ $t('bids.empty') }}</h3>
      <p class="text-gray-500 mb-6">{{ $t('bids.emptyHint') }}</p>
      <NuxtLink to="/search" class="px-6 py-2.5 bg-primary text-white font-medium rounded-lg hover:bg-primary-800 transition-colors">
        {{ $t('bids.browseAuctions') }}
      </NuxtLink>
    </div>

    <!-- Bids List -->
    <div v-else class="space-y-4">
      <div
        v-for="bid in filteredBids"
        :key="bid.id"
        class="bg-white border rounded-xl p-4 hover:shadow-sm transition-shadow"
      >
        <div class="flex items-center gap-4">
          <NuxtLink :to="`/lots/${bid.lotId}`" class="shrink-0">
            <div class="w-20 h-20 rounded-lg bg-gray-100 overflow-hidden">
              <img v-if="bid.imageUrl" :src="bid.imageUrl" :alt="bid.title" class="w-full h-full object-cover" loading="lazy">
            </div>
          </NuxtLink>
          <div class="flex-1 min-w-0">
            <NuxtLink :to="`/lots/${bid.lotId}`" class="hover:text-primary transition-colors">
              <h3 class="font-semibold text-gray-900 line-clamp-1">{{ bid.title }}</h3>
            </NuxtLink>
            <div class="flex items-center gap-4 mt-1 text-sm">
              <div>
                <span class="text-gray-500">{{ $t('bids.yourBid') }}:</span>
                <span class="font-bold text-gray-900 ml-1">{{ formatCurrency(bid.amount) }}</span>
              </div>
              <div>
                <span class="text-gray-500">{{ $t('bids.currentBid') }}:</span>
                <span class="font-bold text-primary ml-1">{{ formatCurrency(bid.currentBid) }}</span>
              </div>
            </div>
          </div>
          <span
            class="px-3 py-1 rounded-full text-xs font-semibold"
            :class="bidStatusClass(bid.status)"
          >
            {{ bid.status }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { BidEntry } from '~/types/auction'
import { formatCurrency } from '~/utils/format'
import { unwrapApiResponse } from '~/utils/api-response'

definePageMeta({ middleware: 'auth' })

const { t } = useI18n()

const loading = ref(false)
const bids = ref<BidEntry[]>([])
const activeStatus = ref('all')

onMounted(() => {
  fetchBids()
})

const statusFilters = computed(() => [
  { value: 'all', label: t('bids.all') },
  { value: 'winning', label: t('bids.winning') },
  { value: 'outbid', label: t('bids.outbid') },
  { value: 'won', label: t('bids.won') },
  { value: 'lost', label: t('bids.lost') },
])

const filteredBids = computed(() => {
  if (activeStatus.value === 'all') return bids.value
  return bids.value.filter((b) => b.status === activeStatus.value)
})

function bidStatusClass(status: string): string {
  switch (status) {
    case 'winning': return 'bg-secondary-50 text-secondary'
    case 'outbid': return 'bg-accent-50 text-accent'
    case 'won': return 'bg-secondary-50 text-secondary-700'
    case 'lost': return 'bg-gray-100 text-gray-600'
    default: return 'bg-gray-100 text-gray-600'
  }
}

async function fetchBids() {
  loading.value = true
  try {
    const { $api } = useNuxtApp()
    const api = $api as typeof $fetch
    const raw = await api<Record<string, unknown>>('/users/me/bids')
    const data = unwrapApiResponse(raw)
    bids.value = (Array.isArray(data.items) ? data.items : []) as BidEntry[]
  } catch {
    bids.value = []
  } finally {
    loading.value = false
  }
}

useHead({
  title: t('bids.pageTitle'),
})
</script>
