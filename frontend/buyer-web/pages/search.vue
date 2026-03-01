<template>
  <div class="max-w-7xl mx-auto px-4 py-6">
    <!-- Search Header -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900 mb-2">
        {{ query ? $t('search.resultsFor', { query }) : $t('search.browseAll') }}
      </h1>
      <p v-if="totalResults > 0" class="text-gray-500">
        {{ $t('search.results', { count: totalResults }) }}
      </p>
    </div>

    <div class="flex gap-6">
      <!-- Filter Sidebar -->
      <aside class="hidden lg:block w-72 shrink-0">
        <SearchFilterSidebar
          :filters="activeFilters"
          @update:filters="updateFilters"
        />
      </aside>

      <!-- Mobile Filter Toggle -->
      <div class="lg:hidden fixed bottom-4 left-4 right-4 z-40">
        <Button
          :label="$t('search.filters')"
          icon="pi pi-filter"
          class="w-full shadow-lg"
          :badge="activeFilterCount > 0 ? String(activeFilterCount) : undefined"
          @click="showMobileFilters = true"
        />
      </div>

      <!-- Mobile Filter Modal -->
      <Teleport to="body">
        <div v-if="showMobileFilters" class="fixed inset-0 z-50 lg:hidden">
          <div class="absolute inset-0 bg-black/50" @click="showMobileFilters = false" />
          <div class="absolute inset-y-0 left-0 w-80 max-w-full bg-white overflow-y-auto">
            <div class="flex items-center justify-between p-4 border-b sticky top-0 bg-white">
              <h2 class="font-semibold text-lg">{{ $t('search.filters') }}</h2>
              <Button
                icon="pi pi-times"
                text
                rounded
                aria-label="Close filters"
                @click="showMobileFilters = false"
              />
            </div>
            <div class="p-4">
              <SearchFilterSidebar
                :filters="activeFilters"
                @update:filters="updateFilters"
              />
            </div>
          </div>
        </div>
      </Teleport>

      <!-- Main Content -->
      <div class="flex-1 min-w-0">
        <!-- Sort Bar -->
        <div class="flex items-center justify-between mb-4 bg-white rounded-lg border p-3">
          <div class="flex items-center gap-4">
            <label class="text-sm text-gray-600 shrink-0">{{ $t('search.sort') }}:</label>
            <select
              v-model="sortBy"
              class="text-sm border-0 bg-transparent font-medium text-gray-900 focus:ring-0 cursor-pointer"
              @change="applySort"
            >
              <option value="closing_soonest">{{ $t('search.closingSoonest') }}</option>
              <option value="price_asc">{{ $t('search.priceAsc') }}</option>
              <option value="price_desc">{{ $t('search.priceDesc') }}</option>
              <option value="newest">{{ $t('search.newest') }}</option>
              <option value="bid_count">{{ $t('search.bidCount') }}</option>
            </select>
          </div>
          <div class="flex items-center gap-2">
            <button
              class="p-2 rounded-md transition-colors"
              :class="viewMode === 'grid' ? 'bg-primary-50 text-primary' : 'text-gray-400 hover:text-gray-600'"
              @click="viewMode = 'grid'"
            >
              <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                <path d="M5 3a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2V5a2 2 0 00-2-2H5zM5 11a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2v-2a2 2 0 00-2-2H5zM11 5a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V5zM11 13a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
              </svg>
            </button>
            <button
              class="p-2 rounded-md transition-colors"
              :class="viewMode === 'list' ? 'bg-primary-50 text-primary' : 'text-gray-400 hover:text-gray-600'"
              @click="viewMode = 'list'"
            >
              <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M3 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z" clip-rule="evenodd" />
              </svg>
            </button>
          </div>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-6">
          <div v-for="i in 6" :key="i" class="bg-white rounded-xl border p-4 animate-pulse">
            <div class="aspect-[4/3] bg-gray-200 rounded-lg mb-4" />
            <div class="h-4 bg-gray-200 rounded mb-2 w-3/4" />
            <div class="h-4 bg-gray-200 rounded mb-4 w-1/2" />
            <div class="h-6 bg-gray-200 rounded w-1/3" />
          </div>
        </div>

        <!-- Results Grid -->
        <div
          v-else-if="lots.length > 0"
          :class="viewMode === 'grid'
            ? 'grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-6'
            : 'flex flex-col gap-4'"
        >
          <AuctionLotCard
            v-for="lot in lots"
            :key="(lot.id as string)"
            :lot="(lot as Partial<Auction> & { id: string })"
            :view-mode="viewMode"
          />
        </div>

        <!-- No Results -->
        <div v-else class="text-center py-16">
          <svg class="w-16 h-16 text-gray-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <h3 class="text-lg font-medium text-gray-900 mb-2">{{ $t('search.noResults') }}</h3>
          <p class="text-gray-500 mb-6">{{ $t('search.noResultsHint') }}</p>
          <Button :label="$t('search.clearFilters')" @click="clearAllFilters" />
        </div>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="flex items-center justify-center gap-2 mt-8">
          <button
            class="px-4 py-2 rounded-lg border font-medium text-sm disabled:opacity-40"
            :disabled="currentPage <= 1"
            @click="goToPage(currentPage - 1)"
          >
            {{ $t('common.back') }}
          </button>

          <template v-for="page in visiblePages" :key="page">
            <span v-if="page === '...'" class="px-2 text-gray-400">...</span>
            <button
              v-else
              class="w-10 h-10 rounded-lg text-sm font-medium transition-colors"
              :class="page === currentPage
                ? 'bg-primary text-white'
                : 'border hover:bg-gray-50'"
              @click="goToPage(page as number)"
            >
              {{ page }}
            </button>
          </template>

          <button
            class="px-4 py-2 rounded-lg border font-medium text-sm disabled:opacity-40"
            :disabled="currentPage >= totalPages"
            @click="goToPage(currentPage + 1)"
          >
            {{ $t('common.next') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Auction } from '~/types/auction'
import type { SearchFilters } from '~/types/search'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const { search: performSearch } = useSearch()

const showMobileFilters = ref(false)
const viewMode = ref<'grid' | 'list'>('grid')
const loading = ref(false)
const lots = ref<Record<string, unknown>[]>([])
const totalResults = ref(0)
const totalPages = ref(0)
const currentPage = ref(1)

const query = computed(() => (route.query.q as string) || '')
const sortBy = ref((route.query.sort as string) || 'closing_soonest')

const activeFilters = computed<SearchFilters>(() => ({
  q: (route.query.q as string) || undefined,
  category: (route.query.category as string) || undefined,
  country: route.query.country ? (route.query.country as string).split(',') : undefined,
  priceMin: route.query.priceMin ? Number(route.query.priceMin) : undefined,
  priceMax: route.query.priceMax ? Number(route.query.priceMax) : undefined,
  distance: route.query.distance ? Number(route.query.distance) : undefined,
  reserveStatus: (route.query.reserveStatus as string) || undefined,
  sort: sortBy.value,
  page: currentPage.value,
}))

const activeFilterCount = computed(() => {
  let count = 0
  if (activeFilters.value.category) count++
  if (activeFilters.value.country?.length) count++
  if (activeFilters.value.priceMin || activeFilters.value.priceMax) count++
  if (activeFilters.value.distance) count++
  if (activeFilters.value.reserveStatus) count++
  return count
})

const visiblePages = computed(() => {
  const total = totalPages.value
  const current = currentPage.value

  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1)
  }

  const pages: (number | string)[] = [1]

  const rangeStart = Math.max(2, current - 1)
  const rangeEnd = Math.min(total - 1, current + 1)

  if (rangeStart > 2) {
    pages.push('...')
  }

  for (let i = rangeStart; i <= rangeEnd; i++) {
    pages.push(i)
  }

  if (rangeEnd < total - 1) {
    pages.push('...')
  }

  if (total > 1) {
    pages.push(total)
  }

  return pages
})

