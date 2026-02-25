<template>
  <div class="max-w-7xl mx-auto px-4 py-6">
    <!-- Breadcrumb -->
    <nav class="flex items-center gap-2 text-sm text-gray-500 mb-6">
      <NuxtLink to="/" class="hover:text-primary">{{ $t('nav.home') }}</NuxtLink>
      <span>/</span>
      <NuxtLink to="/search" class="hover:text-primary">{{ $t('nav.search') }}</NuxtLink>
      <span>/</span>
      <span v-if="lot" class="text-gray-900 truncate max-w-xs">{{ lot.title }}</span>
    </nav>

    <!-- Loading State -->
    <div v-if="pending" class="animate-pulse">
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div class="lg:col-span-2">
          <div class="aspect-[4/3] bg-gray-200 rounded-xl mb-4" />
          <div class="flex gap-2">
            <div v-for="i in 4" :key="i" class="w-20 h-20 bg-gray-200 rounded-lg" />
          </div>
        </div>
        <div>
          <div class="h-64 bg-gray-200 rounded-xl" />
        </div>
      </div>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="text-center py-16">
      <h2 class="text-xl font-semibold text-gray-900 mb-2">{{ $t('common.error') }}</h2>
      <p class="text-gray-500 mb-4">{{ $t('lot.notFound') }}</p>
      <NuxtLink to="/search" class="px-6 py-2 bg-primary text-white rounded-lg font-medium">
        {{ $t('search.browseAll') }}
      </NuxtLink>
    </div>

    <!-- Lot Detail -->
    <div v-else-if="lot" class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      <!-- Left Column: Images + Details -->
      <div class="lg:col-span-2 space-y-6">
        <!-- Image Gallery -->
        <div>
          <!-- Main Image -->
          <div
            class="relative aspect-[4/3] bg-gray-100 rounded-xl overflow-hidden cursor-zoom-in mb-3"
            @click="openFullscreen"
          >
            <img
              :src="lot.images[activeImageIndex]?.url"
              :alt="lot.title"
              class="w-full h-full object-contain"
            >
            <div class="absolute top-3 left-3 flex gap-2">
              <SharedCO2Badge v-if="lot.co2Savings" :amount="lot.co2Savings" />
            </div>
            <button
              class="absolute top-3 right-3 p-2 bg-white/80 backdrop-blur-sm rounded-full hover:bg-white transition-colors"
              @click.stop="openFullscreen"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4" />
              </svg>
            </button>
            <!-- Nav Arrows -->
            <button
              v-if="lot.images.length > 1"
              class="absolute left-3 top-1/2 -translate-y-1/2 p-2 bg-white/80 backdrop-blur-sm rounded-full hover:bg-white"
              @click.stop="activeImageIndex = (activeImageIndex - 1 + lot.images.length) % lot.images.length"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <button
              v-if="lot.images.length > 1"
              class="absolute right-3 top-1/2 -translate-y-1/2 p-2 bg-white/80 backdrop-blur-sm rounded-full hover:bg-white"
              @click.stop="activeImageIndex = (activeImageIndex + 1) % lot.images.length"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>
          <!-- Thumbnails -->
          <div class="flex gap-2 overflow-x-auto pb-2">
            <button
              v-for="(image, index) in lot.images"
              :key="index"
              class="w-20 h-20 shrink-0 rounded-lg overflow-hidden border-2 transition-colors"
              :class="index === activeImageIndex ? 'border-primary' : 'border-transparent hover:border-gray-300'"
              @click="activeImageIndex = index"
            >
              <img :src="image.url" :alt="`${lot.title} - ${index + 1}`" class="w-full h-full object-cover" loading="lazy">
            </button>
          </div>
        </div>

        <!-- Fullscreen Modal -->
        <Teleport to="body">
          <div v-if="isFullscreen" class="fixed inset-0 z-50 bg-black flex items-center justify-center" @click="isFullscreen = false">
            <button class="absolute top-4 right-4 p-3 text-white hover:bg-white/10 rounded-full z-10" @click="isFullscreen = false">
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
            <img
              :src="lot.images[activeImageIndex]?.url"
              :alt="lot.title"
              class="max-w-full max-h-full object-contain"
              @click.stop
            >
            <button
              v-if="lot.images.length > 1"
              class="absolute left-4 top-1/2 -translate-y-1/2 p-3 text-white hover:bg-white/10 rounded-full"
              @click.stop="activeImageIndex = (activeImageIndex - 1 + lot.images.length) % lot.images.length"
            >
              <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <button
              v-if="lot.images.length > 1"
              class="absolute right-4 top-1/2 -translate-y-1/2 p-3 text-white hover:bg-white/10 rounded-full"
              @click.stop="activeImageIndex = (activeImageIndex + 1) % lot.images.length"
            >
              <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>
        </Teleport>

        <!-- Title & Actions -->
        <div>
          <div class="flex items-start justify-between gap-4">
            <h1 class="text-2xl font-bold text-gray-900">{{ lot.title }}</h1>
            <div class="flex items-center gap-2 shrink-0">
              <button
                class="p-2 rounded-lg border hover:bg-gray-50 transition-colors"
                :title="$t('lot.watchlist')"
                @click="toggleWatchlist"
              >
                <svg class="w-5 h-5" :class="isWatched ? 'text-warning fill-current' : 'text-gray-400'" viewBox="0 0 24 24" stroke="currentColor" fill="none">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                </svg>
              </button>
              <button
                class="p-2 rounded-lg border hover:bg-gray-50 transition-colors"
                :title="$t('lot.share')"
                @click="shareLot"
              >
                <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
                </svg>
              </button>
            </div>
          </div>
          <div class="flex items-center gap-3 mt-2 text-sm text-gray-500">
            <span v-if="lot.category && !lot.category.includes('-')" class="px-2 py-1 bg-gray-100 rounded text-xs font-medium">{{ lot.category }}</span>
            <span>{{ $t('lot.lotNumber') }}: {{ lot.lotNumber }}</span>
            <span class="flex items-center gap-1">
              <span class="text-base">{{ countryFlag(lot.country) }}</span>
              {{ lot.location }}
            </span>
          </div>
        </div>

        <!-- Description -->
        <div class="bg-white rounded-xl border p-6">
          <h2 class="text-lg font-semibold text-gray-900 mb-3">{{ $t('lot.description') }}</h2>
          <div class="prose prose-sm max-w-none text-gray-700" v-html="lot.description" />
        </div>

        <!-- Specifications -->
        <div class="bg-white rounded-xl border p-6">
          <h2 class="text-lg font-semibold text-gray-900 mb-4">{{ $t('lot.specifications') }}</h2>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-3">
            <div
              v-for="spec in lot.specifications"
              :key="spec.key"
              class="flex items-center justify-between py-2 border-b border-gray-100"
            >
              <span class="text-sm text-gray-500">{{ spec.label }}</span>
              <span class="text-sm font-medium text-gray-900">{{ spec.value }}</span>
            </div>
          </div>
        </div>

        <!-- Location -->
        <div class="bg-white rounded-xl border p-6">
          <h2 class="text-lg font-semibold text-gray-900 mb-3">{{ $t('lot.location') }}</h2>
          <div class="flex items-start gap-3 mb-4">
            <svg class="w-5 h-5 text-gray-400 mt-0.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            <div>
              <p class="font-medium text-gray-900">{{ lot.location }}</p>
              <p class="text-sm text-gray-500">{{ lot.address }}</p>
            </div>
          </div>
          <div class="aspect-[2/1] bg-gray-200 rounded-lg overflow-hidden">
            <div class="w-full h-full flex items-center justify-center text-gray-400 text-sm">
              {{ $t('lot.mapPlaceholder') }}
            </div>
          </div>
        </div>

        <!-- Seller Info -->
        <div class="bg-white rounded-xl border p-6">
          <h2 class="text-lg font-semibold text-gray-900 mb-4">{{ $t('lot.seller') }}</h2>
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-full bg-primary-100 flex items-center justify-center text-primary font-bold text-lg">
              {{ lot.seller?.name?.charAt(0) || 'S' }}
            </div>
            <div>
              <p class="font-medium text-gray-900">{{ lot.seller?.name }}</p>
              <div class="flex items-center gap-2 text-sm text-gray-500">
                <span>{{ lot.seller?.totalAuctions }} {{ $t('lot.auctions') }}</span>
                <span class="w-1 h-1 rounded-full bg-gray-300" />
                <span class="flex items-center gap-1">
                  <svg class="w-4 h-4 text-accent" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                  </svg>
                  {{ lot.seller?.rating?.toFixed(1) }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Bid History (Collapsible) -->
        <div class="bg-white rounded-xl border overflow-hidden">
          <button
            class="w-full p-6 flex items-center justify-between hover:bg-gray-50 transition-colors"
            @click="showBidHistory = !showBidHistory"
          >
            <h2 class="text-lg font-semibold text-gray-900">
              {{ $t('auction.bidHistory') }} ({{ lot.bidCount }})
            </h2>
            <svg
              class="w-5 h-5 text-gray-400 transition-transform"
              :class="{ 'rotate-180': showBidHistory }"
              fill="none" stroke="currentColor" viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
            </svg>
          </button>
          <div v-if="showBidHistory" class="px-6 pb-6">
            <div class="space-y-3">
              <div
                v-for="(bid, index) in lot.bidHistory"
                :key="bid.id"
                class="flex items-center justify-between py-2 border-b border-gray-100 last:border-0"
              >
                <div class="flex items-center gap-3">
                  <span
                    class="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold"
                    :class="index === 0 ? 'bg-primary text-white' : 'bg-gray-100 text-gray-600'"
                  >
                    {{ index + 1 }}
                  </span>
                  <div>
                    <span class="font-medium text-gray-900">{{ bid.bidderLabel }}</span>
                    <span v-if="bid.isAutoBid" class="ml-2 text-xs text-gray-400 bg-gray-100 px-1.5 py-0.5 rounded">Auto</span>
                  </div>
                </div>
                <div class="text-right">
                  <p class="font-bold text-gray-900">{{ formatCurrency(bid.amount) }}</p>
                  <p class="text-xs text-gray-500">{{ formatDate(bid.timestamp) }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Column: Bid Panel -->
      <div class="lg:col-span-1">
        <div class="sticky top-24">
          <AuctionBidPanel :lot="lot" />
        </div>
      </div>
    </div>

    <!-- Related Lots -->
    <section v-if="relatedLots.length > 0" class="mt-12">
      <h2 class="text-2xl font-bold text-gray-900 mb-6">{{ $t('lot.relatedLots') }}</h2>
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <AuctionLotCard
          v-for="related in relatedLots"
          :key="related.id"
          :lot="related"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import type { Auction } from '~/stores/auction'

const { t } = useI18n()
const route = useRoute()
const { getAuction, subscribeToAuction, unsubscribeFromAuction } = useAuction()

const lotId = computed(() => route.params.id as string)
const activeImageIndex = ref(0)
const isFullscreen = ref(false)
const showBidHistory = ref(false)
const isWatched = ref(false)
const relatedLots = ref<Auction[]>([])

const { data: lot, pending, error } = await useAsyncData(
  `lot-${lotId.value}`,
  () => getAuction(lotId.value),
)

onMounted(() => {
  if (lotId.value) {
    subscribeToAuction(lotId.value)
  }
})

onUnmounted(() => {
  if (lotId.value) {
    unsubscribeFromAuction(lotId.value)
  }
})

const countryFlags: Record<string, string> = {
  NL: '\ud83c\uddf3\ud83c\uddf1',
  DE: '\ud83c\udde9\ud83c\uddea',
  FR: '\ud83c\uddeb\ud83c\uddf7',
  BE: '\ud83c\udde7\ud83c\uddea',
  PL: '\ud83c\uddf5\ud83c\uddf1',
  IT: '\ud83c\uddee\ud83c\uddf9',
  RO: '\ud83c\uddf7\ud83c\uddf4',
  ES: '\ud83c\uddea\ud83c\uddf8',
  AT: '\ud83c\udde6\ud83c\uddf9',
}

function countryFlag(code: string): string {
  return countryFlags[code] || code
}

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-IE', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: 0,
  }).format(amount)
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleString()
}

function openFullscreen() {
  isFullscreen.value = true
}

function toggleWatchlist() {
  isWatched.value = !isWatched.value
}

async function shareLot() {
  if (navigator.share) {
    await navigator.share({
      title: lot.value?.title,
      url: window.location.href,
    })
  } else {
    await navigator.clipboard.writeText(window.location.href)
  }
}

useHead({
  title: computed(() => lot.value?.title || t('lot.loading')),
  meta: [
    { name: 'description', content: computed(() => lot.value?.description?.substring(0, 160) || '') },
  ],
})
</script>
