<template>
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
        <span class="text-5xl md:text-6xl font-bold text-secondary">
          {{ animatedValue.toLocaleString() }}
        </span>
        <span class="text-2xl font-bold text-secondary-700 ml-2">kg CO&#8322;</span>
      </div>
      <p class="text-sm text-gray-500 mt-3">{{ $t('home.co2Description') }}</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { TARGET_CO2_SAVINGS } from '~/utils/constants'

const animatedValue = ref(0)

onMounted(() => {
  const duration = 2000
  const steps = 60
  const increment = TARGET_CO2_SAVINGS / steps
  let current = 0
  const interval = setInterval(() => {
    current += increment
    if (current >= TARGET_CO2_SAVINGS) {
      animatedValue.value = TARGET_CO2_SAVINGS
      clearInterval(interval)
    } else {
      animatedValue.value = Math.floor(current)
    }
  }, duration / steps)
})
</script>