async function fetchResults() {
  loading.value = true
  try {
    const result = await performSearch(activeFilters.value)
    lots.value = result.items
    totalResults.value = result.total
    totalPages.value = result.totalPages
  } catch {
    lots.value = []
    totalResults.value = 0
    totalPages.value = 0
  } finally {
    loading.value = false
  }
}

function updateFilters(newFilters: Partial<SearchFilters>) {
  const query: Record<string, string | undefined> = {
    ...route.query as Record<string, string>,
  }

  if (newFilters.category !== undefined) query.category = newFilters.category || undefined
  if (newFilters.country !== undefined) query.country = newFilters.country?.join(',') || undefined
  if (newFilters.priceMin !== undefined) query.priceMin = newFilters.priceMin?.toString() || undefined
  if (newFilters.priceMax !== undefined) query.priceMax = newFilters.priceMax?.toString() || undefined
  if (newFilters.distance !== undefined) query.distance = newFilters.distance?.toString() || undefined
  if (newFilters.reserveStatus !== undefined) query.reserveStatus = newFilters.reserveStatus || undefined

  query.page = '1'
  currentPage.value = 1

  router.push({ path: '/search', query })
  showMobileFilters.value = false
}

function applySort() {
  router.push({
    path: '/search',
    query: { ...route.query, sort: sortBy.value, page: '1' },
  })
}

function goToPage(page: number) {
  currentPage.value = page
  router.push({
    path: '/search',
    query: { ...route.query, page: page.toString() },
  })
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function clearAllFilters() {
  router.push({ path: '/search' })
}

watch(() => route.query, () => {
  currentPage.value = Number(route.query.page) || 1
  sortBy.value = (route.query.sort as string) || 'closing_soonest'
  fetchResults()
}, { immediate: true })

useHead({
  title: computed(() => query.value ? t('search.titleWithQuery', { query: query.value }) : t('search.title')),
})

useSeoMeta({
  description: computed(() => query.value
    ? t('search.resultsFor', { query: query.value })
    : t('search.browseAll')),
  ogTitle: computed(() => query.value ? t('search.titleWithQuery', { query: query.value }) : t('search.title')),
  ogDescription: computed(() => query.value
    ? t('search.resultsFor', { query: query.value })
    : t('search.browseAll')),
})
</script>
