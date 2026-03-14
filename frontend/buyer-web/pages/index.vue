<template>
  <div>
    <!-- Hero Section -->
    <section class="relative bg-gradient-to-br from-primary-700 via-primary to-primary-800 text-white py-20 px-4">
      <div class="max-w-5xl mx-auto text-center">
        <h1 class="text-4xl md:text-5xl font-bold mb-4">
          {{ $t('home.heroTitle') }}
        </h1>
        <p class="text-lg md:text-xl text-primary-100 mb-8 max-w-2xl mx-auto">
          {{ $t('home.heroSubtitle') }}
        </p>
        <div class="max-w-2xl mx-auto">
          <SearchBar />
        </div>
      </div>
      <div class="absolute inset-0 bg-[url('/images/hero-pattern.svg')] opacity-10 pointer-events-none" />
    </section>

    <!-- Country Selector Bar -->
    <section class="bg-white border-b py-4 px-4">
      <div class="max-w-7xl mx-auto">
        <div class="flex items-center gap-2 overflow-x-auto pb-2">
          <span class="text-sm font-medium text-gray-500 shrink-0">{{ $t('home.browseByCountry') }}:</span>
          <button
            v-for="country in COUNTRIES"
            :key="country.code"
            class="flex items-center gap-2 px-4 py-2 rounded-full border hover:border-primary hover:bg-primary-50 transition-colors shrink-0"
            :class="{ 'border-primary bg-primary-50': selectedCountry === country.code }"
            @click="selectCountry(country.code)"
          >
            <span class="text-xl">{{ country.flag }}</span>
            <span class="text-sm font-medium">{{ $t('countries.' + country.code) }}</span>
          </button>
        </div>
      </div>
    </section>

    <!-- Category Pills -->
    <HomeCategoryGrid />

    <!-- Featured Auctions Carousel -->
    <HomeHeroCarousel :auctions="featuredAuctions" />

    <!-- New Lots Grid -->
    <section class="py-10 px-4">
      <div class="max-w-7xl mx-auto">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-2xl font-bold text-gray-900">{{ $t('home.newLots') }}</h2>
          <NuxtLink to="/search?sort=newest" class="text-primary font-medium hover:underline">
            {{ $t('home.viewAll') }} &rarr;
          </NuxtLink>
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          <AuctionLotCard
            v-for="lot in newLots"
            :key="lot.id"
            :lot="lot"
          />
        </div>
      </div>
    </section>

    <!-- CO2 Savings Counter -->
    <HomeCO2Counter />

    <!-- How It Works -->
    <HomeHowItWorks />
  </div>
</template>

<script setup lang="ts">
import { COUNTRIES } from '~/utils/constants'
import { unwrapApiResponse } from '~/utils/api-response'
import { mapAuctionResponse } from '~/utils/auction-mapper'

const { t } = useI18n()

const selectedCountry = ref<string | null>(null)

async function fetchLotsFromCatalog(params: Record<string, string | number>) {
  const { $api } = useNuxtApp()
  const api = $api as typeof $fetch
  try {
    const raw = await api<Record<string, unknown>>('/lots', { params })
    const data = unwrapApiResponse(raw)
    const items = Array.isArray(data.items) ? data.items as Record<string, unknown>[] : []
    const mapped = items.map((lot) => ({
      id: (lot.id ?? '') as string,
      title: (lot.title ?? '') as string,
      images: [{ url: (lot.primaryImageUrl ?? '') as string, alt: (lot.title ?? '') as string }],
      imageUrl: (lot.primaryImageUrl ?? '') as string,
      currentBid: (lot.startingBid ?? 0) as number,
      endTime: '',
    }))
    // Deduplicate by title to handle repeated seed data runs
    const seen = new Set<string>()
    return mapped.filter((lot) => {
      if (seen.has(lot.title)) return false
      seen.add(lot.title)
      return true
    })
  } catch {
    return []
  }
}

const { data: featuredAuctions } = useAsyncData('featured-auctions', async () => {
  const { $api } = useNuxtApp()
  const api = $api as typeof $fetch
  try {
    // First try to get explicitly featured auctions
    const raw = await api<Record<string, unknown>>('/auctions', {
      params: { featured: true, status: 'ACTIVE', size: 12 },
    })
    const data = unwrapApiResponse(raw)
    const items = Array.isArray(data.items) ? data.items as Record<string, unknown>[] : []
    const mapped = items.map((item) => mapAuctionResponse(item))
    const seen = new Set<string>()
    const deduped = mapped.filter((lot) => {
      if (seen.has(lot.title)) return false
      seen.add(lot.title)
      return true
    })

    // If no featured auctions exist yet, fall back to all active
    if (deduped.length === 0) {
      const fallbackRaw = await api<Record<string, unknown>>('/auctions', {
        params: { status: 'ACTIVE', size: 9 },
      })
      const fallbackData = unwrapApiResponse(fallbackRaw)
      const fallbackItems = Array.isArray(fallbackData.items) ? fallbackData.items as Record<string, unknown>[] : []
      const fallbackMapped = fallbackItems.map((item) => mapAuctionResponse(item))
      const fallbackSeen = new Set<string>()
      return fallbackMapped.filter((lot) => {
        if (fallbackSeen.has(lot.title)) return false
        fallbackSeen.add(lot.title)
        return true
      })
    }

    return deduped
  } catch {
    // Fallback to catalog if auction-engine unavailable
    return await fetchLotsFromCatalog({ status: 'ACTIVE', page: 0, pageSize: 9 })
  }
}, {
  default: () => [],
  server: false,
})

const { data: newLots } = useAsyncData('new-lots', async () => {
  // Show both ACTIVE lots (with auctions) and APPROVED lots (newly listed, awaiting auction)
  const activeLots = await fetchLotsFromCatalog({ status: 'ACTIVE', page: 0, pageSize: 8 })
  const approvedLots = await fetchLotsFromCatalog({ status: 'APPROVED', page: 0, pageSize: 8 })
  // Merge and take the 8 newest by combining both lists
  return [...activeLots, ...approvedLots].slice(0, 8)
}, {
  default: () => [],
  server: false,
})

function selectCountry(code: string) {
  selectedCountry.value = selectedCountry.value === code ? null : code
  navigateTo({ path: '/search', query: { country: selectedCountry.value || undefined } })
}

useHead({
  title: t('home.pageTitle'),
})

useSeoMeta({
  description: t('home.heroSubtitle'),
  ogTitle: t('home.pageTitle'),
  ogDescription: t('home.heroSubtitle'),
  ogType: 'website',
})
</script>
