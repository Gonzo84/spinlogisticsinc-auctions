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
          <SearchSearchBar />
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
            v-for="country in countries"
            :key="country.code"
            class="flex items-center gap-2 px-4 py-2 rounded-full border hover:border-primary hover:bg-primary-50 transition-colors shrink-0"
            :class="{ 'border-primary bg-primary-50': selectedCountry === country.code }"
            @click="selectCountry(country.code)"
          >
            <span class="text-xl">{{ country.flag }}</span>
            <span class="text-sm font-medium">{{ country.name }}</span>
          </button>
        </div>
      </div>
    </section>

    <!-- Category Pills -->
    <section class="bg-white py-6 px-4">
      <div class="max-w-7xl mx-auto">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">{{ $t('home.categories') }}</h2>
        <div class="flex flex-wrap gap-3">
          <NuxtLink
            v-for="category in categories"
            :key="category.slug"
            :to="{ path: '/search', query: { category: category.slug } }"
            class="inline-flex items-center gap-2 px-5 py-2.5 rounded-full bg-gray-100 hover:bg-primary-50 hover:text-primary text-gray-700 font-medium text-sm transition-colors"
          >
            <span>{{ category.icon }}</span>
            <span>{{ category.name }}</span>
            <span class="text-xs text-gray-400">({{ category.count }})</span>
          </NuxtLink>
        </div>
      </div>
    </section>

    <!-- Featured Auctions Carousel -->
    <section class="py-10 px-4 bg-gray-50">
      <div class="max-w-7xl mx-auto">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-2xl font-bold text-gray-900">{{ $t('home.featuredAuctions') }}</h2>
          <div class="flex gap-2">
            <button
              class="p-2 rounded-full border hover:bg-white transition-colors"
              :disabled="carouselIndex === 0"
              @click="carouselIndex = Math.max(0, carouselIndex - 1)"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <button
              class="p-2 rounded-full border hover:bg-white transition-colors"
              :disabled="carouselIndex >= featuredAuctions.length - 3"
              @click="carouselIndex = Math.min(featuredAuctions.length - 3, carouselIndex + 1)"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>
        </div>
        <div class="overflow-hidden">
          <div
            class="flex gap-6 transition-transform duration-300"
            :style="{ transform: `translateX(-${carouselIndex * 33.33}%)` }"
          >
            <div
              v-for="auction in featuredAuctions"
              :key="auction.id"
              class="min-w-[calc(33.333%-1rem)] flex-shrink-0"
            >
              <NuxtLink
                :to="`/lots/${auction.id}`"
                class="block bg-white rounded-xl shadow-sm hover:shadow-md transition-shadow overflow-hidden"
              >
                <div class="relative aspect-[4/3] bg-gray-200">
                  <img
                    :src="auction.imageUrl"
                    :alt="auction.title"
                    class="w-full h-full object-cover"
                    loading="lazy"
                  >
                  <div class="absolute top-3 left-3">
                    <span class="px-2 py-1 bg-accent text-white text-xs font-bold rounded">
                      {{ $t('home.featured') }}
                    </span>
                  </div>
                </div>
                <div class="p-4">
                  <h3 class="font-semibold text-gray-900 line-clamp-2 mb-2">{{ auction.title }}</h3>
                  <div class="flex items-center justify-between">
                    <div>
                      <p class="text-xs text-gray-500">{{ $t('auction.currentBid') }}</p>
                      <p class="text-lg font-bold text-primary">{{ formatCurrency(auction.currentBid) }}</p>
                    </div>
                    <AuctionAuctionTimer :end-time="auction.endTime" compact />
                  </div>
                </div>
              </NuxtLink>
            </div>
          </div>
        </div>
      </div>
    </section>

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
    <section class="py-16 px-4 bg-secondary-50">
      <div class="max-w-4xl mx-auto text-center">
        <div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-secondary-100 mb-6">
          <svg class="w-8 h-8 text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <h2 class="text-3xl font-bold text-gray-900 mb-2">{{ $t('home.co2Title') }}</h2>
        <p class="text-gray-600 mb-8">{{ $t('home.co2Subtitle') }}</p>
        <div class="flex items-center justify-center gap-1">
          <span
            ref="co2Counter"
            class="text-5xl md:text-6xl font-bold text-secondary animate-count-up"
          >
            {{ animatedCo2.toLocaleString() }}
          </span>
          <span class="text-2xl font-bold text-secondary-700 ml-2">kg CO&#8322;</span>
        </div>
        <p class="text-sm text-gray-500 mt-3">{{ $t('home.co2Description') }}</p>
      </div>
    </section>

    <!-- How It Works -->
    <section class="py-16 px-4 bg-white">
      <div class="max-w-5xl mx-auto">
        <h2 class="text-3xl font-bold text-gray-900 text-center mb-12">{{ $t('home.howItWorks') }}</h2>
        <div class="grid grid-cols-1 md:grid-cols-4 gap-8">
          <div
            v-for="(step, index) in howItWorksSteps"
            :key="index"
            class="text-center"
          >
            <div class="inline-flex items-center justify-center w-14 h-14 rounded-full bg-primary-100 text-primary font-bold text-xl mb-4">
              {{ index + 1 }}
            </div>
            <h3 class="font-semibold text-gray-900 mb-2">{{ step.title }}</h3>
            <p class="text-sm text-gray-600">{{ step.description }}</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
