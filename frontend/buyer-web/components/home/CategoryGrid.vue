<template>
  <section class="bg-white py-6 px-4">
    <div class="max-w-7xl mx-auto">
      <h2 class="text-lg font-semibold text-gray-900 mb-4">{{ $t('home.categories') }}</h2>
      <div class="flex flex-wrap gap-3">
        <NuxtLink
          v-for="category in CATEGORIES"
          :key="category.slug"
          :to="{ path: '/search', query: { category: category.slug } }"
          class="inline-flex items-center gap-2 px-5 py-2.5 rounded-full bg-gray-100 hover:bg-primary-50 hover:text-primary text-gray-700 font-medium text-sm transition-colors"
        >
          <span>{{ category.icon }}</span>
          <span>{{ $t(category.i18nKey) }}</span>
        </NuxtLink>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { CATEGORIES } from '~/utils/constants'
</script>
