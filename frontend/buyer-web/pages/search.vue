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
        <div class="flex items-center justify-between mb-4 card !p-3">
          <div class="flex items-center gap-4">
            <label class="text-sm text-gray-600 shrink-0">{{ $t('search.sort') }}:</label>
            <Select
              v-model="sortBy"
              :options="sortOptions"
              optionLabel="label"
              optionValue="value"
              class="w-44 !border-0 !shadow-none !bg-transparent"
              @change="applySort"
            />
          </div>
          <div class="flex items-center gap-1">
            <Button
              icon="pi pi-th-large"
              :text="viewMode !== 'grid'"
              :severity="viewMode === 'grid' ? undefined : 'secondary'"
              size="small"
              rounded
              aria-label="Grid view"
              @click="viewMode = 'grid'"
            />
            <Button
              icon="pi pi-list"
              :text="viewMode !== 'list'"
              :severity="viewMode === 'list' ? undefined : 'secondary'"
              size="small"
              rounded
              aria-label="List view"
              @click="viewMode = 'list'"
            />
          </div>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="flex items-center justify-center py-24">
          <ProgressSpinner />
        </div>

        <!-- Results Grid — wrapped in ClientOnly to prevent hydration mismatches from timers/bid counts (gotcha #66) -->
        <div v-else-if="lots.length > 0">
          <ClientOnly>
            <div
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
            <template #fallback>
              <div
                :class="viewMode === 'grid'
                  ? 'grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-6'
                  : 'flex flex-col gap-4'"
              >
                <div v-for="n in 6" :key="n" class="bg-white rounded-xl border overflow-hidden animate-pulse">
                  <div class="aspect-[4/3] bg-gray-200" />
                  <div class="p-4 space-y-2">
                    <div class="h-3 bg-gray-200 rounded w-16" />
                    <div class="h-4 bg-gray-200 rounded w-3/4" />
                    <div class="h-3 bg-gray-200 rounded w-1/2" />
                    <div class="flex justify-between mt-3">
                      <div class="h-5 bg-gray-200 rounded w-20" />
                      <div class="h-5 bg-gray-200 rounded w-16" />
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </ClientOnly>
        </div>

        <!-- No Results -->
        <div v-else class="text-center py-16">
          <i class="pi pi-search text-6xl text-gray-300 mb-4" />
          <h3 class="text-lg font-medium text-gray-900 mb-2">{{ $t('search.noResults') }}</h3>
          <p class="text-gray-500 mb-6">{{ $t('search.noResultsHint') }}</p>
          <Button :label="$t('search.clearFilters')" @click="clearAllFilters" />
        </div>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="flex items-center justify-center gap-2 mt-8">
          <Button
            :label="$t('common.back')"
            outlined
            size="small"
            :disabled="currentPage <= 1"
            @click="goToPage(currentPage - 1)"
          />

          <template v-for="page in visiblePages" :key="page">
            <span v-if="page === '...'" class="px-2 text-gray-400">...</span>
            <Button
              v-else
              :label="String(page)"
              :outlined="page !== currentPage"
              :text="page !== currentPage"
              size="small"
              class="!w-10 !h-10"
              @click="goToPage(page as number)"
            />
          </template>

          <Button
            :label="$t('common.next')"
            outlined
            size="small"
            :disabled="currentPage >= totalPages"
            @click="goToPage(currentPage + 1)"
          />
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

const sortOptions = computed(() => [
  { label: t('search.closingSoonest'), value: 'closing_soonest' },
  { label: t('search.priceAsc'), value: 'price_asc' },
  { label: t('search.priceDesc'), value: 'price_desc' },
  { label: t('search.newest'), value: 'newest' },
  { label: t('search.bidCount'), value: 'bid_count' },
])

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

  if ('category' in newFilters) query.category = newFilters.category || undefined
  if ('country' in newFilters) query.country = newFilters.country?.join(',') || undefined
  if ('priceMin' in newFilters) query.priceMin = newFilters.priceMin?.toString() || undefined
  if ('priceMax' in newFilters) query.priceMax = newFilters.priceMax?.toString() || undefined
  if ('distance' in newFilters) query.distance = newFilters.distance?.toString() || undefined
  if ('reserveStatus' in newFilters) query.reserveStatus = newFilters.reserveStatus || undefined

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
