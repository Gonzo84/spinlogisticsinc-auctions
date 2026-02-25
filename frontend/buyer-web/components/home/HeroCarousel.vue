<template>
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
            :disabled="carouselIndex >= auctions.length - 3"
            @click="carouselIndex = Math.min(auctions.length - 3, carouselIndex + 1)"
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