const { t } = useI18n()
const { listAuctions } = useAuction()

const carouselIndex = ref(0)
const selectedCountry = ref<string | null>(null)
const animatedCo2 = ref(0)

const countries = [
  { code: 'NL', flag: '\ud83c\uddf3\ud83c\uddf1', name: 'Netherlands' },
  { code: 'DE', flag: '\ud83c\udde9\ud83c\uddea', name: 'Germany' },
  { code: 'FR', flag: '\ud83c\uddeb\ud83c\uddf7', name: 'France' },
  { code: 'BE', flag: '\ud83c\udde7\ud83c\uddea', name: 'Belgium' },
  { code: 'PL', flag: '\ud83c\uddf5\ud83c\uddf1', name: 'Poland' },
  { code: 'IT', flag: '\ud83c\uddee\ud83c\uddf9', name: 'Italy' },
  { code: 'RO', flag: '\ud83c\uddf7\ud83c\uddf4', name: 'Romania' },
  { code: 'ES', flag: '\ud83c\uddea\ud83c\uddf8', name: 'Spain' },
  { code: 'AT', flag: '\ud83c\udde6\ud83c\uddf9', name: 'Austria' },
]

const categories = [
  { slug: 'transport', icon: '\ud83d\ude9a', name: t('categories.transport'), count: 1240 },
  { slug: 'agriculture', icon: '\ud83d\ude9c', name: t('categories.agriculture'), count: 890 },
  { slug: 'construction', icon: '\ud83c\udfd7\ufe0f', name: t('categories.construction'), count: 1560 },
  { slug: 'metalworking', icon: '\u2699\ufe0f', name: t('categories.metalworking'), count: 430 },
  { slug: 'woodworking', icon: '\ud83e\udeb5', name: t('categories.woodworking'), count: 310 },
  { slug: 'food-processing', icon: '\ud83c\udfed', name: t('categories.foodProcessing'), count: 220 },
  { slug: 'electronics', icon: '\ud83d\udd0c', name: t('categories.electronics'), count: 670 },
  { slug: 'warehouse', icon: '\ud83d\udce6', name: t('categories.warehouse'), count: 540 },
]

const { data: featuredAuctions } = await useAsyncData('featured-auctions', () =>
  listAuctions({ featured: true, limit: 9 }), {
  default: () => [],
})

const { data: newLots } = await useAsyncData('new-lots', () =>
  listAuctions({ sort: 'newest', limit: 8 }), {
  default: () => [],
})

const targetCo2 = 2_847_350

const howItWorksSteps = computed(() => [
  { title: t('home.step1Title'), description: t('home.step1Desc') },
  { title: t('home.step2Title'), description: t('home.step2Desc') },
  { title: t('home.step3Title'), description: t('home.step3Desc') },
  { title: t('home.step4Title'), description: t('home.step4Desc') },
])

function selectCountry(code: string) {
  selectedCountry.value = selectedCountry.value === code ? null : code
  navigateTo({ path: '/search', query: { country: selectedCountry.value || undefined } })
}

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-EU', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(amount)
}

onMounted(() => {
  const duration = 2000
  const steps = 60
  const increment = targetCo2 / steps
  let current = 0
  const interval = setInterval(() => {
    current += increment
    if (current >= targetCo2) {
      animatedCo2.value = targetCo2
      clearInterval(interval)
    } else {
      animatedCo2.value = Math.floor(current)
    }
  }, duration / steps)
})

useHead({
  title: t('home.pageTitle'),
})
</script>
