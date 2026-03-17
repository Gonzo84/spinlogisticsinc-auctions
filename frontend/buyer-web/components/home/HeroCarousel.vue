<template>
  <section class="py-10 px-4 bg-gray-50">
    <div class="max-w-7xl mx-auto">
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-2xl font-bold text-gray-900">{{ $t('home.featuredAuctions') }}</h2>
        <div v-if="auctions.length > 0" class="flex gap-2">
          <button
            class="p-2 rounded-full border hover:bg-white transition-colors"
            :disabled="carouselIndex === 0"
            @click="carouselIndex = Math.max(0, carouselIndex - 1)"
          >
            <i class="pi pi-chevron-left w-5 h-5" />
          </button>
          <button
            class="p-2 rounded-full border hover:bg-white transition-colors"
            :disabled="carouselIndex >= auctions.length - 3"
            @click="carouselIndex = Math.min(auctions.length - 3, carouselIndex + 1)"
          >
            <i class="pi pi-chevron-right w-5 h-5" />
          </button>
        </div>
      </div>
      <!-- Loading skeleton while fetching -->
      <div v-if="loading" class="grid grid-cols-3 gap-6">
        <div v-for="n in 3" :key="n" class="bg-white rounded-xl shadow-sm overflow-hidden animate-pulse">
          <div class="aspect-[4/3] bg-gray-200" />
          <div class="p-4">
            <div class="h-4 bg-gray-200 rounded w-3/4 mb-3" />
            <div class="h-3 bg-gray-200 rounded w-1/2 mb-2" />
            <div class="h-6 bg-gray-200 rounded w-1/3" />
          </div>
        </div>
      </div>
      <!-- Empty state when no featured auctions -->
      <div v-else-if="auctions.length === 0" class="text-center py-12">
        <i class="pi pi-box text-6xl text-gray-300 mb-4" />
        <p class="text-gray-500 text-lg mb-2">{{ $t('home.noFeaturedAuctions') }}</p>
        <p class="text-gray-400 text-sm">{{ $t('home.checkBackSoon') }}</p>
        <NuxtLink :to="localePath('/search')" class="inline-block mt-4 text-primary font-medium hover:underline">
          {{ $t('home.viewAll') }} &rarr;
        </NuxtLink>
      </div>
      <!-- Carousel content -->
      <div v-else class="overflow-hidden">
        <div
          class="flex gap-6 transition-transform duration-300"
          :style="{ transform: `translateX(-${carouselIndex * 33.33}%)` }"
        >
          <div
            v-for="auction in auctions"
            :key="auction.id"
            class="min-w-[calc(33.333%-1rem)] flex-shrink-0"
          >
            <NuxtLink
              :to="localePath(`/lots/${auction.catalogLotId || auction.id}`)"
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
                  <AuctionTimer v-if="auction.endTime" :end-time="auction.endTime" compact />
                </div>
              </div>
            </NuxtLink>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { formatCurrency } from '~/utils/format'

const localePath = useLocalePath()

interface FeaturedAuction {
  id: string
  catalogLotId?: string
  title: string
  imageUrl: string
  currentBid: number
  endTime: string
}

withDefaults(defineProps<{
  auctions: FeaturedAuction[]
  loading?: boolean
}>(), {
  loading: false,
})

const carouselIndex = ref(0)
</script>
