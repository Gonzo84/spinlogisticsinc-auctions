<template>
  <NuxtLink
    :to="`/lots/${lot.id}`"
    class="block bg-white rounded-xl border hover:shadow-md transition-shadow overflow-hidden"
    :class="viewMode === 'list' ? 'flex' : ''"
  >
    <!-- Image -->
    <div
      class="relative bg-gray-100 overflow-hidden"
      :class="viewMode === 'list' ? 'w-48 h-36 shrink-0' : 'aspect-[4/3]'"
    >
      <img
        v-if="imageUrl"
        :src="imageUrl"
        :alt="lot.title"
        class="w-full h-full object-cover"
        loading="lazy"
      >
      <div v-else class="w-full h-full flex items-center justify-center text-gray-300">
        <svg class="w-12 h-12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
      </div>

      <!-- CO2 Badge -->
      <div v-if="lot.co2Savings" class="absolute top-2 left-2">
        <SharedCO2Badge :amount="lot.co2Savings" />
      </div>

      <!-- Country Flag -->
      <div class="absolute top-2 right-2 text-lg" :title="locationCountry">
        {{ countryFlag }}
      </div>

      <!-- Reserve Indicator -->
      <div
        v-if="lot.reservePrice !== undefined"
        class="absolute bottom-2 left-2 px-2 py-0.5 rounded text-[10px] font-bold"
        :class="lot.reserveMet ? 'bg-secondary text-white' : 'bg-accent text-white'"
      >
        {{ lot.reserveMet ? $t('auction.reserveMet') : $t('auction.reserve') }}
      </div>
    </div>

    <!-- Content -->
    <div class="p-4 flex-1 min-w-0">
      <!-- Category Badge -->
      <span class="inline-block px-2 py-0.5 bg-gray-100 rounded text-[10px] font-medium text-gray-600 uppercase tracking-wide mb-1.5">
        {{ lot.category }}
      </span>

      <!-- Title -->
      <h3
        class="font-semibold text-gray-900 mb-1"
        :class="viewMode === 'list' ? 'text-base line-clamp-1' : 'text-sm line-clamp-2'"
      >
        {{ lot.title }}
      </h3>

      <!-- Location -->
      <p v-if="locationCity || locationCountry" class="text-xs text-gray-500 mb-2 flex items-center gap-1">
        <svg class="w-3 h-3 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
        <span class="truncate">{{ [locationCity, locationCountry].filter(Boolean).join(', ') }}</span>
      </p>

      <!-- Bid Info & Timer -->
      <div class="flex items-end justify-between gap-2 mt-auto">
        <div>
          <p class="text-[10px] text-gray-500 uppercase tracking-wide">{{ $t('auction.currentBid') }}</p>
          <p class="text-lg font-bold text-primary leading-tight">{{ formatCurrency(lot.currentBid ?? 0) }}</p>
          <p class="text-[10px] text-gray-400 mt-0.5">
            {{ lot.bidCount }} {{ $t('auction.bids') }}
          </p>
        </div>
        <div class="shrink-0">
          <AuctionTimer :end-time="lot.endTime ?? ''" compact />
        </div>
      </div>
    </div>
  </NuxtLink>
</template>

<script setup lang="ts">
import type { Auction } from '~/types/auction'
import { formatCurrency } from '~/utils/format'
import { getCountryFlag } from '~/utils/constants'

interface Props {
  lot: Partial<Auction> & { id: string; [key: string]: unknown }
  viewMode?: 'grid' | 'list'
}

const props = withDefaults(defineProps<Props>(), {
  viewMode: 'grid',
})

// Handle both frontend Auction fields and backend catalog-service flat fields
const locationCountry = computed(() =>
  props.lot.country || (props.lot.locationCountry as string) || ''
)
const locationCity = computed(() =>
  props.lot.location || (props.lot.locationCity as string) || (props.lot.city as string) || ''
)
const imageUrl = computed(() =>
  props.lot.images?.[0]?.url || (props.lot.primaryImageUrl as string) || (props.lot.thumbnailUrl as string) || ''
)
const countryFlag = computed(() => getCountryFlag(locationCountry.value))
</script>
