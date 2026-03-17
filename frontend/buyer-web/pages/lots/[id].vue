<template>
  <div class="max-w-7xl mx-auto px-4 py-6">
    <!-- Breadcrumb -->
    <Breadcrumb :model="breadcrumbItems" :home="breadcrumbHome" class="mb-6">
      <template #item="{ item, props: itemProps }">
        <NuxtLink v-if="item.to" :to="item.to as string" v-bind="itemProps.action">
          <span v-if="item.icon" :class="item.icon" />
          <span v-if="item.label" :class="item.class">{{ item.label }}</span>
        </NuxtLink>
        <span v-else v-bind="itemProps.action" :class="item.class">{{ item.label }}</span>
      </template>
    </Breadcrumb>

    <!-- Loading State -->
    <div v-if="pending" class="flex items-center justify-center py-24">
      <ProgressSpinner />
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="text-center py-16">
      <h2 class="text-xl font-semibold text-gray-900 mb-2">{{ $t('common.error') }}</h2>
      <p class="text-gray-500 mb-4">{{ $t('lot.notFound') }}</p>
      <NuxtLink to="/search">
        <Button :label="$t('search.browseAll')" />
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
              <i class="pi pi-expand text-xl" />
            </button>
            <!-- Nav Arrows -->
            <button
              v-if="lot.images.length > 1"
              class="absolute left-3 top-1/2 -translate-y-1/2 p-2 bg-white/80 backdrop-blur-sm rounded-full hover:bg-white"
              @click.stop="activeImageIndex = (activeImageIndex - 1 + lot.images.length) % lot.images.length"
            >
              <i class="pi pi-chevron-left text-xl" />
            </button>
            <button
              v-if="lot.images.length > 1"
              class="absolute right-3 top-1/2 -translate-y-1/2 p-2 bg-white/80 backdrop-blur-sm rounded-full hover:bg-white"
              @click.stop="activeImageIndex = (activeImageIndex + 1) % lot.images.length"
            >
              <i class="pi pi-chevron-right text-xl" />
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
            <Button
              icon="pi pi-times"
              text
              rounded
              class="absolute top-4 right-4 z-10 !text-white hover:!bg-white/10"
              aria-label="Close fullscreen"
              @click="isFullscreen = false"
            />
            <img
              :src="lot.images[activeImageIndex]?.url"
              :alt="lot.title"
              class="max-w-full max-h-full object-contain"
              @click.stop
            >
            <Button
              v-if="lot.images.length > 1"
              icon="pi pi-chevron-left"
              text
              rounded
              class="absolute left-4 top-1/2 -translate-y-1/2 !text-white hover:!bg-white/10"
              aria-label="Previous image"
              @click.stop="activeImageIndex = (activeImageIndex - 1 + lot.images.length) % lot.images.length"
            />
            <Button
              v-if="lot.images.length > 1"
              icon="pi pi-chevron-right"
              text
              rounded
              class="absolute right-4 top-1/2 -translate-y-1/2 !text-white hover:!bg-white/10"
              aria-label="Next image"
              @click.stop="activeImageIndex = (activeImageIndex + 1) % lot.images.length"
            />
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
                <i class="pi text-xl" :class="isWatched ? 'pi-heart-fill text-red-500' : 'pi-heart text-gray-400'" />
              </button>
              <button
                class="p-2 rounded-lg border hover:bg-gray-50 transition-colors"
                :title="$t('lot.share')"
                @click="shareLot"
              >
                <i class="pi pi-share-alt text-xl text-gray-400" />
              </button>
            </div>
          </div>
          <div class="flex items-center gap-3 mt-2 text-sm text-gray-500">
            <span v-if="lot.category && !lot.category.includes('-')" class="px-2 py-1 bg-gray-100 rounded text-xs font-medium">{{ lot.category }}</span>
            <span>{{ $t('lot.lotNumber') }}: {{ lot.catalogLotId }}</span>
            <span class="flex items-center gap-1">
              <span class="text-base">{{ countryFlag(lot.country) }}</span>
              {{ lot.location }}
            </span>
          </div>
        </div>

        <!-- Description -->
        <div class="card">
          <h2 class="section-title !mb-3">{{ $t('lot.description') }}</h2>
          <div class="prose prose-sm max-w-none text-gray-700" v-html="lot.description" />
        </div>

        <!-- Specifications -->
        <div class="card">
          <h2 class="section-title">{{ $t('lot.specifications') }}</h2>
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
        <div class="card">
          <h2 class="section-title !mb-3">{{ $t('lot.location') }}</h2>
          <div class="flex items-start gap-3 mb-4">
            <i class="pi pi-map-marker text-xl text-gray-400 mt-0.5 shrink-0" />
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
        <div class="card">
          <h2 class="section-title">{{ $t('lot.seller') }}</h2>
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
                  <i class="pi pi-star-fill text-sm text-accent" />
                  {{ lot.seller?.rating?.toFixed(1) }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Bid History (Collapsible) -->
        <div class="card !p-0 overflow-hidden">
          <button
            class="w-full p-6 flex items-center justify-between hover:bg-gray-50 transition-colors"
            @click="showBidHistory = !showBidHistory"
          >
            <h2 class="text-lg font-semibold text-gray-900">
              {{ $t('auction.bidHistory') }} ({{ auctionStore.bidCount }})
            </h2>
            <i
              class="pi pi-chevron-down text-gray-400 transition-transform"
              :class="{ 'rotate-180': showBidHistory }"
            />
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
import type { Auction } from '~/types/auction'
import { formatCurrency, formatDate } from '~/utils/format'
import { getCountryFlag } from '~/utils/constants'
import { unwrapApiResponse } from '~/utils/api-response'
import { useAuctionStore } from '~/stores/auction'

const { t } = useI18n()
const route = useRoute()
const { getAuction, subscribeToAuction, unsubscribeFromAuction } = useAuction()
const auctionStore = useAuctionStore()

const lotId = computed(() => route.params.id as string)

const activeImageIndex = ref(0)
const isFullscreen = ref(false)
const showBidHistory = ref(false)
const isWatched = ref(false)
const relatedLots = ref<Auction[]>([])

const { data: lot, pending, error } = await useAsyncData(
  `lot-${lotId.value}`,
  () => getAuction(lotId.value),
  { server: false },
)

const breadcrumbHome = { icon: 'pi pi-home', to: '/' }
const breadcrumbItems = computed(() => [
  { label: t('nav.search'), to: '/search' },
  ...(lot.value ? [{ label: lot.value.title, class: 'truncate max-w-xs' }] : []),
])

let subscribedAuctionId: string | null = null

// Subscribe once auction data is loaded (useAsyncData is client-only so lot.value is initially null)
watch(lot, (lotData) => {
  const auctionId = lotData?.id
  if (auctionId && !subscribedAuctionId) {
    subscribedAuctionId = auctionId
    subscribeToAuction(auctionId)
  }
}, { immediate: true })

onMounted(() => {
  if (lotId.value) {
    checkWatchlistStatus()
  }
})

onUnmounted(() => {
  if (subscribedAuctionId) {
    unsubscribeFromAuction(subscribedAuctionId)
  }
})

function countryFlag(code: string): string {
  return getCountryFlag(code)
}

function openFullscreen() {
  isFullscreen.value = true
}

async function toggleWatchlist() {
  const { $api } = useNuxtApp()
  const api = $api as typeof $fetch
  try {
    if (isWatched.value) {
      await api(`/users/me/watchlist/${lotId.value}`, { method: 'DELETE' })
      isWatched.value = false
    } else {
      await api(`/users/me/watchlist/${lotId.value}`, { method: 'POST' })
      isWatched.value = true
    }
  } catch {
    // Silently fail - don't break the UX
  }
}

async function checkWatchlistStatus() {
  try {
    const { $api } = useNuxtApp()
    const api = $api as typeof $fetch
    const raw = await api<Record<string, unknown>>('/users/me/watchlist')
    const data = unwrapApiResponse(raw)
    const items = Array.isArray(data.items) ? data.items : []
    isWatched.value = items.some((item: Record<string, unknown>) => item.lotId === lotId.value)
  } catch {
    // Not logged in or error - leave as false
  }
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
})

useSeoMeta({
  description: computed(() => lot.value?.description?.substring(0, 160) || ''),
  ogTitle: computed(() => lot.value?.title || ''),
  ogDescription: computed(() => lot.value?.description?.substring(0, 160) || ''),
  ogType: 'website',
  ogImage: computed(() => lot.value?.images?.[0]?.url || ''),
})
</script>
