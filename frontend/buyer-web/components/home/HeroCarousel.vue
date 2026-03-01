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
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <button
            class="p-2 rounded-full border hover:bg-white transition-colors"
            :disabled="carouselIndex >= auctions.length - 3"
            @click="carouselIndex = Math.min(auctions.length - 3, carouselIndex + 1)"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
          </button>
        </div>
      </div>
      <!-- Empty state when no featured auctions -->
      <div v-if="auctions.length === 0" class="text-center py-12">
        <svg class="w-16 h-16 mx-auto text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
        </svg>
        <p class="text-gray-500 text-lg mb-2">No featured auctions right now</p>
        <p class="text-gray-400 text-sm">Check back soon for new lots going live.</p>
        <NuxtLink to="/search" class="inline-block mt-4 text-primary font-medium hover:underline">
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

interface FeaturedAuction {
  id: string
  title: string
  imageUrl: string
  currentBid: number
  endTime: string
}

defineProps<{
  auctions: FeaturedAuction[]
}>()

const carouselIndex = ref(0)
</script>
